package com.oitapp.ui.scanner;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.oitapp.R;
import com.oitapp.utils.SessionManager;

public class BarcodeScannerActivity extends AppCompatActivity {
    public static final String EXTRA_BARCODE = "extra_barcode";
    private boolean scanLaunched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (new SessionManager(this).redirectToLoginIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_barcode_scanner);
        setTitle(R.string.scanner_prompt);

        if (savedInstanceState != null) {
            scanLaunched = savedInstanceState.getBoolean("scan_launched", false);
        }

        MaterialButton btnOpenScanner = findViewById(R.id.btn_open_scanner);
        MaterialButton btnCancel = findViewById(R.id.btn_cancel_scan);
        btnOpenScanner.setOnClickListener(view -> launchScanner());
        btnCancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (!scanLaunched) {
            findViewById(R.id.btn_open_scanner).post(this::launchScanner);
        }
    }

    private void launchScanner() {
        scanLaunched = true;
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt(getString(R.string.scanner_prompt));
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(CaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("scan_launched", scanLaunched);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                Intent output = new Intent();
                output.putExtra(EXTRA_BARCODE, result.getContents());
                setResult(RESULT_OK, output);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
