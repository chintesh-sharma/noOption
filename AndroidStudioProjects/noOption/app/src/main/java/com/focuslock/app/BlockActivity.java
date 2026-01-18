package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Random;

public class BlockActivity extends AppCompatActivity {

    private TextView tvBlockMessage, tvTimeLeft;
    private CountDownTimer timer;
    SharedPreferences prefs;
    private String blockType;
    private String blockedPkg;

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

        // 🔐 Prevent screenshots
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        setContentView(R.layout.activity_block);

        prefs = getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        tvBlockMessage = findViewById(R.id.tvBlockMessage);
        tvTimeLeft = findViewById(R.id.tvTimeLeft);

        tvBlockMessage.setText(
                MESSAGES[new Random().nextInt(MESSAGES.length)]
        );

        blockType = getIntent().getStringExtra("BLOCK_TYPE");
        blockedPkg = getIntent().getStringExtra("BLOCKED_PKG");

        startCorrectTimer(blockType);

        // 🔒 Back → Home
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

    // ================= TIMER =================
    private void startCorrectTimer(String type) {

        long remainingMs;

        if ("WEB".equals(type)) {
            remainingMs = getRemainingMillis(
                    "WEB_START_HOUR", "WEB_START_MIN",
                    "WEB_END_HOUR", "WEB_END_MIN"
            );
        } else if ("TEMP".equals(type)) {
            remainingMs = getRemainingMillis(
                    "TEMP_START_HOUR", "TEMP_START_MIN",
                    "TEMP_END_HOUR", "TEMP_END_MIN"
            );
        } else {
            remainingMs = getRemainingMillis(
                    "PERM_START_HOUR", "PERM_START_MIN",
                    "PERM_END_HOUR", "PERM_END_MIN"
            );
        }

        // 🚫 SETTINGS → NEVER SHOW FINISH
        if ("com.android.settings".equals(blockedPkg)) {
            if (remainingMs <= 0) {
                tvTimeLeft.setText("");
            }
            return;
        }

        if (remainingMs <= 0) {
            showFinishText(type);
            return;
        }

        timer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long ms) {
                tvTimeLeft.setText(
                        "Time left: " + format(ms)
                );
            }

            @Override
            public void onFinish() {
                showFinishText(type);
            }
        };
        timer.start();
    }

    // ================= FINISH TEXT =================
    private void showFinishText(String type) {
        if ("PERM".equals(type)) {
            tvTimeLeft.setText("Permanent focus finished ✔️");
        } else if ("WEB".equals(type)) {
            tvTimeLeft.setText("Website focus finished ✔️");
        } else if ("TEMP".equals(type)) {
            tvTimeLeft.setText("Temporary focus finished ✔️");
        } else {
            tvTimeLeft.setText("");
        }
    }

    private long getRemainingMillis(
            String shK, String smK,
            String ehK, String emK) {

        int sh = prefs.getInt(shK, -1);
        int sm = prefs.getInt(smK, -1);
        int eh = prefs.getInt(ehK, -1);
        int em = prefs.getInt(emK, -1);

        if (sh == -1) return 0;

        Calendar now = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        end.set(Calendar.HOUR_OF_DAY, eh);
        end.set(Calendar.MINUTE, em);
        end.set(Calendar.SECOND, 0);

        if (end.before(now)) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }

        return end.getTimeInMillis() - now.getTimeInMillis();
    }

    private String format(long ms) {
        long sec = ms / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void goHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        MyAccessibilityService.isBlockingScreenShown = false;
    }
}
