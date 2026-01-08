package com.focuslock.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppSelectionActivity extends AppCompatActivity {

    ListView listApps;
    Button btnSave;
    EditText etSearch;

    ArrayList<AppItem> allApps = new ArrayList<>();
    ArrayList<AppItem> visibleApps = new ArrayList<>();

    // Editable only
    Set<String> checkedPackages = new HashSet<>();

    ArrayAdapter<String> adapter;
    SharedPreferences prefs;

    boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_selection);

        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);

        prefs = getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

        // 🚫 Pause blocking ONLY while user is here
        prefs.edit()
                .putBoolean("SETUP_IN_PROGRESS", true)
                .apply();

        listApps = findViewById(R.id.listApps);
        btnSave = findViewById(R.id.btnSaveApps);
        etSearch = findViewById(R.id.etSearch);

        loadInstalledApps();
        setupAdapter();

        // 🔒 Already blocked (EXCEPT permanent)
        Set<String> alreadyBlocked =
                prefs.getStringSet("BLOCKED_APPS", new HashSet<>());

        Set<String> permanentApps =
                prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());

        for (String pkg : alreadyBlocked) {
            if (!permanentApps.contains(pkg)) {
                checkedPackages.add(pkg);
            }
        }

        restoreCheckedToList();
        setupSearch();

        btnSave.setOnClickListener(v -> {

            // 🔥 CORE FIX
            rebuildCheckedFromUI();

            if (isEditMode) {
                // ✏️ DASHBOARD EDIT

                Set<String> finalBlocked = new HashSet<>(checkedPackages);
                finalBlocked.addAll(permanentApps); // permanent always preserved

                prefs.edit()
                        .putStringSet("BLOCKED_APPS", finalBlocked)
                        .putBoolean("SETUP_IN_PROGRESS", false)
                        .apply();

                Intent i = new Intent(this, FocusTimerActivity.class);
                i.putExtra("EDIT_MODE", true);
                startActivity(i);
                finish();

            } else {
                // 🆕 FIRST TIME SETUP

                prefs.edit()
                        .putStringSet(
                                "TEMP_SELECTED_APPS",
                                new HashSet<>(checkedPackages)
                        )
                        .putBoolean("SETUP_IN_PROGRESS", false)
                        .apply();

                startActivity(
                        new Intent(this, PermanentSelectionActivity.class)
                );
                finish();
            }
        });
    }

    // ==============================
    // 🛡️ SAFETY: resume blocking if user exits unexpectedly
    // ==============================
    @Override
    protected void onDestroy() {
        super.onDestroy();

        prefs.edit()
                .putBoolean("SETUP_IN_PROGRESS", false)
                .apply();
    }

    // ==============================
    // LOAD INSTALLED APPS
    // ==============================
    private void loadInstalledApps() {

        Set<String> permanentApps =
                prefs.getStringSet("PERMANENT_BLOCKED_APPS", new HashSet<>());

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        for (ApplicationInfo app : apps) {

            if (pm.getLaunchIntentForPackage(app.packageName) == null)
                continue;

            if (app.packageName.equals(getPackageName()))
                continue;

            if (permanentApps.contains(app.packageName))
                continue;

            String name = pm.getApplicationLabel(app).toString();
            allApps.add(new AppItem(name, app.packageName));
        }

        visibleApps.addAll(allApps);
    }

    private void setupAdapter() {
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                getVisibleNames()
        );
        listApps.setAdapter(adapter);
        listApps.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                rebuildCheckedFromUI();

                visibleApps.clear();
                for (AppItem app : allApps) {
                    if (app.appName.toLowerCase()
                            .contains(s.toString().toLowerCase())) {
                        visibleApps.add(app);
                    }
                }

                adapter.clear();
                adapter.addAll(getVisibleNames());
                adapter.notifyDataSetChanged();

                restoreCheckedToList();
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ==============================
    // CORE STATE SYNC
    // ==============================
    private void rebuildCheckedFromUI() {
        checkedPackages.clear();

        SparseBooleanArray checked = listApps.getCheckedItemPositions();
        for (int i = 0; i < visibleApps.size(); i++) {
            if (checked.get(i)) {
                checkedPackages.add(visibleApps.get(i).packageName);
            }
        }
    }

    private void restoreCheckedToList() {
        for (int i = 0; i < visibleApps.size(); i++) {
            if (checkedPackages.contains(visibleApps.get(i).packageName)) {
                listApps.setItemChecked(i, true);
            }
        }
    }

    private ArrayList<String> getVisibleNames() {
        ArrayList<String> names = new ArrayList<>();
        for (AppItem app : visibleApps) {
            names.add(app.appName);
        }
        return names;
    }

    // ==============================
    // MODEL
    // ==============================
    static class AppItem {
        String appName;
        String packageName;

        AppItem(String name, String pkg) {
            appName = name;
            packageName = pkg;
        }
    }
}
