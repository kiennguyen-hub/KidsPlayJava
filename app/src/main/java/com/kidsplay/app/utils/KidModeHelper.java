package com.kidsplay.app.utils;

import android.app.Activity;
import android.content.Context;

import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.entities.UsageLogEntity;

import java.util.concurrent.Executors;

public class KidModeHelper {

    public static void enableKidMode(Activity activity) {
        if (!AppSettings.isLockExternalApps(activity)) return;
        try {
            activity.startLockTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disableKidMode(Activity activity) {
        try {
            activity.stopLockTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logExitAttempt(Context context, long kidId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.kidDao().insertLog(new UsageLogEntity(kidId, "EXIT_ATTEMPT"));
        });
    }
}
