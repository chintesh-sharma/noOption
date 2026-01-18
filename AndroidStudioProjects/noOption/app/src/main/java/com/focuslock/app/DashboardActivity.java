package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
    PackageManager pm;

    // 🔥 TEST VALUE (CHANGE TO 1 MONTH LATER)
    private static final long PERMANENT_LOCK_DURATION =
            30L * 24 * 60 * 60 * 1000; // 30 days


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs = getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);
        pm = getPackageManager();

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

        // 🔓 Permanent unlock → Permission setup
        btnPermanentApps.setOnClickListener(v ->
                startActivity(new Intent(this, PermissionSetupActivity.class))
        );

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

        // 🔥 Focus status (UNCHANGED)
        boolean focusOn = isAnyFocusActiveNow();
        tvFocusStatus.setText(
                focusOn ? "Focus Mode: ON" : "Focus Mode: OFF"
        );

        // ⏱ Screen time (UNCHANGED FORMAT LOGIC)
        long screenTimeMs = getTodayScreenTimeMillis();
        long totalMinutes = screenTimeMs / (1000 * 60);

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0) {
            tvScreenTime.setText(
                    "Today's Screen Time: " + hours + "h " + minutes + "m"
            );
        } else {
            tvScreenTime.setText(
                    "Today's Screen Time: " + minutes + " min"
            );
        }

        // 📊 Weekly summary (UNCHANGED)
        tvWeeklySummary.setText("Weekly Screen Time: Coming Soon");

        // 🔒 Permanent apps (UNCHANGED)
        Set<String> permanentApps =
                prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());

        if (permanentApps.isEmpty()) {
            tvPermanentApps.setText("Permanent Apps: None");
        } else {
            StringBuilder sb = new StringBuilder("Permanent Apps:\n");

            for (String pkg : permanentApps) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    String appName = pm.getApplicationLabel(info).toString();
                    sb.append("• ").append(appName).append("\n");
                } catch (PackageManager.NameNotFoundException e) {
                    sb.append("• ").append(pkg).append("\n");
                }
            }
            tvPermanentApps.setText(sb.toString());
        }

        // ============================
        // 🔐 PERMANENT LOCK CHECK + TIMER TEXT (ADD ONLY)
        // ============================
        long lockStart = prefs.getLong("PERMANENT_LOCK_START", -1);

        boolean permanentUnlocked = false;

        if (lockStart != -1) {
            long now = System.currentTimeMillis();
            permanentUnlocked =
                    (now - lockStart) >= PERMANENT_LOCK_DURATION;
        }

        if (permanentUnlocked) {
            btnPermanentApps.setEnabled(true);
            btnPermanentApps.setAlpha(1f);
            btnPermanentApps.setText("Reset Setup");
        } else {
            btnPermanentApps.setEnabled(false);
            btnPermanentApps.setAlpha(0.4f);

            long remaining =
                    PERMANENT_LOCK_DURATION
                            - (System.currentTimeMillis() - lockStart);

            if (remaining < 0) remaining = 0;

            btnPermanentApps.setText(
                    "Reset setup in " + formatRemainingTime(remaining)
            );
        }

        // ✅ TEMP reschedule ALWAYS allowed (UNCHANGED)
        btnReschedule.setEnabled(true);
        btnReschedule.setAlpha(1f);
    }

    // 🔹 ONLY FORMAT METHOD (NO LOGIC CHANGE)
    private String formatRemainingTime(long remainingMs) {

        long totalSeconds = remainingMs / 1000;

        long days = totalSeconds / (24 * 60 * 60);
        totalSeconds %= (24 * 60 * 60);

        long hours = totalSeconds / (60 * 60);
        totalSeconds %= (60 * 60);

        long minutes = totalSeconds / 60;

        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    // ================= EXISTING METHODS (UNCHANGED) =================

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
                    totalTime +=
                            (event.getTimeStamp() - lastForegroundTime);
                    lastForegroundTime = 0;
                }
            }
        }

        return totalTime;
    }
}
