package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PermanentSelectionActivity extends AppCompatActivity {

    ListView listPermanent;
    Button btnConfirm;

    ArrayList<String> appNames = new ArrayList<>();
    ArrayList<String> packageNames = new ArrayList<>();

    private static final String PREFS = "FOCUS_PREFS";
    private static final String PERMANENT_MSG_KEY = "PERMANENT_BLOCK_MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permanent_selection);

        listPermanent = findViewById(R.id.listPermanent);
        btnConfirm = findViewById(R.id.btnConfirmPermanent);

        boolean readOnly =
                getIntent().getBooleanExtra("READ_ONLY", false);

        SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        //  DATA SOURCE DECISION (UNCHANGED)
        Set<String> sourcePackages;

        if (readOnly) {
            sourcePackages =
                    prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());
            btnConfirm.setVisibility(View.GONE);
        } else {
            sourcePackages =
                    prefs.getStringSet("TEMP_SELECTED_APPS", new HashSet<>());
        }

        PackageManager pm = getPackageManager();

        for (String pkg : sourcePackages) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                appNames.add(pm.getApplicationLabel(info).toString());
                packageNames.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                readOnly
                        ? android.R.layout.simple_list_item_1
                        : android.R.layout.simple_list_item_multiple_choice,
                appNames
        );

        listPermanent.setAdapter(adapter);

        if (!readOnly) {
            listPermanent.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            btnConfirm.setOnClickListener(v -> {

                Set<String> permanentApps = new HashSet<>();

                for (int i = 0; i < listPermanent.getCount(); i++) {
                    if (listPermanent.isItemChecked(i)) {
                        permanentApps.add(packageNames.get(i));
                    }
                }

                //  FINAL BLOCKED = TEMP + PERMANENT (UNCHANGED)
                Set<String> finalBlocked = new HashSet<>(sourcePackages);

                prefs.edit()
                        .putStringSet("PERMANENT_BLOCKED_APPS", permanentApps)
                        .putStringSet("BLOCKED_APPS", finalBlocked)
                        .remove("TEMP_SELECTED_APPS")
                        .apply();

                // ==========================
                //  GEMINI (PERMANENT FALLBACK ONLY)
                // ==========================
                GeminiTextHelper.generateText(
                        "Permanent app blocking activated. Send a short funny motivational line.",
                        new GeminiTextHelper.GeminiCallback() {

                            @Override
                            public void onResult(String text) {
                                if (text != null && !text.trim().isEmpty()) {
                                    //  Save ONLY ONCE
                                    if (!prefs.contains(PERMANENT_MSG_KEY)) {
                                        prefs.edit()
                                                .putString(PERMANENT_MSG_KEY, text.trim())
                                                .apply();
                                    }
                                }
                            }

                            @Override
                            public void onError(String error) {
                                //  Default fallback ONLY if not already saved
                                if (!prefs.contains(PERMANENT_MSG_KEY)) {
                                    prefs.edit()
                                            .putString(
                                                    PERMANENT_MSG_KEY,
                                                    "Permanent apps blocked. Stay focused 🔒"
                                            )
                                            .apply();
                                }
                            }
                        }
                );

                //  NEXT STEP = TIME SET (UNCHANGED)
                Intent intent =
                        new Intent(
                                PermanentSelectionActivity.this,
                                FocusTimerActivity.class
                        );
                startActivity(intent);
                finish();
            });
        }
    }
}
