package com.focuslock.app;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PermanentAppsViewActivity extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permanent_apps_view);

        listView = findViewById(R.id.listPermanentApps);

        SharedPreferences prefs = getSharedPreferences("FocusLockPrefs", MODE_PRIVATE);
        Set<String> permanentApps = prefs.getStringSet("PERMANENT_BLOCKED_APPS", null);

        List<String> appNames = new ArrayList<>();

        if (permanentApps != null) {
            PackageManager pm = getPackageManager();
            for (String pkg : permanentApps) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    appNames.add(pm.getApplicationLabel(info).toString());
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appNames);

        listView.setAdapter(adapter);
    }
}
