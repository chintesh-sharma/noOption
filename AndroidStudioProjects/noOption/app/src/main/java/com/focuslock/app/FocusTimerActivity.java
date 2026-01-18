package com.focuslock.app;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class FocusTimerActivity extends AppCompatActivity {

    Button btnStartTime, btnEndTime, btnSave;

    int startHour = -1, startMin = -1;
    int endHour = -1, endMin = -1;

    boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_timer);

        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);

        getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE)
                .edit()
                .putBoolean("SETUP_IN_PROGRESS", true)
                .apply();

        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnSave = findViewById(R.id.btnSaveSchedule);

        btnStartTime.setOnClickListener(v -> pickStartTime());
        btnEndTime.setOnClickListener(v -> pickEndTime());
        btnSave.setOnClickListener(v -> saveSchedule());
    }

    private void pickStartTime() {
        new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    startHour = hourOfDay;
                    startMin = minute;
                    btnStartTime.setText("Start: " + formatTime(hourOfDay, minute));
                },
                9, 0, false
        ).show();
    }

    private void pickEndTime() {
        new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    endHour = hourOfDay;
                    endMin = minute;
                    btnEndTime.setText("End: " + formatTime(hourOfDay, minute));
                },
                18, 0, false
        ).show();
    }

    private void saveSchedule() {

        if (startHour == -1 || endHour == -1) {
            Toast.makeText(this, "Please select both times", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();

        if (isEditMode) {
            ed.putInt("TEMP_START_HOUR", startHour);
            ed.putInt("TEMP_START_MIN", startMin);
            ed.putInt("TEMP_END_HOUR", endHour);
            ed.putInt("TEMP_END_MIN", endMin);
        } else {
            ed.putInt("PERM_START_HOUR", startHour);
            ed.putInt("PERM_START_MIN", startMin);
            ed.putInt("PERM_END_HOUR", endHour);
            ed.putInt("PERM_END_MIN", endMin);
            ed.putBoolean("PERMANENT_MODE_ACTIVE", true);
        }

        ed.putBoolean("FOCUS_ACTIVE", true);

        if (!isEditMode) {
            ed.putBoolean("SETUP_COMPLETE", true);
        }

        ed.putBoolean("SETUP_IN_PROGRESS", false);
        ed.apply();

        Toast.makeText(this, "Focus Schedule Saved", Toast.LENGTH_SHORT).show();

        GeminiTextHelper.generateText(
                "Focus mode just started. Send a short funny motivational line.",
                new GeminiTextHelper.GeminiCallback() {

                    @Override
                    public void onResult(String text) {
                        if (text != null && !text.trim().isEmpty()) {
                            prefs.edit()
                                    .putString("FOCUS_START_MESSAGE", text.trim())
                                    .apply();
                        }
                    }

                    @Override
                    public void onError(String error) {}
                }
        );

        if (!isEditMode) {
            startActivity(new Intent(this, PermanentWebsiteSelectionActivity.class));
        } else {
            startActivity(new Intent(this, DashboardActivity.class));
        }

        finish();
    }

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
