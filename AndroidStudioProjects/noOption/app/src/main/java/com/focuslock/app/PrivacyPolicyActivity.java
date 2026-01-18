package com.focuslock.app;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        btnContinue = findViewById(R.id.btnPrivacyContinue);

        btnContinue.setOnClickListener(v -> {
            finish(); // 🔥 wapas WelcomeActivity
        });
    }
}
