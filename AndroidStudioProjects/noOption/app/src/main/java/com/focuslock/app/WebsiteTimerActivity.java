package com.focuslock.app;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class WebsiteTimerActivity extends AppCompatActivity {

    Button btnStartTime, btnEndTime, btnSave, btnSkip;

    int startHour = -1, startMin = -1;
    int endHour = -1, endMin = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_website_timer);

        btnStartTime = findViewById(R.id.btnWebStartTime);
        btnEndTime = findViewById(R.id.btnWebEndTime);
        btnSave = findViewById(R.id.btnSaveWebTimer);
        btnSkip = findViewById(R.id.btnSkipWebTimer);

        // 🚫 Pause blocking during setup (UNCHANGED IDEA)
        getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE)
                .edit()
                .putBoolean("SETUP_IN_PROGRESS", true)
                .apply();

        btnStartTime.setOnClickListener(v -> pickStartTime());
        btnEndTime.setOnClickListener(v -> pickEndTime());

        // ⏭️ SKIP → setup complete screen → dashboard
        btnSkip.setOnClickListener(v -> {
            SharedPreferences prefs =
                    getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

            prefs.edit()
                    .putBoolean("WEB_TIMER_SET", false)
                    .putBoolean("SETUP_COMPLETE", true)
                    .putBoolean("SETUP_IN_PROGRESS", false)
                    .apply();

            goToSetupComplete();
        });

        // 💾 SAVE → setup complete screen → dashboard
        btnSave.setOnClickListener(v -> saveTimer());

        // 🔒 BACK PROTECTION (same as permanent website screen)
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // App background me jayegi
                        moveTaskToBack(true);
                    }
                }
        );
    }

    // ⏱ Start time picker
    private void pickStartTime() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    startHour = hourOfDay;
                    startMin = minute;
                    btnStartTime.setText(
                            "Start: " + formatTime(hourOfDay, minute)
                    );
                },
                9, 0,
                false
        );
        dialog.show();
    }

    // ⏱ End time picker
    private void pickEndTime() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    endHour = hourOfDay;
                    endMin = minute;
                    btnEndTime.setText(
                            "End: " + formatTime(hourOfDay, minute)
                    );
                },
                18, 0,
                false
        );
        dialog.show();
    }

    // 💾 SAVE WEBSITE TIMER
    private void saveTimer() {

        if (startHour == -1 || endHour == -1) {
            Toast.makeText(
                    this,
                    "Please select both start and end time",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        prefs.edit()
                .putInt("WEB_START_HOUR", startHour)
                .putInt("WEB_START_MIN", startMin)
                .putInt("WEB_END_HOUR", endHour)
                .putInt("WEB_END_MIN", endMin)
                .putBoolean("WEB_TIMER_SET", true)
                .putBoolean("SETUP_COMPLETE", true)
                .putBoolean("SETUP_IN_PROGRESS", false)
                .apply();

        Toast.makeText(
                this,
                "Website blocking time saved",
                Toast.LENGTH_SHORT
        ).show();

        goToSetupComplete();
    }

    // ✅ SETUP COMPLETE SCREEN (SAVE + SKIP BOTH)
    private void goToSetupComplete() {
        Intent i = new Intent(this, SetupCompleteActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    // 🕒 AM / PM formatter
    private String formatTime(int hour, int min) {
        String amPm = (hour >= 12) ? "PM" : "AM";
        int h = hour % 12;
        if (h == 0) h = 12;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d %s",
                h, min, amPm
        );
    }
}
