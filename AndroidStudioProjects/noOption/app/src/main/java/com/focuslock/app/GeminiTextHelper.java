package com.focuslock.app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class GeminiTextHelper {

    public interface GeminiCallback {
        void onResult(String text);
        void onError(String error);
    }

    // 🔹 Local emergency fallback (Gemini-style)
    private static final String[] SAFE_GEMINI_FALLBACK = {
            "Distraction ko block kar, future unlock kar 🔓",
            "Bas thoda focus, phir success 🚀",
            "Phone ruk ja, dreams chal rahe hain 💭",
            "Aaj focus, kal freedom 💪",
            "Self-control is the real power 🔥"
    };

    public static void generateText(
            String userPrompt,
            GeminiCallback callback
    ) {
        new Thread(() -> {
            try {

                String apiKey = BuildConfig.GEMINI_API_KEY;

                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key="
                                + apiKey
                );

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                // Request body
                JSONObject textPart = new JSONObject();
                textPart.put("text", userPrompt);

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject content = new JSONObject();
                content.put("role", "user");
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject body = new JSONObject();
                body.put("contents", contents);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(conn.getInputStream())
                        );

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("GEMINI_DEBUG", "RAW RESPONSE: " + response);

                JSONObject json = new JSONObject(response.toString());

                JSONArray candidates = json.getJSONArray("candidates");
                JSONObject contentObj =
                        candidates.getJSONObject(0).getJSONObject("content");

                JSONArray partsArr = contentObj.getJSONArray("parts");
                String resultText =
                        partsArr.getJSONObject(0).getString("text");

                if (resultText == null || resultText.trim().isEmpty()) {
                    throw new Exception("Empty Gemini response");
                }

                new Handler(Looper.getMainLooper())
                        .post(() -> callback.onResult(resultText.trim()));

            } catch (Exception e) {
                Log.e("GEMINI_DEBUG", "Gemini failed, using safe fallback", e);

                //  ALWAYS RETURN SOMETHING (NO SILENCE)
                String fallback =
                        SAFE_GEMINI_FALLBACK[
                                new Random().nextInt(SAFE_GEMINI_FALLBACK.length)
                                ];

                new Handler(Looper.getMainLooper())
                        .post(() -> callback.onResult(fallback));
            }
        }).start();
    }
}
