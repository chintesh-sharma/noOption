package com.focuslock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyAccessibilityService extends AccessibilityService {

    private static final List<String> BROWSERS = Arrays.asList(
            "com.android.chrome",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser"
    );

    // 🔒 LOOP / FLICKER GUARD
    public static boolean isBlockingScreenShown = false;
    private static long lastBlockTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null || event.getPackageName() == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return;

        String pkg = event.getPackageName().toString();

        // Ignore system / launcher / own app
        if (pkg.equals(getPackageName())
                || pkg.equals("com.android.systemui")
                || pkg.contains("launcher")) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        if (prefs.getBoolean("SETUP_IN_PROGRESS", false)) return;

        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);

        // ================= PERMANENT TIME =================
        int pSh = prefs.getInt("PERM_START_HOUR", -1);
        int pSm = prefs.getInt("PERM_START_MIN", -1);
        int pEh = prefs.getInt("PERM_END_HOUR", -1);
        int pEm = prefs.getInt("PERM_END_MIN", -1);

        boolean permActive =
                pSh != -1 && isWithin(h, m, pSh, pSm, pEh, pEm);

        if (!permActive) {
            isBlockingScreenShown = false;
            return;
        }

        Set<String> permanentApps =
                prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());

        // 🔒 SETTINGS BLOCK
        if (pkg.equals("com.android.settings")) {
            showBlockSafely(pkg, "PERM");
            return;
        }

        // 🔒 PERMANENT APPS
        if (permanentApps.contains(pkg)) {
            showBlockSafely(pkg, "PERM");
            return;
        }

        // 🔒 WEBSITE BLOCK
        if (BROWSERS.contains(pkg)) {

            Set<String> sites =
                    prefs.getStringSet("PERMANENT_BLOCKED_WEBSITES",
                            new HashSet<>());

            boolean webActive = permActive;

            if (prefs.getBoolean("WEB_TIMER_SET", false)) {
                int wSh = prefs.getInt("WEB_START_HOUR", -1);
                int wSm = prefs.getInt("WEB_START_MIN", -1);
                int wEh = prefs.getInt("WEB_END_HOUR", -1);
                int wEm = prefs.getInt("WEB_END_MIN", -1);

                webActive = wSh != -1 && isWithin(h, m, wSh, wSm, wEh, wEm);
            }

            if (webActive && !sites.isEmpty()) {
                showBlockSafely(pkg, "WEB");
            }
        }

        // 🔒 TEMP / EDITABLE APPS (unchanged)
        int tSh = prefs.getInt("TEMP_START_HOUR", -1);
        int tSm = prefs.getInt("TEMP_START_MIN", -1);
        int tEh = prefs.getInt("TEMP_END_HOUR", -1);
        int tEm = prefs.getInt("TEMP_END_MIN", -1);

        boolean tempActive =
                tSh != -1 && isWithin(h, m, tSh, tSm, tEh, tEm);

        if (tempActive) {
            Set<String> tempApps =
                    prefs.getStringSet("BLOCKED_APPS", new HashSet<>());

            if (tempApps.contains(pkg)) {
                showBlockSafely(pkg, "TEMP");
            }
        }
    }

    @Override
    public void onInterrupt() {}

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

    // ================= SAFE BLOCK =================
    private void showBlockSafely(String pkg, String type) {

        long now = System.currentTimeMillis();
        if (isBlockingScreenShown && now - lastBlockTime < 800) return;

        isBlockingScreenShown = true;
        lastBlockTime = now;

        Intent i = new Intent(this, BlockActivity.class);
        i.putExtra("BLOCKED_PKG", pkg);
        i.putExtra("BLOCK_TYPE", type); // 🔥 ONLY NEW THING
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(i);
    }
}
