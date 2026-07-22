package com.oitapp.ui.patients;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.ui.scanner.BarcodeScannerActivity;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientListActivity extends AppCompatActivity {
    private static final int REQUEST_SCAN_MRN = 1001;
    private final List<Map<String, Object>> patients = new ArrayList<>();
    private PatientAdapter adapter;
    private LinearProgressIndicator progressIndicator;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (sessionManager.redirectToLoginIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_patient_list);
        setTitle(R.string.patient_list);

        progressIndicator = findViewById(R.id.progress_patients);
        TextInputEditText etSearch = findViewById(R.id.et_search);
        MaterialButton btnScanMrn = findViewById(R.id.btn_scan_mrn);
        RecyclerView recyclerView = findViewById(R.id.rv_patients);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAdapter(patients, patient -> {
            int patientId = ResponseHelper.getInt(patient, "id");
            sessionManager.setCurrentPatient(patientId);
            Intent intent = new Intent(PatientListActivity.this, PatientDetailActivity.class);
            intent.putExtra("patient_id", patientId);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        btnScanMrn.setOnClickListener(view -> startActivityForResult(
                new Intent(this, BarcodeScannerActivity.class), REQUEST_SCAN_MRN));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                loadPatients(editable == null ? "" : editable.toString().trim());
            }
        });

        loadPatients("");
    }

    private void loadPatients(String search) {
        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService(this).getPatients(search).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                progressIndicator.setVisibility(View.GONE);
                patients.clear();
                if (response.body() != null) {
                    patients.addAll(ResponseHelper.unwrapDataList(response.body(), "patients"));
                }
                adapter.notifyDataSetChanged();
                if (patients.isEmpty()) {
                    Toast.makeText(PatientListActivity.this, R.string.no_patients_found, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                progressIndicator.setVisibility(View.GONE);
                showRetryDialog(search, throwable.getMessage());
            }
        });
    }

    private void loadPatientByBarcode(String barcode) {
        progressIndicator.setVisibility(View.VISIBLE);
        ApiClient.getApiService(this).getPatientByBarcode(barcode).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                progressIndicator.setVisibility(View.GONE);
                Map<String, Object> patient = response.body() == null
                        ? java.util.Collections.<String, Object>emptyMap()
                        : ResponseHelper.unwrapDataMap(response.body(), "patient");
                int patientId = ResponseHelper.getInt(patient, "id");
                if (patientId == 0) {
                    Toast.makeText(PatientListActivity.this, R.string.no_patients_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionManager.setCurrentPatient(patientId);
                Intent intent = new Intent(PatientListActivity.this, PatientDetailActivity.class);
                intent.putExtra("patient_id", patientId);
                startActivity(intent);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(PatientListActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN_MRN) {
            if (resultCode == RESULT_OK && data != null) {
                loadPatientByBarcode(data.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE));
            } else {
                Toast.makeText(this, R.string.barcode_cancelled, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showRetryDialog(final String search, String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.network_error)
                .setMessage(message == null ? getString(R.string.unable_to_load_data) : message)
                .setPositiveButton(R.string.retry, (dialogInterface, i) -> loadPatients(search))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
        interface OnPatientClickListener {
            void onPatientClick(Map<String, Object> patient);
        }

        private final List<Map<String, Object>> items;
        private final OnPatientClickListener listener;

        PatientAdapter(List<Map<String, Object>> items, OnPatientClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new PatientViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
            Map<String, Object> patient = items.get(position);
            String title = ResponseHelper.getString(patient, "name");
            if (title.isEmpty()) {
                title = (ResponseHelper.getString(patient, "first_name") + " "
                        + ResponseHelper.getString(patient, "last_name")).trim();
            }
            holder.title.setText(title.isEmpty() ? "Patient #" + ResponseHelper.getInt(patient, "id") : title);
            holder.subtitle.setText("MRN: " + ResponseHelper.getString(patient, "mrn"));
            holder.itemView.setOnClickListener(view -> listener.onPatientClick(patient));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PatientViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;

            PatientViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
