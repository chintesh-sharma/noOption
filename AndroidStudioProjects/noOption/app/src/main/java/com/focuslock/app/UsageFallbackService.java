package com.focuslock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class UsageFallbackService extends Service {

    private static final String CHANNEL_ID = "focus_fallback";

    private Handler handler;
    private Runnable checker;

    // ✅ FIX: Cached last foreground pkg (Overlay sticky banega)
    private String lastForegroundPkg = "";



    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Focus Mode Active")
                        .setContentText("Distractions are blocked")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setOngoing(true)
                        .build();

        // ✅ Foreground service stable
        startForeground(101, notification);

        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (checker != null) return START_STICKY;

        checker = () -> {
            try {
                checkAndControlOverlay();
            } catch (Exception ignored) {}

            handler.postDelayed(checker, 20);
        };

        handler.post(checker);
        return START_STICKY;
    }

    // =================================================
    // ✅ CORE BRAIN (FINAL FIXED)
    // =================================================
    private void checkAndControlOverlay() {


        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        boolean recentsOpen =
                prefs.getBoolean("RECENTS_OPEN", false);

        if (recentsOpen) {

            startOverlay();

            prefs.edit()
                    .putBoolean("RECENTS_OPEN", false)
                    .apply();

            return;
        }

        long unlockUntil =
                prefs.getLong("EMERGENCY_UNLOCK_UNTIL", 0);

        boolean emergencyActive =
                System.currentTimeMillis() < unlockUntil;

        if (emergencyActive) {
            stopOverlay();
            return;
        }




        // ❌ Setup ke time kabhi overlay nahi
        if (prefs.getBoolean("SETUP_IN_PROGRESS", false)) {
            stopOverlay();
            return;
        }

         recentsOpen =
                prefs.getBoolean("RECENTS_OPEN", false);

        if (recentsOpen) {

            startOverlay();

            prefs.edit().putBoolean("RECENTS_OPEN", false).apply();

            return;
        }

        // ✅ Foreground package stable
        String foregroundPkg = getForegroundPackage();

        if (foregroundPkg == null || foregroundPkg.isEmpty()) {
            stopOverlay();
            return;
        }

        // ❌ Apni app pe overlay nahi
        if (foregroundPkg.equals(getPackageName())) {
            stopOverlay();
            return;
        }

        // ✅ Permanent blocked apps list
        Set<String> permanentApps =
                new HashSet<>(prefs.getStringSet(
                        "PERMANENT_BLOCKED_APPS",
                        new HashSet<>()
                ));

        // ✅ Focus Active Checks
        boolean permActive = isPermanentFocusActive(prefs);
        boolean webActive = isWebsiteFocusActive(prefs);

        // ✅ SETTINGS Overlay → Permanent + Website focus dono me
        if (foregroundPkg.equals("com.android.settings")
                && (permActive || webActive)) {

            startOverlay();
            return;
        }

        // ✅ Blocked Apps Overlay → ONLY Permanent focus time me
        if (permActive && permanentApps.contains(foregroundPkg)) {

            startOverlay();
            return;
        }

        // ❌ Browser / Websites ka koi overlay nahi
        stopOverlay();
    }

    // =================================================
    // ✅ PERMANENT FOCUS TIME CHECK
    // =================================================
    private boolean isPermanentFocusActive(SharedPreferences prefs) {

        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);

        int sh = prefs.getInt("PERM_START_HOUR", -1);
        if (sh == -1) return false;

        return isWithin(
                h, m,
                sh,
                prefs.getInt("PERM_START_MIN", 0),
                prefs.getInt("PERM_END_HOUR", 0),
                prefs.getInt("PERM_END_MIN", 0)
        );
    }

    // =================================================
    // ✅ WEBSITE FOCUS TIME CHECK (ONLY SETTINGS Overlay)
    // =================================================
    private boolean isWebsiteFocusActive(SharedPreferences prefs) {

        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);

        int sh = prefs.getInt("WEB_START_HOUR", -1);
        if (sh == -1) return false;

        return isWithin(
                h, m,
                sh,
                prefs.getInt("WEB_START_MIN", 0),
                prefs.getInt("WEB_END_HOUR", 0),
                prefs.getInt("WEB_END_MIN", 0)
        );
    }

    // =================================================
    // ✅ FOREGROUND PACKAGE DETECTOR (STICKY FIXED)
    // =================================================
    private String getForegroundPackage() {

        UsageStatsManager usm =
                (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

        long end = System.currentTimeMillis();
        long begin = end - 10000;

        UsageEvents events = usm.queryEvents(begin, end);
        UsageEvents.Event event = new UsageEvents.Event();

        while (events.hasNextEvent()) {
            events.getNextEvent(event);

            if (event.getEventType()
                    == UsageEvents.Event.MOVE_TO_FOREGROUND) {

                lastForegroundPkg = event.getPackageName();
            }
        }

        // ✅ Return cached always
        return lastForegroundPkg;
    }

    // =================================================
    // ✅ TIME HELPER (UNCHANGED)
    // =================================================
    private boolean isWithin(
            int h, int m,
            int sh, int sm,
            int eh, int em) {

        int now = h * 60 + m;
        int start = sh * 60 + sm;
        int end = eh * 60 + em;

        if (start == end) return true;
        if (start < end) return now >= start && now < end;
        return now >= start || now < end;
    }

    // =================================================
    // ✅ OVERLAY CONTROL
    // =================================================
    private void startOverlay() {
        startService(new Intent(this, OverlayProtectionService.class));
    }

    private void stopOverlay() {
        stopService(new Intent(this, OverlayProtectionService.class));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (handler != null && checker != null) {
            handler.removeCallbacks(checker);
        }

        stopOverlay();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // =================================================
    // ✅ NOTIFICATION CHANNEL
    // =================================================
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Focus Protection",
                            NotificationManager.IMPORTANCE_LOW
                    );

            NotificationManager nm =
                    getSystemService(NotificationManager.class);

            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}