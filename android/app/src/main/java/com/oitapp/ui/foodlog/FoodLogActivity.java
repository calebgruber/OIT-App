package com.oitapp.ui.foodlog;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodLogActivity extends AppCompatActivity {
    private final List<Map<String, Object>> foodLogs = new ArrayList<>();
    private FoodLogAdapter adapter;
    private SessionManager sessionManager;
    private int patientId;
    private TextInputEditText etDescription;
    private TextView tvTimeLogged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (sessionManager.redirectToLoginIfNeeded(this)) {
            return;
        }
        patientId = sessionManager.getCurrentPatientId();
        if (patientId == 0) {
            Toast.makeText(this, R.string.patient_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_food_log);
        setTitle(R.string.food_log);

        TextView tvPatient = findViewById(R.id.tv_food_patient);
        tvPatient.setText(getString(R.string.active_patient) + ": #" + patientId);
        etDescription = findViewById(R.id.et_food_description);
        tvTimeLogged = findViewById(R.id.tv_time_logged);
        RecyclerView recyclerView = findViewById(R.id.rv_food_logs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FoodLogAdapter(foodLogs);
        recyclerView.setAdapter(adapter);
        MaterialButton btnSubmit = findViewById(R.id.btn_submit_food);
        btnSubmit.setOnClickListener(view -> submitFoodLog());

        tvTimeLogged.setText(getString(R.string.time_logged) + ": " + currentTimestamp());
        loadFoodLogs();
    }

    private void loadFoodLogs() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        ApiClient.getApiService(this).getFoodLogs(patientId, date).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                foodLogs.clear();
                if (response.body() != null) {
                    foodLogs.addAll(ResponseHelper.unwrapDataList(response.body(), "food_logs"));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showRetryDialog(throwable.getMessage());
            }
        });
    }

    private void submitFoodLog() {
        String description = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            etDescription.setError(getString(R.string.description_required));
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("description", description);
        body.put("logged_at", currentTimestamp());
        ApiClient.getApiService(this).createFoodLog(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Toast.makeText(FoodLogActivity.this, R.string.food_logged_success, Toast.LENGTH_SHORT).show();
                etDescription.setText(null);
                tvTimeLogged.setText(getString(R.string.time_logged) + ": " + currentTimestamp());
                loadFoodLogs();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showRetryDialog(throwable.getMessage());
            }
        });
    }

    private String currentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
    }

    private void showRetryDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.network_error)
                .setMessage(message == null ? getString(R.string.unable_to_load_data) : message)
                .setPositiveButton(R.string.retry, (dialogInterface, i) -> loadFoodLogs())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.FoodLogViewHolder> {
        private final List<Map<String, Object>> items;

        FoodLogAdapter(List<Map<String, Object>> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public FoodLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new FoodLogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FoodLogViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            holder.title.setText(ResponseHelper.getString(item, "description"));
            holder.subtitle.setText(ResponseHelper.getString(item, "logged_at"));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class FoodLogViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;

            FoodLogViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
