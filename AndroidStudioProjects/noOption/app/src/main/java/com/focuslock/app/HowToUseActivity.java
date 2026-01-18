package com.focuslock.app;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HowToUseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_use);

        WebView webView = findViewById(R.id.webViewHowTo);
        Button btnContinue = findViewById(R.id.btnContinueHowTo);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        // ✅ GITHUB PAGES VIDEO (WORKS PERFECTLY)
        webView.loadUrl("https://chintesh-sharma.github.io/noOption_Help2/");

        // ⬅️ Back to PermissionSetupActivity
        btnContinue.setOnClickListener(v -> finish());
    }
}
