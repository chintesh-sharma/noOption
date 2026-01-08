package com.focuslock.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class BlockActivity extends AppCompatActivity {

    private TextView tvBlockMessage;

    private static final String[] MESSAGES = {
            "Focus mode ON 🔒",
            "Distraction not allowed 🚫",
            "Stay focused 💪",
            "Back to work 😤",
            "Phone later, success first 🚀"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔐 Prevent screenshots only (NO LOCK)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        setContentView(R.layout.activity_block);

        tvBlockMessage = findViewById(R.id.tvBlockMessage);

        tvBlockMessage.setText(
                MESSAGES[new Random().nextInt(MESSAGES.length)]
        );

        // ✅ CORRECT BACK HANDLING (NO ERROR)
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        goHome();
                    }
                }
        );
    }

    private void goHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

