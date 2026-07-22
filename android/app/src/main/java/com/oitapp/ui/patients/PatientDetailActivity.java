package com.oitapp.ui.patients;

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
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientDetailActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private LinearProgressIndicator progressIndicator;
    private TextView tvPatientName;
    private TextView tvPatientMeta;
    private TextView tvPatientAllergen;
    private Chip chipCurrentDose;
    private TextView tvNextUpdose;
    private RecordAdapter adapter;
    private final List<DisplayItem> displayItems = new ArrayList<>();
    private Map<String, Object> patientData = java.util.Collections.emptyMap();
    private Map<String, Object> dosePlanData = java.util.Collections.emptyMap();
    private int patientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (sessionManager.redirectToLoginIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_patient_detail);
        setTitle(R.string.patient_detail);

        patientId = getIntent().getIntExtra("patient_id", sessionManager.getCurrentPatientId());
        if (patientId == 0) {
            Toast.makeText(this, R.string.patient_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressIndicator = findViewById(R.id.progress_detail);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvPatientMeta = findViewById(R.id.tv_patient_meta);
        tvPatientAllergen = findViewById(R.id.tv_patient_allergen);
        chipCurrentDose = findViewById(R.id.chip_current_dose);
        tvNextUpdose = findViewById(R.id.tv_next_updose);
        RecyclerView recyclerView = findViewById(R.id.rv_tab_content);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(displayItems);
        recyclerView.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.overview));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.syringes));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.cartons));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.mar_history));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.missions));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                renderTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                renderTab(tab.getPosition());
            }
        });

        loadPatient();
        loadDosePlan();
    }

    private void loadPatient() {
        showLoading(true);
        ApiClient.getApiService(this).getPatient(patientId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.body() == null) {
                    return;
                }
                patientData = ResponseHelper.unwrapDataMap(response.body(), "patient");
                sessionManager.setCurrentPatient(patientId);
                bindHeader();
                renderTab(getCurrentTabIndex());
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showLoading(false);
                showRetryDialog(() -> loadPatient(), throwable.getMessage());
            }
        });
    }

    private void loadDosePlan() {
        showLoading(true);
        ApiClient.getApiService(this).getDosePlan(patientId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                showLoading(false);
                if (response.body() != null) {
                    dosePlanData = ResponseHelper.unwrapDataMap(response.body(), "dose_plan");
                }
                bindHeader();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showLoading(false);
            }
        });
    }

    private void bindHeader() {
        String name = ResponseHelper.getString(patientData, "name");
        if (name.isEmpty()) {
            name = (ResponseHelper.getString(patientData, "first_name") + " "
                    + ResponseHelper.getString(patientData, "last_name")).trim();
        }
        tvPatientName.setText(name.isEmpty() ? "Patient #" + patientId : name);
        tvPatientMeta.setText(getString(R.string.mrn) + ": " + ResponseHelper.getString(patientData, "mrn")
                + "   " + getString(R.string.dob) + ": " + ResponseHelper.getString(patientData, "dob"));
        tvPatientAllergen.setText(getString(R.string.allergen) + ": "
                + firstNonEmpty(ResponseHelper.getString(patientData, "allergen"), ResponseHelper.getString(dosePlanData, "allergen")));
        chipCurrentDose.setText(getString(R.string.current_dose) + ": "
                + firstNonEmpty(ResponseHelper.getString(dosePlanData, "current_dose"), ResponseHelper.getString(patientData, "current_dose")));
        tvNextUpdose.setText(getString(R.string.next_updose_date) + ": "
                + firstNonEmpty(ResponseHelper.getString(dosePlanData, "next_updose_date"), ResponseHelper.getString(patientData, "next_updose_date")));
    }

    private void renderTab(int index) {
        displayItems.clear();
        if (index == 0) {
            addItem("Name", tvPatientName.getText().toString());
            addItem(getString(R.string.mrn), ResponseHelper.getString(patientData, "mrn"));
            addItem(getString(R.string.dob), ResponseHelper.getString(patientData, "dob"));
            addItem(getString(R.string.allergen), ResponseHelper.getString(patientData, "allergen"));
            addItem(getString(R.string.current_dose), firstNonEmpty(ResponseHelper.getString(dosePlanData, "current_dose"), ResponseHelper.getString(patientData, "current_dose")));
            addItem(getString(R.string.next_updose_date), firstNonEmpty(ResponseHelper.getString(dosePlanData, "next_updose_date"), ResponseHelper.getString(patientData, "next_updose_date")));
        } else if (index == 1) {
            appendList(ResponseHelper.getList(patientData, "syringes"), "barcode", "status");
        } else if (index == 2) {
            appendList(ResponseHelper.getList(patientData, "cartons"), "lot_number", "expiry_date");
        } else if (index == 3) {
            appendList(ResponseHelper.getList(patientData, "mar_entries"), "administered_at", "outcome");
        } else if (index == 4) {
            appendList(ResponseHelper.getList(patientData, "missions"), "description", "status");
        }
        if (displayItems.isEmpty()) {
            addItem(getString(R.string.no_records), getString(R.string.unable_to_load_data));
        }
        adapter.notifyDataSetChanged();
    }

    private void appendList(List<Map<String, Object>> items, String titleKey, String subtitleKey) {
        for (Map<String, Object> item : items) {
            addItem(firstNonEmpty(ResponseHelper.getString(item, titleKey), "Record #" + ResponseHelper.getInt(item, "id")),
                    ResponseHelper.getString(item, subtitleKey));
        }
    }

    private void addItem(String title, String subtitle) {
        displayItems.add(new DisplayItem(title, subtitle));
    }

    private int getCurrentTabIndex() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        return tabLayout.getSelectedTabPosition() < 0 ? 0 : tabLayout.getSelectedTabPosition();
    }

    private void showLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showRetryDialog(final Runnable retryAction, String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.network_error)
                .setMessage(message == null ? getString(R.string.unable_to_load_data) : message)
                .setPositiveButton(R.string.retry, (dialogInterface, i) -> retryAction.run())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String firstNonEmpty(String first, String second) {
        return first == null || first.isEmpty() ? (second == null ? "" : second) : first;
    }

    private static class DisplayItem {
        final String title;
        final String subtitle;

        DisplayItem(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
        private final List<DisplayItem> items;

        RecordAdapter(List<DisplayItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new RecordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
            DisplayItem item = items.get(position);
            holder.title.setText(item.title);
            holder.subtitle.setText(item.subtitle);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class RecordViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;

            RecordViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(android.R.id.text1);
                subtitle = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
