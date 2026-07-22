package com.oitapp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.ui.completedose.CompleteDoseActivity;
import com.oitapp.ui.foodlog.FoodLogActivity;
import com.oitapp.ui.login.LoginActivity;
import com.oitapp.ui.missions.MissionsActivity;
import com.oitapp.ui.patients.PatientListActivity;
import com.oitapp.ui.pulldose.PullDoseStep1Activity;
import com.oitapp.ui.symptomlog.SymptomLogActivity;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private TextView tvCurrentUser;
    private TextView tvSelectedPatient;
    private TextView tvStatMissions;
    private TextView tvStatDoses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (sessionManager.redirectToLoginIfNeeded(this)) {
            return;
        }
        setContentView(R.layout.activity_main);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        tvCurrentUser = findViewById(R.id.tv_current_user);
        tvSelectedPatient = findViewById(R.id.tv_selected_patient);
        tvStatMissions = findViewById(R.id.tv_stat_missions);
        tvStatDoses = findViewById(R.id.tv_stat_doses);

        TextView tvHeaderName = navigationView.getHeaderView(0).findViewById(R.id.tv_nav_header_name);
        TextView tvHeaderRole = navigationView.getHeaderView(0).findViewById(R.id.tv_nav_header_role);
        String username = sessionManager.getUsername();
        String role = sessionManager.getRole();
        tvCurrentUser.setText(getString(R.string.current_user) + ": " + username);
        tvSelectedPatient.setText(buildSelectedPatientText());
        tvHeaderName.setText(username.isEmpty() ? getString(R.string.app_name) : username);
        tvHeaderRole.setText(getString(R.string.role) + ": " + role);

        setMenuButton(R.id.btn_patients, PatientListActivity.class);
        setMenuButton(R.id.btn_pull_dose, PullDoseStep1Activity.class);
        setMenuButton(R.id.btn_complete_dose, CompleteDoseActivity.class);
        setMenuButton(R.id.btn_food_log, FoodLogActivity.class);
        setMenuButton(R.id.btn_symptom_log, SymptomLogActivity.class);
        setMenuButton(R.id.btn_missions, MissionsActivity.class);

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            drawerLayout.closeDrawers();
            if (itemId == R.id.nav_logout) {
                logout();
                return true;
            }
            if (itemId == R.id.nav_patients) {
                openScreen(PatientListActivity.class);
            } else if (itemId == R.id.nav_pull_dose) {
                openScreen(PullDoseStep1Activity.class);
            } else if (itemId == R.id.nav_complete_dose) {
                openScreen(CompleteDoseActivity.class);
            } else if (itemId == R.id.nav_food_log) {
                openScreen(FoodLogActivity.class);
            } else if (itemId == R.id.nav_symptom_log) {
                openScreen(SymptomLogActivity.class);
            } else if (itemId == R.id.nav_missions) {
                openScreen(MissionsActivity.class);
            }
            return true;
        });

        loadDashboardStats();
    }

    private void setMenuButton(int buttonId, final Class<?> targetActivity) {
        MaterialButton button = findViewById(buttonId);
        button.setOnClickListener(view -> openScreen(targetActivity));
    }

    private void openScreen(Class<?> targetActivity) {
        if (requiresPatient(targetActivity) && sessionManager.getCurrentPatientId() == 0) {
            Toast.makeText(this, R.string.patient_required, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, PatientListActivity.class));
            return;
        }
        startActivity(new Intent(this, targetActivity));
    }

    private boolean requiresPatient(Class<?> targetActivity) {
        return targetActivity == PullDoseStep1Activity.class
                || targetActivity == CompleteDoseActivity.class
                || targetActivity == FoodLogActivity.class
                || targetActivity == SymptomLogActivity.class
                || targetActivity == MissionsActivity.class;
    }

    private void loadDashboardStats() {
        final int patientId = sessionManager.getCurrentPatientId();
        if (patientId == 0) {
            tvStatMissions.setText("0");
            tvStatDoses.setText("0");
            return;
        }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        ApiClient.getApiService(this).getMissions(patientId, today).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                List<Map<String, Object>> missions = response.body() == null
                        ? java.util.Collections.<Map<String, Object>>emptyList()
                        : ResponseHelper.unwrapDataList(response.body(), "missions");
                tvStatMissions.setText(String.valueOf(missions.size()));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                tvStatMissions.setText("0");
            }
        });
        ApiClient.getApiService(this).getNotifications(patientId, true).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                List<Map<String, Object>> notifications = response.body() == null
                        ? java.util.Collections.<Map<String, Object>>emptyList()
                        : ResponseHelper.unwrapDataList(response.body(), "notifications");
                tvStatDoses.setText(String.valueOf(notifications.size()));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                tvStatDoses.setText("0");
            }
        });
    }

    private String buildSelectedPatientText() {
        int patientId = sessionManager.getCurrentPatientId();
        if (patientId == 0) {
            return getString(R.string.select_patient_prompt);
        }
        return getString(R.string.selected_patient) + ": #" + patientId;
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
