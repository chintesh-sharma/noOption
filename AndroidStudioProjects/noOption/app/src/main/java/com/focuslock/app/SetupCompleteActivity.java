package com.focuslock.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class SetupCompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_complete);

        // 🔒 Disable back completely
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // do nothing
                    }
                }
        );

        // ⏳ 2 seconds → Dashboard
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(this, DashboardActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }, 2000);
    }
}
