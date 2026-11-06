package com.kidsplay.app.utils;

import android.content.Context;

/** Cài đặt đơn giản của ứng dụng (lưu SharedPreferences, không chứa dữ liệu nhạy cảm). */
public final class AppSettings {

    private static final String PREFS = "kidsplay_settings";
    private static final String KEY_LOCK_EXTERNAL = "lock_external_apps";

    private AppSettings() {}

    /** Bật = Kid Mode ghim màn hình, chặn trẻ thoát sang ứng dụng khác. Mặc định bật. */
    public static boolean isLockExternalApps(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LOCK_EXTERNAL, true);
    }

    public static void setLockExternalApps(Context context, boolean enabled) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_LOCK_EXTERNAL, enabled).apply();
    }
}
