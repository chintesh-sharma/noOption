package com.focuslock.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class BlockActivity extends AppCompatActivity {

    TextView tvBlockMessage;

    //  Offline fun fallback messages
    private static final String[] FUN_MESSAGES = {
            "Focus kar bhai 😤 You got this!",
            "Bas thoda sa aur 💪",
            "Distraction ruk, success aane de 🚀",
            "Phone band, future ON 🔥",
            "Aaj mehnat, kal result 😎"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent screenshots / recent preview
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        setContentView(R.layout.activity_block);

        tvBlockMessage = findViewById(R.id.tvBlockMessage);

        //  Disable BACK button
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // BACK disabled
                    }
                });

        showBlockMessage();
    }

    private void showBlockMessage() {

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        //  Try Gemini runtime message (pre-generated)
        String message =
                prefs.getString("RUNTIME_GEMINI_MESSAGE", null);

        // If Gemini not available → random fallback
        if (message == null || message.trim().isEmpty()) {
            message = FUN_MESSAGES[new Random()
                    .nextInt(FUN_MESSAGES.length)];
        }

        //  Show instantly (NO DELAY, NO FAIL)
        tvBlockMessage.setText(message);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // intentionally empty
    }

    @Override
    protected void onStop() {
        super.onStop();
        // keep alive
    }
}

