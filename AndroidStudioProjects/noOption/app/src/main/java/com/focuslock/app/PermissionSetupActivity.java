package com.focuslock.app;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionSetupActivity extends AppCompatActivity {

    Button btnAccessibility, btnBattery, btnAppSettings,
            btnUsageAccess, btnAutostart, btnContinue;

    private static final int REQ_DEVICE_ADMIN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_setup);

        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnBattery = findViewById(R.id.btnBattery);
        btnAppSettings = findViewById(R.id.btnAppSettings);
        btnUsageAccess = findViewById(R.id.btnUsageAccess);
        btnAutostart = findViewById(R.id.btnAutostart); // ✅ NEW
        btnContinue = findViewById(R.id.btnContinue);

        // ---------------- EXISTING ----------------

        btnAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        btnBattery.setOnClickListener(v -> {
            try {
                startActivity(
                        new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                );
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        btnAppSettings.setOnClickListener(v -> openAppSettingsSafely());

        btnUsageAccess.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        );

        // ---------------- NEW : AUTOSTART ----------------

        btnAutostart.setOnClickListener(v -> openAutostartSettings());

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

    // ================= NEW : AUTOSTART =================

    private void openAutostartSettings() {
        try {
            // MIUI Autostart
            Intent intent = new Intent();
            intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
            );
            startActivity(intent);

        } catch (Exception e1) {
            try {
                // App info fallback
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
