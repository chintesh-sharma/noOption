package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        2001
                );
            }
        }

        super.onCreate(savedInstanceState);

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        boolean setupComplete =
                prefs.getBoolean("SETUP_COMPLETE", false);

        Intent intent;

        if (setupComplete) {
            // Setup complete → Dashboard
            intent = new Intent(this, DashboardActivity.class);
        } else {
            // Setup incomplete → start setup AGAIN
            intent = new Intent(this, PermissionSetupActivity.class);
        }

        startActivity(intent);
        finish();
    }
}

