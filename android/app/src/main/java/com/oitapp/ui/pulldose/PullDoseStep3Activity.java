    package com.oitapp.ui.pulldose;

    import android.app.AlertDialog;
    import android.content.Intent;
    import android.os.Bundle;
    import android.text.TextUtils;
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
    import com.oitapp.R;
    import com.oitapp.api.ApiClient;
    import com.oitapp.ui.scanner.BarcodeScannerActivity;
    import com.oitapp.utils.PrinterManager;
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

    public class PullDoseStep3Activity extends AppCompatActivity {
        private static final int REQUEST_SCAN_SUPPLY = 1002;
        private final List<Map<String, Object>> supplies = new ArrayList<>();
        private SupplyAdapter adapter;
        private LinearProgressIndicator progressIndicator;
        private TextView tvSourceType;
        private TextView tvSelectedSupply;
        private SessionManager sessionManager;
        private PrinterManager printerManager;
        private Map<String, Object> selectedSupply = java.util.Collections.emptyMap();
        private String doseForm;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            sessionManager = new SessionManager(this);
            if (sessionManager.redirectToLoginIfNeeded(this)) {
                return;
            }
            setContentView(R.layout.activity_pull_dose_step3);
            setTitle(R.string.pull_dose_step3_title);
            printerManager = new PrinterManager();

            doseForm = getIntent().getStringExtra("dose_form");
            progressIndicator = findViewById(R.id.progress_step3);
            tvSourceType = findViewById(R.id.tv_source_type);
            tvSelectedSupply = findViewById(R.id.tv_selected_supply);
            MaterialButton btnScanSupply = findViewById(R.id.btn_scan_supply);
            MaterialButton btnConfirmPrint = findViewById(R.id.btn_confirm_print);
            RecyclerView recyclerView = findViewById(R.id.rv_supplies);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SupplyAdapter(supplies, supply -> {
                selectedSupply = supply;
                tvSelectedSupply.setText(buildSupplySummary(supply));
            });
            recyclerView.setAdapter(adapter);

            tvSourceType.setText(getString(R.string.source_supply) + ": " + ("milk".equals(doseForm) ? getString(R.string.cartons) : getString(R.string.powder)));
            btnScanSupply.setOnClickListener(view -> startActivityForResult(new Intent(this, BarcodeScannerActivity.class), REQUEST_SCAN_SUPPLY));
            btnConfirmPrint.setOnClickListener(view -> submitPullDose());

            loadSupplies();
        }

        private void loadSupplies() {
            showLoading(true);
            int patientId = getIntent().getIntExtra("patient_id", sessionManager.getCurrentPatientId());
            Callback<Map<String, Object>> callback = new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    showLoading(false);
                    supplies.clear();
                    if (response.body() != null) {
                        supplies.addAll("milk".equals(doseForm)
                                ? ResponseHelper.unwrapDataList(response.body(), "cartons")
                                : ResponseHelper.unwrapDataList(response.body(), "powder_supplies"));
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                    showLoading(false);
                    showRetryDialog(throwable.getMessage());
                }
            };

            if ("milk".equals(doseForm)) {
                ApiClient.getApiService(this).getCartons(patientId).enqueue(callback);
            } else {
                ApiClient.getApiService(this).getPowderSupplies(patientId).enqueue(callback);
            }
        }

        private void lookupSupplyByBarcode(String barcode) {
            showLoading(true);
            Callback<Map<String, Object>> callback = new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    showLoading(false);
                    selectedSupply = response.body() == null
                            ? java.util.Collections.<String, Object>emptyMap()
                            : ResponseHelper.unwrapDataMap(response.body(), "milk".equals(doseForm) ? "carton" : "powder_supply");
                    if (selectedSupply.isEmpty()) {
                        Toast.makeText(PullDoseStep3Activity.this, R.string.no_records, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    tvSelectedSupply.setText(buildSupplySummary(selectedSupply));
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                    showLoading(false);
                    Toast.makeText(PullDoseStep3Activity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            if ("milk".equals(doseForm)) {
                ApiClient.getApiService(this).getCartonByBarcode(barcode).enqueue(callback);
            } else {
                ApiClient.getApiService(this).getPowderSupplyByBarcode(barcode).enqueue(callback);
            }
        }

        private void submitPullDose() {
            if (selectedSupply.isEmpty()) {
                Toast.makeText(this, R.string.supply_required, Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("patient_id", getIntent().getIntExtra("patient_id", 0));
            body.put("dose_choice", getIntent().getStringExtra("dose_choice"));
            body.put("custom_dose", getIntent().getStringExtra("custom_dose"));
            body.put("current_dose", getIntent().getStringExtra("current_dose"));
            body.put("dose_form", doseForm);
            body.put("quantity", getIntent().getIntExtra("quantity", 1));
            body.put("shelf_life_hours", getIntent().getIntExtra("shelf_life_hours", 0));
            body.put("seal_type", getIntent().getStringExtra("seal_type"));
            body.put("source_supply_id", ResponseHelper.getInt(selectedSupply, "id"));

            showLoading(true);
            ApiClient.getApiService(this).pullDose(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    showLoading(false);
                    if (response.body() == null) {
                        Toast.makeText(PullDoseStep3Activity.this, R.string.unable_to_load_data, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Map<String, Object>> syringes = ResponseHelper.unwrapDataList(response.body(), "syringes");
                    if (printerManager.isConnected()) {
                        for (Map<String, Object> syringe : syringes) {
                            printerManager.printLabel(buildLabelData(syringe));
                        }
                    }
                    StringBuilder message = new StringBuilder(getString(R.string.dose_created));
                    message.append("\nCreated syringes: ").append(syringes.size());
                    if (!printerManager.isConnected()) {
                        message.append("\n").append(getString(R.string.printer_not_connected));
                    }
                    new AlertDialog.Builder(PullDoseStep3Activity.this)
                            .setTitle(R.string.success)
                            .setMessage(message.toString())
                            .setPositiveButton(R.string.ok, (dialogInterface, i) -> finishAffinity())
                            .show();
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                    showLoading(false);
                    showRetryDialog(throwable.getMessage());
                }
            });
        }

        private PrinterManager.LabelData buildLabelData(Map<String, Object> syringe) {
            PrinterManager.LabelData labelData = new PrinterManager.LabelData();
            labelData.patientName = String.valueOf(sessionManager.getCurrentPatientId());
            labelData.mrn = ResponseHelper.getString(selectedSupply, "patient_mrn");
            labelData.syringeBarcode = ResponseHelper.getString(syringe, "barcode");
            labelData.doseAmount = firstNonEmpty(ResponseHelper.getString(syringe, "dose_amount"), getIntent().getStringExtra("current_dose"));
            labelData.doseForm = doseForm;
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
            labelData.drawDate = now;
            labelData.scheduledDate = now;
            labelData.finalExpiry = firstNonEmpty(ResponseHelper.getString(syringe, "expiry_at"), ResponseHelper.getString(selectedSupply, "expiry_date"));
            labelData.cartonLot = firstNonEmpty(ResponseHelper.getString(selectedSupply, "lot_number"), ResponseHelper.getString(selectedSupply, "barcode"));
            labelData.cartonExpiry = ResponseHelper.getString(selectedSupply, "expiry_date");
            labelData.sealType = getIntent().getStringExtra("seal_type");
            return labelData;
        }

        private String buildSupplySummary(Map<String, Object> supply) {
            return firstNonEmpty(ResponseHelper.getString(supply, "lot_number"), ResponseHelper.getString(supply, "barcode"))
                    + " • " + firstNonEmpty(ResponseHelper.getString(supply, "expiry_date"), ResponseHelper.getString(supply, "status"));
        }

        private String firstNonEmpty(String first, String second) {
            return TextUtils.isEmpty(first) ? (second == null ? "" : second) : first;
        }

        private void showLoading(boolean loading) {
            progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        private void showRetryDialog(String message) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.network_error)
                    .setMessage(message == null ? getString(R.string.unable_to_load_data) : message)
                    .setPositiveButton(R.string.retry, (dialogInterface, i) -> loadSupplies())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_SCAN_SUPPLY && resultCode == RESULT_OK && data != null) {
                lookupSupplyByBarcode(data.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE));
            }
        }

        private static class SupplyAdapter extends RecyclerView.Adapter<SupplyAdapter.SupplyViewHolder> {
            interface OnSupplyClickListener {
                void onSupplyClick(Map<String, Object> supply);
            }

            private final List<Map<String, Object>> items;
            private final OnSupplyClickListener listener;

            SupplyAdapter(List<Map<String, Object>> items, OnSupplyClickListener listener) {
                this.items = items;
                this.listener = listener;
            }

            @NonNull
            @Override
            public SupplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
                return new SupplyViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull SupplyViewHolder holder, int position) {
                Map<String, Object> item = items.get(position);
                holder.title.setText(ResponseHelper.getString(item, "barcode"));
                holder.subtitle.setText(ResponseHelper.getString(item, "expiry_date"));
                holder.itemView.setOnClickListener(view -> listener.onSupplyClick(item));
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            static class SupplyViewHolder extends RecyclerView.ViewHolder {
                TextView title;
                TextView subtitle;

                SupplyViewHolder(@NonNull View itemView) {
                    super(itemView);
                    title = itemView.findViewById(android.R.id.text1);
                    subtitle = itemView.findViewById(android.R.id.text2);
                }
            }
        }
    }
