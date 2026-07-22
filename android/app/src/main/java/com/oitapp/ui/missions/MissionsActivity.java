package com.oitapp.ui.missions;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.oitapp.R;
import com.oitapp.api.ApiClient;
import com.oitapp.utils.ResponseHelper;
import com.oitapp.utils.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionsActivity extends AppCompatActivity {
    private final List<Map<String, Object>> missions = new ArrayList<>();
    private MissionAdapter adapter;
    private SessionManager sessionManager;
    private int patientId;
    private ChipGroup chipGroupBadges;

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
        setContentView(R.layout.activity_missions);
        setTitle(R.string.daily_missions);

        chipGroupBadges = findViewById(R.id.chip_group_badges);
        RecyclerView recyclerView = findViewById(R.id.rv_missions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MissionAdapter(missions, mission -> completeMission(ResponseHelper.getInt(mission, "id")));
        recyclerView.setAdapter(adapter);

        loadMissions();
        loadBadges();
    }

    private void loadMissions() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        ApiClient.getApiService(this).getMissions(patientId, today).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                missions.clear();
                if (response.body() != null) {
                    missions.addAll(ResponseHelper.unwrapDataList(response.body(), "missions"));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                showRetryDialog(throwable.getMessage());
            }
        });
    }

    private void loadBadges() {
        ApiClient.getApiService(this).getBadges(patientId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                chipGroupBadges.removeAllViews();
                List<Map<String, Object>> badges = response.body() == null
                        ? java.util.Collections.<Map<String, Object>>emptyList()
                        : ResponseHelper.unwrapDataList(response.body(), "badges");
                if (badges.isEmpty()) {
                    Chip chip = new Chip(MissionsActivity.this);
                    chip.setText(R.string.no_badges);
                    chipGroupBadges.addView(chip);
                    return;
                }
                for (Map<String, Object> badge : badges) {
                    Chip chip = new Chip(MissionsActivity.this);
                    chip.setText(ResponseHelper.getString(badge, "name"));
                    chipGroupBadges.addView(chip);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable throwable) {
                chipGroupBadges.removeAllViews();
            }
        });
    }

    private void completeMission(final int missionId) {
        ApiClient.getApiService(this).completeMission(missionId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Toast.makeText(MissionsActivity.this, R.string.mission_completed_success, Toast.LENGTH_SHORT).show();
                loadMissions();
                loadBadges();
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
                .setMessage(message == null ? getString(R.string.unable_to_load_data) : message)
                .setPositiveButton(R.string.retry, (dialogInterface, i) -> loadMissions())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.MissionViewHolder> {
        interface OnMissionActionListener {
            void onComplete(Map<String, Object> mission);
        }

        private final List<Map<String, Object>> items;
        private final OnMissionActionListener listener;

        MissionAdapter(List<Map<String, Object>> items, OnMissionActionListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public MissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission, parent, false);
            return new MissionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MissionViewHolder holder, int position) {
            Map<String, Object> mission = items.get(position);
            holder.title.setText(ResponseHelper.getString(mission, "type"));
            holder.subtitle.setText(ResponseHelper.getString(mission, "description"));
            holder.status.setText(ResponseHelper.getString(mission, "status"));
            holder.button.setOnClickListener(view -> listener.onComplete(mission));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class MissionViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;
            TextView status;
            MaterialButton button;

            MissionViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_item_title);
                subtitle = itemView.findViewById(R.id.tv_item_subtitle);
                status = itemView.findViewById(R.id.tv_item_status);
                button = itemView.findViewById(R.id.btn_item_action);
            }
        }
    }
}
