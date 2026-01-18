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

    private static final List<String> BROWSERS = Arrays.asList(
            "com.android.chrome",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser"
    );

    public static boolean isBlockingScreenShown = false;
    private static long lastBlockTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (event == null || event.getPackageName() == null) return;

        // 🔥 LISTEN MORE EVENTS (ADD ONLY)
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            return;
        }

        String pkg = event.getPackageName().toString();

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

        // ================= SETTINGS BLOCK =================
        boolean permForSettings = false;
        int pShS = prefs.getInt("PERM_START_HOUR", -1);
        if (pShS != -1) {
            permForSettings = isWithin(
                    h, m,
                    pShS,
                    prefs.getInt("PERM_START_MIN", 0),
                    prefs.getInt("PERM_END_HOUR", 0),
                    prefs.getInt("PERM_END_MIN", 0)
            );
        }

        boolean webForSettings = false;
        int wShS = prefs.getInt("WEB_START_HOUR", -1);
        if (wShS != -1) {
            webForSettings = isWithin(
                    h, m,
                    wShS,
                    prefs.getInt("WEB_START_MIN", 0),
                    prefs.getInt("WEB_END_HOUR", 0),
                    prefs.getInt("WEB_END_MIN", 0)
            );
        }

        if ((permForSettings || webForSettings)
                && pkg.equals("com.android.settings")) {
            showBlockSafely(pkg, "PERM");
            return;
        }

        // ================= PERMANENT APPS =================
        boolean permActive = false;
        int pSh = prefs.getInt("PERM_START_HOUR", -1);
        if (pSh != -1) {
            permActive = isWithin(
                    h, m,
                    pSh,
                    prefs.getInt("PERM_START_MIN", 0),
                    prefs.getInt("PERM_END_HOUR", 0),
                    prefs.getInt("PERM_END_MIN", 0)
            );
        }

        Set<String> permanentApps =
                new HashSet<>(prefs.getStringSet(
                        "PERMANENT_BLOCKED_APPS", new HashSet<>()));

        // 🔥 FAST BLOCK (NO UI WAIT)
        if (permActive && permanentApps.contains(pkg)) {
            showBlockSafely(pkg, "PERM");
            return;
        }

        // ================= PERMANENT WEBSITES =================
        if (BROWSERS.contains(pkg)) {

            boolean webActive = false;
            int wSh = prefs.getInt("WEB_START_HOUR", -1);

            if (wSh != -1) {
                webActive = isWithin(
                        h, m,
                        wSh,
                        prefs.getInt("WEB_START_MIN", 0),
                        prefs.getInt("WEB_END_HOUR", 0),
                        prefs.getInt("WEB_END_MIN", 0)
                );
            }

            Set<String> sites =
                    new HashSet<>(prefs.getStringSet(
                            "PERMANENT_BLOCKED_WEBSITES", new HashSet<>()));

            if (webActive && !sites.isEmpty()) {

                String currentUrl = getCurrentUrl(event, pkg);

                if (currentUrl != null) {
                    for (String site : sites) {
                        if (currentUrl.contains(site)) {
                            showBlockSafely(pkg, "WEB");
                            return;
                        }
                    }
                }
            }
        }

        // ================= TEMP APPS =================
        boolean tempActive = false;
        int tSh = prefs.getInt("TEMP_START_HOUR", -1);

        if (tSh != -1) {
            tempActive = isWithin(
                    h, m,
                    tSh,
                    prefs.getInt("TEMP_START_MIN", 0),
                    prefs.getInt("TEMP_END_HOUR", 0),
                    prefs.getInt("TEMP_END_MIN", 0)
            );
        }

        if (tempActive) {

            Set<String> tempApps =
                    new HashSet<>(prefs.getStringSet(
                            "BLOCKED_APPS", new HashSet<>()));

            if (tempApps.contains(pkg)
                    && !permanentApps.contains(pkg)) {

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

    private void showBlockSafely(String pkg, String type) {

        long now = System.currentTimeMillis();

        // 🔥 FASTER THROTTLE (800ms → 300ms)
        if (isBlockingScreenShown && now - lastBlockTime < 300) return;

        isBlockingScreenShown = true;
        lastBlockTime = now;

        Intent i = new Intent(this, BlockActivity.class);
        i.putExtra("BLOCKED_PKG", pkg);
        i.putExtra("BLOCK_TYPE", type);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(i);
    }

    // ================= URL DETECTOR =================
    private String getCurrentUrl(AccessibilityEvent event, String pkg) {

        if (event.getSource() == null) return null;

        String[] possibleIds = null;

        if (pkg.equals("com.android.chrome")
                || pkg.equals("com.brave.browser")) {

            possibleIds = new String[]{
                    pkg + ":id/url_bar"
            };

        } else if (pkg.equals("com.microsoft.emmx")) {

            possibleIds = new String[]{
                    "com.microsoft.emmx:id/url_bar",
                    "com.microsoft.emmx:id/location_bar",
                    "com.microsoft.emmx:id/omnibox_text_box",
                    "com.microsoft.emmx:id/search_box"
            };

        } else if (pkg.equals("com.sec.android.app.sbrowser")) {

            possibleIds = new String[]{
                    "com.sec.android.app.sbrowser:id/location_bar_edit_text"
            };
        }

        if (possibleIds == null) return null;

        for (String id : possibleIds) {
            try {
                List<AccessibilityNodeInfo> nodes =
                        event.getSource().findAccessibilityNodeInfosByViewId(id);

                if (nodes != null && !nodes.isEmpty()) {
                    CharSequence text = nodes.get(0).getText();
                    if (text != null) {
                        return text.toString().toLowerCase();
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }
}
