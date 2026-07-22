package com.oitapp.ui.pulldose;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PullDoseStep1Activity extends AppCompatActivity {
    private SessionManager sessionManager;
    private int patientId;
    private TextView tvPatientName;
    private TextView tvPatientInfo;
    private Chip chipCurrentDose;
    private TextInputEditText etCustomDose;
    private RadioGroup rgDoseChoice;
    private LinearProgressIndicator progressIndicator;
    private Map<String, Object> dosePlan = java.util.Collections.emptyMap();

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
        setContentView(R.layout.activity_pull_dose_step1);
        setTitle(R.string.pull_dose_step1_title);

        tvPatientName = findViewById(R.id.tv_step1_patient_name);
        tvPatientInfo = findViewById(R.id.tv_step1_patient_info);
        chipCurrentDose = findViewById(R.id.chip_step1_current_dose);
        etCustomDose = findViewById(R.id.et_custom_dose);
        rgDoseChoice = findViewById(R.id.rg_dose_choice);
        progressIndicator = findViewById(R.id.progress_step1);
        MaterialButton btnNext = findViewById(R.id.btn_step1_next);
        btnNext.setOnClickListener(view -> continueToNextStep());

        loadPatientData();
        loadDosePlan();
    }

    private void loadPatientData() {
        showLoading(true);
        ApiClient.getApiService(this).getPatient(patientId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                showLoading(false);
                Map<String, Object> patient = response.body() == null
                        ? java.util.Collections.<String, Object>emptyMap()
                        : ResponseHelper.unwrapDataMap(response.body(), "patient");
                String name = ResponseHelper.getString(patient, "name");
                if (name.isEmpty()) {
                    name = (ResponseHelper.getString(patient, "first_name") + " "
                            + ResponseHelper.getString(patient, "last_name")).trim();
                }
                tvPatientName.setText(name);
                tvPatientInfo.setText(getString(R.string.mrn) + ": " + ResponseHelper.getString(patient, "mrn")
                        + " • " + getString(R.string.allergen) + ": " + ResponseHelper.getString(patient, "allergen"));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showLoading(false);
                showRetryDialog(() -> loadPatientData(), throwable.getMessage());
            }
        });
    }

    private void loadDosePlan() {
        showLoading(true);
        ApiClient.getApiService(this).getDosePlan(patientId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                showLoading(false);
                dosePlan = response.body() == null
                        ? java.util.Collections.<String, Object>emptyMap()
                        : ResponseHelper.unwrapDataMap(response.body(), "dose_plan");
                chipCurrentDose.setText(getString(R.string.current_dose) + ": "
                        + ResponseHelper.getString(dosePlan, "current_dose"));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showLoading(false);
            }
        });
    }

    private void continueToNextStep() {
        int checkedId = rgDoseChoice.getCheckedRadioButtonId();
        if (checkedId == -1) {
            Toast.makeText(this, R.string.dose_selection_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String selection = checkedId == R.id.rb_continue ? "continue" : checkedId == R.id.rb_updose ? "updose" : "custom";
        String customDose = etCustomDose.getText() == null ? "" : etCustomDose.getText().toString().trim();
        if ("custom".equals(selection) && TextUtils.isEmpty(customDose)) {
            etCustomDose.setError(getString(R.string.custom_dose));
            return;
        }
        Intent intent = new Intent(this, PullDoseStep2Activity.class);
        intent.putExtra("patient_id", patientId);
        intent.putExtra("dose_choice", selection);
        intent.putExtra("custom_dose", customDose);
        intent.putExtra("current_dose", ResponseHelper.getString(dosePlan, "current_dose"));
        startActivity(intent);
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
}
