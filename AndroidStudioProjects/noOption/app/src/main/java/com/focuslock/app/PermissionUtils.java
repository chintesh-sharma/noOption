package com.focuslock.app;

import android.content.Context;
import android.provider.Settings;

public class PermissionUtils {

    public static boolean isAccessibilityEnabled(Context context) {

        int enabled = 0;
        final String serviceName =
                context.getPackageName() + "/" + MyAccessibilityService.class.getCanonicalName();

        try {
            enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        if (enabled == 1) {
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            if (enabledServices != null) {
                return enabledServices.contains(serviceName);
            }
        }
        return false;
    }
}