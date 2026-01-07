package com.focuslock.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyAccessibilityService extends AccessibilityService {

    //  Supported browsers
    private static final List<String> BROWSERS = Arrays.asList(
            "com.android.chrome",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser"
    );

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null || event.getPackageName() == null) return;

        String pkg = event.getPackageName().toString();

        // Ignore own app & system UI & launcher
        if (pkg.equals(getPackageName())
                || pkg.equals("com.android.systemui")
                || pkg.contains("launcher")) {
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        //  Do not block while setup/edit is running
        if (prefs.getBoolean("SETUP_IN_PROGRESS", false)) return;

        // App sets
        Set<String> permanentApps =
                prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());

        Set<String> blockedApps =
                prefs.getStringSet("BLOCKED_APPS", new HashSet<>());

        //  Current time
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);

        // ==============================
        //  PERMANENT APPS TIME
        // ==============================
        int pSh = prefs.getInt("PERM_START_HOUR", -1);
        int pSm = prefs.getInt("PERM_START_MIN", -1);
        int pEh = prefs.getInt("PERM_END_HOUR", -1);
        int pEm = prefs.getInt("PERM_END_MIN", -1);

        // ==============================
        // ⚙️ SETTINGS APP BLOCK (PERMANENT MODE)
        // ==============================
        if (pkg.equals("com.android.settings")
                && pSh != -1
                && isWithinFocusTime(h, m, pSh, pSm, pEh, pEm)) {

            launchBlockScreen();
            return;
        }

        // ==============================
        //  PERMANENT APPS BLOCK
        // ==============================
        if (permanentApps.contains(pkg)
                && pSh != -1
                && isWithinFocusTime(h, m, pSh, pSm, pEh, pEm)) {

            launchBlockScreen();
            return;
        }

        // ==============================
        //️ TEMP / EDITABLE APPS
        // ==============================
        int tSh = prefs.getInt("TEMP_START_HOUR", -1);
        int tSm = prefs.getInt("TEMP_START_MIN", -1);
        int tEh = prefs.getInt("TEMP_END_HOUR", -1);
        int tEm = prefs.getInt("TEMP_END_MIN", -1);

        if (blockedApps.contains(pkg)
                && tSh != -1
                && isWithinFocusTime(h, m, tSh, tSm, tEh, tEm)) {

            launchBlockScreen();
            return;
        }

        // ==============================
        //  WEBSITE BLOCKING
        // ==============================
        if (BROWSERS.contains(pkg)
                && isWebsiteBlockingActive(prefs, h, m)) {

            Set<String> blockedWebsites =
                    prefs.getStringSet(
                            "PERMANENT_BLOCKED_WEBSITES",
                            new HashSet<>()
                    );

            if (!blockedWebsites.isEmpty()) {

                String hit = findBlockedWebsite(
                        getRootInActiveWindow(),
                        blockedWebsites
                );

                if (hit != null) {
                    launchBlockScreen();
                    return;
                }
            }
        }

        //  Clear TEMP app schedule if expired
        clearTempScheduleIfExpired(prefs, h, m, tSh, tSm, tEh, tEm);
    }

    @Override
    public void onInterrupt() {
        // No-op
    }

    // ==============================
    // ⏱ Time window logic
    // ==============================
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

    // ==============================
    //  Website timer active?
    // ==============================
    private boolean isWebsiteBlockingActive(
            SharedPreferences prefs,
            int h, int m) {

        if (!prefs.getBoolean("WEB_TIMER_SET", false))
            return false;

        int sh = prefs.getInt("WEB_START_HOUR", -1);
        int sm = prefs.getInt("WEB_START_MIN", -1);
        int eh = prefs.getInt("WEB_END_HOUR", -1);
        int em = prefs.getInt("WEB_END_MIN", -1);

        return sh != -1 && isWithinFocusTime(h, m, sh, sm, eh, em);
    }

    // ==============================
    //  Find blocked website
    // ==============================
    private String findBlockedWebsite(
            AccessibilityNodeInfo node,
            Set<String> blockedDomains) {

        if (node == null) return null;

        if (node.getText() != null) {
            String text = node.getText().toString().toLowerCase();

            for (String domain : blockedDomains) {
                if (text.contains(domain.toLowerCase())) {
                    return domain;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            String result =
                    findBlockedWebsite(node.getChild(i), blockedDomains);
            if (result != null) return result;
        }

        return null;
    }

    // ==============================
    //  Clear TEMP app schedule
    // ==============================
    private void clearTempScheduleIfExpired(
            SharedPreferences prefs,
            int h, int m,
            int tSh, int tSm,
            int tEh, int tEm) {

        if (tSh == -1) return;

        int current = h * 60 + m;
        int start = tSh * 60 + tSm;
        int end = tEh * 60 + tEm;

        boolean expired;

        if (start < end) {
            expired = current >= end;
        } else {
            expired = current >= end && current < start;
        }

        if (expired) {
            prefs.edit()
                    .remove("TEMP_START_HOUR")
                    .remove("TEMP_START_MIN")
                    .remove("TEMP_END_HOUR")
                    .remove("TEMP_END_MIN")
                    .apply();
        }
    }

    // ==============================
    //  Launch block screen
    // ==============================
    private void launchBlockScreen() {
        Intent i = new Intent(this, BlockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }
}
