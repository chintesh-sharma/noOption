package com.focuslock.app;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionSetupActivity extends AppCompatActivity {

    Button btnAccessibility, btnBattery, btnAppSettings,
            btnUsageAccess, btnAutostart, btnNotification, btnContinue;

    TextView tvHowTo;

    private static final int REQ_DEVICE_ADMIN = 1001;
    private static final int REQ_NOTIFICATION = 2001;

    SharedPreferences prefs;

    // 🔐 ONLY FOR BACKGROUND + OVERLAY
    private int bgOverlayClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        prefs = getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnBattery = findViewById(R.id.btnBattery);
        btnAppSettings = findViewById(R.id.btnAppSettings);
        btnUsageAccess = findViewById(R.id.btnUsageAccess);
        btnAutostart = findViewById(R.id.btnAutostart);
        btnNotification = findViewById(R.id.btnNotification);
        btnContinue = findViewById(R.id.btnContinue);
        tvHowTo = findViewById(R.id.tvHowTo);

        // ================= ACCESSIBILITY =================
        btnAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        // ================= BATTERY =================
        btnBattery.setOnClickListener(v -> {
            prefs.edit().putBoolean("BATTERY_STEP_DONE", true).apply();
            try {
                startActivity(
                        new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                );
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        // ================= BACKGROUND + OVERLAY =================
        btnAppSettings.setOnClickListener(v -> {

            bgOverlayClickCount++;

            // 🔁 BOTH TIMES → OPEN SAME SETTINGS
            openBackgroundAndOverlaySettings();
        });

        // ================= USAGE ACCESS =================
        btnUsageAccess.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        );

        // ================= AUTOSTART =================
        btnAutostart.setOnClickListener(v -> {
            prefs.edit().putBoolean("AUTOSTART_STEP_DONE", true).apply();
            openAutostartSettings();
        });

        // ================= NOTIFICATION =================
        btnNotification.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQ_NOTIFICATION
                    );
                    return;
                }
            }

            Intent intent =
                    new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });

        // ================= CONTINUE =================
        btnContinue.setOnClickListener(v -> {

            if (!PermissionUtils.isAccessibilityEnabled(this)) {
                Toast.makeText(this, "Enable Accessibility first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isUsageAccessGranted()) {
                Toast.makeText(this, "Enable Usage Access first", Toast.LENGTH_SHORT).show();
                return;
            }

            requestDeviceAdmin();
        });

        // ================= HOW TO USE =================
        tvHowTo.setOnClickListener(v ->
                startActivity(new Intent(this, HowToUseActivity.class))
        );
    }

    // ================= RESUME (ONLY HERE UI CHANGE) =================
    @Override
    protected void onResume() {
        super.onResume();

        // 🔹 Accessibility
        if (PermissionUtils.isAccessibilityEnabled(this)) {
            btnAccessibility.setText("Accessibility Enabled ✓");
            btnAccessibility.setEnabled(false);
            btnAccessibility.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        // 🔹 Usage Access
        if (isUsageAccessGranted()) {
            btnUsageAccess.setText("Usage Access Enabled ✓");
            btnUsageAccess.setEnabled(false);
            btnUsageAccess.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        // 🔹 Battery
        if (prefs.getBoolean("BATTERY_STEP_DONE", false)) {
            btnBattery.setText("Battery Optimization Checked ✓");
            btnBattery.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        // 🔹 Autostart
        if (prefs.getBoolean("AUTOSTART_STEP_DONE", false)) {
            btnAutostart.setText("Autostart Checked ✓");
            btnAutostart.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        // 🔥 BACKGROUND + OVERLAY (ONLY AFTER 2 CLICKS + RETURN)
        if (bgOverlayClickCount >= 2 && Settings.canDrawOverlays(this)) {
            btnAppSettings.setText("Background & Overlay Enabled ✓");
            btnAppSettings.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        // 🔹 Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {

                btnNotification.setText("Notifications Enabled ✓");
                btnNotification.setEnabled(false);
                btnNotification.setBackgroundTintList(
                        getColorStateList(android.R.color.holo_blue_dark)
                );
            }
        }
    }

    // ================= HELPERS =================

    private void openBackgroundAndOverlaySettings() {

        // 1️⃣ Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(overlayIntent);
            return;
        }

        // 2️⃣ OEM popup / background window permission
        try {
            Intent intent = new Intent();
            intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
            );
            intent.putExtra("extra_pkgname", getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            Intent fallback =
                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.parse("package:" + getPackageName()));
            startActivity(fallback);
        }
    }

    private boolean isUsageAccessGranted() {
        AppOpsManager appOps =
                (AppOpsManager) getSystemService(APP_OPS_SERVICE);

        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName()
        );

        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void openAutostartSettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
            );
            startActivity(intent);
        } catch (Exception e) {
            Intent intent =
                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void requestDeviceAdmin() {

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

        if (dpm == null) return;

        ComponentName adminComponent =
                new ComponentName(this, MyDeviceAdminReceiver.class);

        if (dpm.isAdminActive(adminComponent)) {
            goNext();
            return;
        }

        Intent intent =
                new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);

        intent.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                adminComponent
        );

        intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Direct uninstall rokne ke liye ye permission zaroori hai"
        );

        startActivityForResult(intent, REQ_DEVICE_ADMIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_DEVICE_ADMIN) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

            ComponentName adminComponent =
                    new ComponentName(this, MyDeviceAdminReceiver.class);

            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                goNext();
            }
        }
    }

    private void goNext() {
        startActivity(new Intent(this, AppSelectionActivity.class));
        finish();
    }
}
