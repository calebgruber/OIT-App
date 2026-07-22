package com.oitapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.oitapp.ui.login.LoginActivity;

public class SessionManager {
    public static final String DEFAULT_API_URL = "http://10.0.2.2:3000/api/v1/";
    private static final String PREF_NAME = "oit_app_prefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_CURRENT_PATIENT_ID = "current_patient_id";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        SharedPreferences resolvedPreferences;
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                resolvedPreferences = EncryptedSharedPreferences.create(
                        PREF_NAME,
                        masterKeyAlias,
                        appContext,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (Exception exception) {
                resolvedPreferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            }
        } else {
            resolvedPreferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        preferences = resolvedPreferences;
    }

    public void saveToken(String token) {
        preferences.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    public void saveUserSession(String token, int userId, String username, String role) {
        preferences.edit()
                .putString(KEY_AUTH_TOKEN, token)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public String getToken() {
        return preferences.getString(KEY_AUTH_TOKEN, "");
    }

    public int getUserId() {
        return preferences.getInt(KEY_USER_ID, 0);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, "");
    }

    public void clearSession() {
        String apiUrl = getApiUrl();
        preferences.edit().clear().putString(KEY_API_URL, apiUrl).apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && token.trim().length() > 0;
    }

    public void setCurrentPatient(int patientId) {
        preferences.edit().putInt(KEY_CURRENT_PATIENT_ID, patientId).apply();
    }

    public int getCurrentPatientId() {
        return preferences.getInt(KEY_CURRENT_PATIENT_ID, 0);
    }

    public String getApiUrl() {
        String apiUrl = preferences.getString(KEY_API_URL, DEFAULT_API_URL);
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return DEFAULT_API_URL;
        }
        return apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
    }

    public void setApiUrl(String apiUrl) {
        String normalized = (apiUrl == null || apiUrl.trim().isEmpty()) ? DEFAULT_API_URL : apiUrl.trim();
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        preferences.edit().putString(KEY_API_URL, normalized).apply();
    }

    public boolean redirectToLoginIfNeeded(Activity activity) {
        if (isLoggedIn()) {
            return false;
        }
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }
}
