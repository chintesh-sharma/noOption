package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity {

    TextView tvScreenTime, tvFocusStatus, tvPermanentApps, tvWeeklySummary;
    Button btnEditBlocked, btnPermanentApps, btnReschedule;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs = getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        tvScreenTime = findViewById(R.id.tvScreenTime);
        tvFocusStatus = findViewById(R.id.tvFocusStatus);
        tvPermanentApps = findViewById(R.id.tvPermanentApps);
        tvWeeklySummary = findViewById(R.id.tvWeeklySummary);

        btnEditBlocked = findViewById(R.id.btnEditBlocked);
        btnPermanentApps = findViewById(R.id.btnPermanentApps);
        btnReschedule = findViewById(R.id.btnReschedule);

        btnEditBlocked.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppSelectionActivity.class);
            intent.putExtra("EDIT_MODE", true);
            startActivity(intent);
        });

        btnPermanentApps.setOnClickListener(v ->
                startActivity(
                        new Intent(this, PermanentAppsViewActivity.class)
                )
        );

        // 🔥 TEMP apps reschedule ONLY (EDIT MODE)
        btnReschedule.setOnClickListener(v -> {
            Intent i = new Intent(this, FocusTimerActivity.class);
            i.putExtra("EDIT_MODE", true);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatus();
    }

    private void loadStatus() {

        // 🔥 Focus status
        boolean focusOn = isAnyFocusActiveNow();
        tvFocusStatus.setText(
                focusOn ? "Focus Mode: ON" : "Focus Mode: OFF"
        );

        // ⏱ Screen time
        long screenTimeMs = getTodayScreenTimeMillis();
        long minutes = screenTimeMs / (1000 * 60);
        tvScreenTime.setText("Today's Screen Time: " + minutes + " min");

        // 📊 Weekly summary
        tvWeeklySummary.setText("Weekly Screen Time: Coming Soon");

        // 🔒 Permanent apps (VIEW ONLY)
        Set<String> permanentApps =
                prefs.getStringSet(
                        "PERMANENT_BLOCKED_APPS",
                        new HashSet<>()
                );

        if (permanentApps.isEmpty()) {
            tvPermanentApps.setText("Permanent Apps: None");
        } else {
            StringBuilder sb = new StringBuilder("Permanent Apps:\n");
            for (String pkg : permanentApps) {
                sb.append("• ").append(pkg).append("\n");
            }
            tvPermanentApps.setText(sb.toString());
        }

        // ✅ TEMP reschedule ALWAYS allowed
        btnReschedule.setEnabled(true);
        btnReschedule.setAlpha(1f);
    }

    private boolean isAnyFocusActiveNow() {

        Set<String> permanentApps =
                prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());

        Set<String> blockedApps =
                prefs.getStringSet("BLOCKED_APPS", new HashSet<>());

        if (permanentApps.isEmpty() && blockedApps.isEmpty()) {
            return false;
        }

        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);

        int pSh = prefs.getInt("PERM_START_HOUR", -1);
        int pSm = prefs.getInt("PERM_START_MIN", -1);
        int pEh = prefs.getInt("PERM_END_HOUR", -1);
        int pEm = prefs.getInt("PERM_END_MIN", -1);

        if (!permanentApps.isEmpty()
                && pSh != -1
                && isWithinFocusTime(h, m, pSh, pSm, pEh, pEm)) {
            return true;
        }

        int tSh = prefs.getInt("TEMP_START_HOUR", -1);
        int tSm = prefs.getInt("TEMP_START_MIN", -1);
        int tEh = prefs.getInt("TEMP_END_HOUR", -1);
        int tEm = prefs.getInt("TEMP_END_MIN", -1);

        return tSh != -1
                && isWithinFocusTime(h, m, tSh, tSm, tEh, tEm);
    }

    private boolean isWithinFocusTime(
            int h, int m,
            int sh, int sm,
            int eh, int em) {

        int current = h * 60 + m;
        int start = sh * 60 + sm;
        int end = eh * 60 + em;

        if (start < end) {
            return current >= start && current < end;
        } else {
            return current >= start || current < end;
        }
    }

    private long getTodayScreenTimeMillis() {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long startOfDay = cal.getTimeInMillis();
        long end = System.currentTimeMillis();

        android.app.usage.UsageStatsManager usm =
                (android.app.usage.UsageStatsManager)
                        getSystemService(USAGE_STATS_SERVICE);

        android.app.usage.UsageEvents events =
                usm.queryEvents(startOfDay, end);

        android.app.usage.UsageEvents.Event event =
                new android.app.usage.UsageEvents.Event();

        long totalTime = 0;
        long lastForegroundTime = 0;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            if (event.getEventType()
                    == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {

                lastForegroundTime = event.getTimeStamp();

            } else if (event.getEventType()
                    == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND) {

                if (lastForegroundTime != 0) {
                    totalTime += (event.getTimeStamp() - lastForegroundTime);
                    lastForegroundTime = 0;
                }
            }
        }

        return totalTime;
    }
}