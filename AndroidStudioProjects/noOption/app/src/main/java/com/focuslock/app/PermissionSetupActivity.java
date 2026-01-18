package com.focuslock.app;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionSetupActivity extends AppCompatActivity {

    Button btnAccessibility, btnBattery, btnAppSettings,
            btnUsageAccess, btnAutostart, btnContinue;

    // 🔹 NEW (for How to use)
    TextView tvHowTo;

    private static final int REQ_DEVICE_ADMIN = 1001;

    SharedPreferences prefs;

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
        btnContinue = findViewById(R.id.btnContinue);

        // 🔹 NEW: How to use TextView
        tvHowTo = findViewById(R.id.tvHowTo);

        // ---------------- EXISTING ----------------

        btnAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

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

        btnAppSettings.setOnClickListener(v -> {

            prefs.edit().putBoolean("APP_SETTINGS_STEP_DONE", true).apply();

            openAppSettingsSafely();
        });

        btnUsageAccess.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        );

        btnAutostart.setOnClickListener(v -> {

            prefs.edit().putBoolean("AUTOSTART_STEP_DONE", true).apply();

            openAutostartSettings();
        });

        // ---------------- CONTINUE (UNCHANGED) ----------------

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

        // =================================================
        // 🔹 NEW: HOW TO USE LINK (ONLY UI HELP)
        // =================================================
        tvHowTo.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=YOUR_VIDEO_ID")
            );
            startActivity(intent);
        });
    }

    // ================= UI STATUS UPDATE =================

    @Override
    protected void onResume() {
        super.onResume();

        if (PermissionUtils.isAccessibilityEnabled(this)) {
            btnAccessibility.setText("Accessibility Enabled ✓");
            btnAccessibility.setEnabled(false);
            btnAccessibility.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        if (isUsageAccessGranted()) {
            btnUsageAccess.setText("Usage Access Enabled ✓");
            btnUsageAccess.setEnabled(false);
            btnUsageAccess.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        if (prefs.getBoolean("BATTERY_STEP_DONE", false)) {
            btnBattery.setText("Battery Optimization Checked ✓");
            btnBattery.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        if (prefs.getBoolean("APP_SETTINGS_STEP_DONE", false)) {
            btnAppSettings.setText("Background Activity Checked ✓");
            btnAppSettings.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }

        if (prefs.getBoolean("AUTOSTART_STEP_DONE", false)) {
            btnAutostart.setText("Autostart Checked ✓");
            btnAutostart.setBackgroundTintList(
                    getColorStateList(android.R.color.holo_blue_dark)
            );
        }
    }

    // ================= DEVICE ADMIN (UNCHANGED) =================

    private void requestDeviceAdmin() {

        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

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

            if (dpm.isAdminActive(adminComponent)) {
                goNext();
            } else {
                Toast.makeText(
                        this,
                        "Device Admin zaroori hai uninstall protection ke liye",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void goNext() {
        startActivity(new Intent(this, AppSelectionActivity.class));
        finish();
    }

    // ================= CHECKS (UNCHANGED) =================

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

    private void openAppSettingsSafely() {
        try {
            Intent intent = new Intent();
            intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
            );
            intent.putExtra("extra_pkgname", getPackageName());
            startActivity(intent);

        } catch (Exception e1) {
            try {
                Intent intent =
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(
                        this,
                        "Open settings manually",
                        Toast.LENGTH_LONG
                ).show();
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }
    }

    // ================= AUTOSTART (UNCHANGED) =================

    private void openAutostartSettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
            );
            startActivity(intent);

        } catch (Exception e1) {
            try {
                Intent intent =
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);

            } catch (Exception e2) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }
    }
}
