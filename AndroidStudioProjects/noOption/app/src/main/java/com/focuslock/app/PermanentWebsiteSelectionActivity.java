package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class PermanentWebsiteSelectionActivity extends AppCompatActivity {

    LinearLayout websiteContainer;
    Button btnAddWebsite, btnContinue, btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permanent_website_selection);

        websiteContainer = findViewById(R.id.websiteContainer);
        btnAddWebsite = findViewById(R.id.btnAddWebsite);
        btnContinue = findViewById(R.id.btnContinueWebsite);
        btnSkip = findViewById(R.id.btnSkipWebsite);

        // Start with one input
        addWebsiteField();

        btnAddWebsite.setOnClickListener(v -> addWebsiteField());

        btnSkip.setOnClickListener(v -> {
            // Skip allowed – save empty & go next
            saveWebsites(new HashSet<>());
            goToWebsiteTimer();
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
            goToWebsiteTimer();
        });
    }

    //  Add new EditText dynamically
    private void addWebsiteField() {
        EditText et = new EditText(this);
        et.setHint("Enter website (e.g. instagram.com)");
        et.setSingleLine(true);
        et.setPadding(20, 20, 20, 20);

        websiteContainer.addView(et);
    }

    //  Collect & sanitize websites
    private Set<String> collectWebsites() {
        Set<String> result = new HashSet<>();

        for (int i = 0; i < websiteContainer.getChildCount(); i++) {
            EditText et = (EditText) websiteContainer.getChildAt(i);
            String text = et.getText().toString().trim().toLowerCase();

            if (!TextUtils.isEmpty(text)) {
                result.add(cleanDomain(text));
            }
        }
        return result;
    }
    //  Remove https, http, www
    private String cleanDomain(String input) {
        input = input.replace("https://", "");
        input = input.replace("http://", "");
        input = input.replace("www.", "");
        return input;
    }

    // Save to SharedPreferences
    private void saveWebsites(Set<String> websites) {
        SharedPreferences prefs =
                getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        prefs.edit()
                .putStringSet("PERMANENT_BLOCKED_WEBSITES", websites)
                .apply();
    }

    //  Next screen
    private void goToWebsiteTimer() {
        startActivity(new Intent(
                this,
                WebsiteTimerActivity.class   // STEP-2
        ));
        finish();
    }
}
