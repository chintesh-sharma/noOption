package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView; // ✅ ADD

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        boolean welcomeShown =
                prefs.getBoolean("WELCOME_SHOWN", false);

        boolean setupComplete =
                prefs.getBoolean("SETUP_COMPLETE", false);

        // 🔒 AFTER SETUP → ALWAYS DASHBOARD (NO WELCOME)
        if (welcomeShown && setupComplete) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 🔁 AFTER WELCOME BUT SETUP NOT DONE
        if (welcomeShown) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 🆕 FIRST INSTALL ONLY
        setContentView(R.layout.activity_welcome);

        LinearLayout continueLayout = findViewById(R.id.layoutContinue);

        // ✅ ADD ONLY (PRIVACY POLICY CLICK)
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacy);
        tvPrivacyPolicy.setOnClickListener(v -> {
            startActivity(
                    new Intent(
                            WelcomeActivity.this,
                            PrivacyPolicyActivity.class
                    )
            );
        });

        continueLayout.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("WELCOME_SHOWN", true)
                    .apply();

            startActivity(new Intent(
                    WelcomeActivity.this,
                    MainActivity.class
            ));
            finish();
        });
    }
}