package com.focuslock.app;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class OverlayProtectionService extends Service {

    private WindowManager windowManager;
    private View overlayView;

    public static boolean isOverlayShowing = false;


    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showOverlay();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isOverlayShowing = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ================= OVERLAY UI =================
    private void showOverlay() {

        if (overlayView != null || isOverlayShowing) return;

        isOverlayShowing = true;


        overlayView = LayoutInflater.from(this)
                .inflate(R.layout.overlay_lock, null);

        Button btnExit = overlayView.findViewById(R.id.btnExitOverlay);
        btnExit.setOnClickListener(v -> {

            removeOverlay();

            // ✅ IMPORTANT: reset RECENTS flag here
            SharedPreferences prefs =
                    getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

            prefs.edit()
                    .putBoolean("RECENTS_OPEN", false)
                    .apply();

            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });


        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        type,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(overlayView, params);
    }

    private void removeOverlay() {

        if (overlayView != null) {

            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}

            overlayView = null;
            isOverlayShowing = false;

            // ✅ RESET FLAG ONLY when overlay actually removed
            SharedPreferences prefs =
                    getSharedPreferences("FOCUS_PREFS", MODE_PRIVATE);

            prefs.edit()
                    .putBoolean("RECENTS_OPEN", false)
                    .apply();
        }
    }

}