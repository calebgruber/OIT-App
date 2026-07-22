package com.oitapp.ui.symptomlog;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
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

public class SymptomLogActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private int patientId;
    private TextView tvSeverityValue;
    private SeekBar seekSeverity;
    private TextInputEditText etNotes;
    private CheckBox[] symptomBoxes;

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
        setContentView(R.layout.activity_symptom_log);
        setTitle(R.string.symptom_log);

        TextView tvPatient = findViewById(R.id.tv_symptom_patient);
        tvPatient.setText(getString(R.string.active_patient) + ": #" + patientId);
        tvSeverityValue = findViewById(R.id.tv_severity_value);
        seekSeverity = findViewById(R.id.seek_severity);
        etNotes = findViewById(R.id.et_symptom_notes);
        symptomBoxes = new CheckBox[]{
                findViewById(R.id.cb_hives),
                findViewById(R.id.cb_swelling),
                findViewById(R.id.cb_itching),
                findViewById(R.id.cb_vomiting),
                findViewById(R.id.cb_cough),
                findViewById(R.id.cb_wheeze),
                findViewById(R.id.cb_other)
        };
        MaterialButton btnSubmit = findViewById(R.id.btn_submit_symptom);
        updateSeverityLabel(1);
        seekSeverity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSeverityLabel(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        btnSubmit.setOnClickListener(view -> submitSymptomLog());
    }

    private void updateSeverityLabel(int severity) {
        tvSeverityValue.setText(getString(R.string.severity_level, severity));
    }

    private void submitSymptomLog() {
        List<String> symptoms = new ArrayList<>();
        for (CheckBox checkBox : symptomBoxes) {
            if (checkBox.isChecked()) {
                symptoms.add(checkBox.getText().toString());
            }
        }
        if (symptoms.isEmpty()) {
            Toast.makeText(this, R.string.symptom_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("patient_id", patientId);
        body.put("severity", seekSeverity.getProgress() + 1);
        body.put("symptoms", symptoms);
        body.put("notes", etNotes.getText() == null ? "" : etNotes.getText().toString().trim());
        body.put("logged_at", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()));
        ApiClient.getApiService(this).createSymptomLog(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Toast.makeText(SymptomLogActivity.this, R.string.symptom_logged_success, Toast.LENGTH_SHORT).show();
                for (CheckBox checkBox : symptomBoxes) {
                    checkBox.setChecked(false);
                }
                etNotes.setText(null);
                seekSeverity.setProgress(0);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                Toast.makeText(SymptomLogActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
