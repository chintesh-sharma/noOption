package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class PermanentWebsiteSelectionActivity extends AppCompatActivity {

    LinearLayout websiteContainer;
    Button btnAddWebsite, btnContinue, btnSkip;

    private static final String PREFS = "FOCUS_PREFS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permanent_website_selection);

        websiteContainer = findViewById(R.id.websiteContainer);
        btnAddWebsite = findViewById(R.id.btnAddWebsite);
        btnContinue = findViewById(R.id.btnContinueWebsite);
        btnSkip = findViewById(R.id.btnSkipWebsite);

        SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        // 🚫 Pause blocking during setup (UNCHANGED)
        prefs.edit()
                .putBoolean("SETUP_IN_PROGRESS", true)
                .apply();

        addWebsiteField();

        btnAddWebsite.setOnClickListener(v -> addWebsiteField());

        btnSkip.setOnClickListener(v -> {

            if (!prefs.contains("PERMANENT_BLOCKED_WEBSITES")) {
                saveWebsites(new HashSet<>());
            }

            resumeAndGoNext();
        });

        btnContinue.setOnClickListener(v -> {

            Set<String> websites = collectWebsites();

            if (websites.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter at least one website or skip",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            saveWebsites(websites);
            resumeAndGoNext();
        });

        // 🔒 Back press → app background (UNCHANGED)
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        moveTaskToBack(true);
                    }
                }
        );
    }

    // ================= ADD WEBSITE FIELD (UNCHANGED) =================
    private void addWebsiteField() {
        EditText et = new EditText(this);
        et.setHint("Enter website (e.g. youtube.com)");
        et.setTextColor(Color.WHITE);
        et.setSingleLine(true);
        et.setPadding(20, 20, 20, 20);
        websiteContainer.addView(et);
    }

    // ================= COLLECT WEBSITES =================
    private Set<String> collectWebsites() {
        Set<String> result = new HashSet<>();

        for (int i = 0; i < websiteContainer.getChildCount(); i++) {
            EditText et = (EditText) websiteContainer.getChildAt(i);
            String text = et.getText().toString().trim().toLowerCase();

            if (!TextUtils.isEmpty(text)) {
                String clean = cleanDomain(text);
                if (!TextUtils.isEmpty(clean)) {
                    result.add(clean);
                }
            }
        }
        return result;
    }

    // ================= STRONG DOMAIN CLEANER (FIX ONLY) =================
    private String cleanDomain(String input) {

        input = input.trim().toLowerCase();

        // remove protocol
        if (input.startsWith("https://")) {
            input = input.substring(8);
        } else if (input.startsWith("http://")) {
            input = input.substring(7);
        }

        // remove www / m
        if (input.startsWith("www.")) {
            input = input.substring(4);
        }
        if (input.startsWith("m.")) {
            input = input.substring(2);
        }

        // remove path/query
        int slashIndex = input.indexOf("/");
        if (slashIndex != -1) {
            input = input.substring(0, slashIndex);
        }

        return input;
    }

    // ================= SAVE WEBSITES (UNCHANGED) =================
    private void saveWebsites(Set<String> websites) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet("PERMANENT_BLOCKED_WEBSITES", websites)
                .apply();
    }

    // ================= NEXT STEP (UNCHANGED) =================
    private void resumeAndGoNext() {

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean("SETUP_IN_PROGRESS", false)
                .apply();

        startActivity(new Intent(
                this,
                WebsiteTimerActivity.class
        ));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        if (prefs.getBoolean("SETUP_COMPLETE", false)) {
            prefs.edit()
                    .putBoolean("SETUP_IN_PROGRESS", false)
                    .apply();
        }
    }
}
