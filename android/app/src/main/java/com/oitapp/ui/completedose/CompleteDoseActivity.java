    package com.oitapp.ui.completedose;

    import android.app.AlertDialog;
    import android.content.Intent;
    import android.os.Bundle;
    import android.text.TextUtils;
    import android.widget.RadioGroup;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AppCompatActivity;
    import com.google.android.material.button.MaterialButton;
    import com.google.android.material.textfield.TextInputEditText;
    import com.oitapp.R;
    import com.oitapp.api.ApiClient;
    import com.oitapp.ui.scanner.BarcodeScannerActivity;
    import com.oitapp.utils.ResponseHelper;
    import com.oitapp.utils.SessionManager;
    import java.util.HashMap;
    import java.util.Map;
    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    public class CompleteDoseActivity extends AppCompatActivity {
        private static final int REQUEST_SCAN_SYRINGE = 1003;
        private SessionManager sessionManager;
        private int patientId;
        private TextView tvPatient;
        private TextView tvSyringeInfo;
        private TextInputEditText etNotes;
        private RadioGroup rgOutcome;
        private Map<String, Object> currentSyringe = java.util.Collections.emptyMap();

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
            setContentView(R.layout.activity_complete_dose);
            setTitle(R.string.complete_dose);

            tvPatient = findViewById(R.id.tv_complete_patient);
            tvSyringeInfo = findViewById(R.id.tv_syringe_info);
            etNotes = findViewById(R.id.et_complete_notes);
            rgOutcome = findViewById(R.id.rg_outcome);
            MaterialButton btnScan = findViewById(R.id.btn_scan_syringe);
            MaterialButton btnAdminister = findViewById(R.id.btn_administer);

            tvPatient.setText(getString(R.string.active_patient) + ": #" + patientId);
            btnScan.setOnClickListener(view -> startActivityForResult(new Intent(this, BarcodeScannerActivity.class), REQUEST_SCAN_SYRINGE));
            btnAdminister.setOnClickListener(view -> submitAdministration());
        }

        private void lookupSyringe(String barcode) {
            ApiClient.getApiService(this).getSyringeByBarcode(barcode).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    currentSyringe = response.body() == null
                            ? java.util.Collections.<String, Object>emptyMap()
                            : ResponseHelper.unwrapDataMap(response.body(), "syringe");
                    if (!isSyringeValid(currentSyringe)) {
                        currentSyringe = java.util.Collections.emptyMap();
                        Toast.makeText(CompleteDoseActivity.this, R.string.syringe_verification_failed, Toast.LENGTH_LONG).show();
                        tvSyringeInfo.setText(getString(R.string.syringe_info));
                        return;
                    }
                    tvSyringeInfo.setText(buildSyringeSummary(currentSyringe));
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                    Toast.makeText(CompleteDoseActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private boolean isSyringeValid(Map<String, Object> syringe) {
            if (syringe.isEmpty()) {
                return false;
            }
            int syringePatientId = ResponseHelper.getInt(syringe, "patient_id");
            String status = ResponseHelper.getString(syringe, "status");
            return (syringePatientId == 0 || syringePatientId == patientId)
                    && !"used".equalsIgnoreCase(status)
                    && !"discarded".equalsIgnoreCase(status)
                    && !"expired".equalsIgnoreCase(status);
        }

        private void submitAdministration() {
            if (currentSyringe.isEmpty()) {
                Toast.makeText(this, R.string.scan_syringe_barcode, Toast.LENGTH_SHORT).show();
                return;
            }
            int checkedId = rgOutcome.getCheckedRadioButtonId();
            String outcome = checkedId == R.id.rb_outcome_reaction ? "reaction"
                    : checkedId == R.id.rb_outcome_missed ? "missed" : "completed";
            Map<String, Object> body = new HashMap<>();
            body.put("patient_id", patientId);
            body.put("syringe_id", ResponseHelper.getInt(currentSyringe, "id"));
            body.put("barcode", ResponseHelper.getString(currentSyringe, "barcode"));
            body.put("outcome", outcome);
            body.put("notes", etNotes.getText() == null ? "" : etNotes.getText().toString().trim());

            ApiClient.getApiService(this).completeDose(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    if (!response.isSuccessful()) {
                        showRetryDialog(getString(R.string.unable_to_load_data));
                        return;
                    }
                    Toast.makeText(CompleteDoseActivity.this, R.string.medication_recorded, Toast.LENGTH_SHORT).show();
                    currentSyringe = java.util.Collections.emptyMap();
                    tvSyringeInfo.setText(getString(R.string.syringe_info));
                    rgOutcome.check(R.id.rb_outcome_completed);
                    etNotes.setText(null);
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                    showRetryDialog(throwable.getMessage());
                }
            });
        }

        private void showRetryDialog(String message) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.network_error)
                    .setMessage(message)
                    .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                        if (!currentSyringe.isEmpty()) {
                            lookupSyringe(ResponseHelper.getString(currentSyringe, "barcode"));
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private String buildSyringeSummary(Map<String, Object> syringe) {
            return "Barcode: " + ResponseHelper.getString(syringe, "barcode")
                    + "\nDose: " + firstNonEmpty(ResponseHelper.getString(syringe, "dose_amount"), ResponseHelper.getString(syringe, "dose"))
                    + "\nExpiry: " + ResponseHelper.getString(syringe, "expiry_at")
                    + "\nStatus: " + ResponseHelper.getString(syringe, "status");
        }

        private String firstNonEmpty(String first, String second) {
            return TextUtils.isEmpty(first) ? (second == null ? "" : second) : first;
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_SCAN_SYRINGE && resultCode == RESULT_OK && data != null) {
                lookupSyringe(data.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE));
            }
        }
    }
