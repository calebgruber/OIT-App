package com.oitapp.ui.login;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.ui.main.MainActivity;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private LinearProgressIndicator progressIndicator;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            openMainScreen();
            return;
        }

        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        progressIndicator = findViewById(R.id.progress_login);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        MaterialButton btnApiUrl = findViewById(R.id.btn_api_url);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.logging_in));

        btnLogin.setOnClickListener(view -> attemptLogin());
        btnApiUrl.setOnClickListener(view -> showApiUrlDialog());
        etPassword.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void attemptLogin() {
        tilUsername.setError(null);
        tilPassword.setError(null);

        String username = etUsername.getText() == null ? "" : etUsername.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(username)) {
                tilUsername.setError(getString(R.string.username_password_required));
            }
            if (TextUtils.isEmpty(password)) {
                tilPassword.setError(getString(R.string.username_password_required));
            }
            return;
        }

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        showLoading(true);
        ApiClient.getApiService(this).login(credentials).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorDialog(getString(R.string.invalid_credentials));
                    return;
                }
                Map<String, Object> body = response.body();
                Map<String, Object> data = ResponseHelper.getMap(body, "data");
                String token = firstNonEmpty(
                        ResponseHelper.getString(body, "token"),
                        ResponseHelper.getString(body, "auth_token"),
                        ResponseHelper.getString(data, "token"),
                        ResponseHelper.getString(data, "auth_token")
                );
                Map<String, Object> user = ResponseHelper.getMap(body, "user");
                if (user.isEmpty()) {
                    user = ResponseHelper.getMap(data, "user");
                }
                if (TextUtils.isEmpty(token)) {
                    showErrorDialog(getString(R.string.login_failed));
                    return;
                }
                int userId = ResponseHelper.getInt(user, "id");
                String resolvedUsername = firstNonEmpty(
                        ResponseHelper.getString(user, "username"),
                        ResponseHelper.getString(data, "username"),
                        username
                );
                String role = firstNonEmpty(
                        ResponseHelper.getString(user, "role"),
                        ResponseHelper.getString(data, "role"),
                        "Clinician"
                );
                sessionManager.saveUserSession(token, userId, resolvedUsername, role);
                openMainScreen();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable throwable) {
                showLoading(false);
                showRetryDialog(throwable.getMessage());
            }
        });
    }

    private void showApiUrlDialog() {
        final EditText input = new EditText(this);
        input.setText(sessionManager.getApiUrl());
        input.setHint(getString(R.string.api_default_hint));
        new AlertDialog.Builder(this)
                .setTitle(R.string.configure_api_url)
                .setView(input)
                .setPositiveButton(R.string.save, (dialogInterface, i) -> {
                    sessionManager.setApiUrl(input.getText() == null ? "" : input.getText().toString());
                    Toast.makeText(this, R.string.api_url_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        if (loading) {
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.login_failed)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showRetryDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.network_error)
                .setMessage(message == null ? getString(R.string.unable_to_load_data) : message)
                .setPositiveButton(R.string.retry, (dialogInterface, i) -> attemptLogin())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openMainScreen() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
