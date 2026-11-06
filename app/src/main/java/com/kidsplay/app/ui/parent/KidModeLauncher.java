package com.kidsplay.app.ui.parent;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.ui.kid.KidModeActivity;
import com.kidsplay.app.utils.PinDialogs;

import java.util.concurrent.Executors;

/** Vào chế độ trẻ em: bắt buộc đã có PIN phụ huynh để còn thoát ra được. */
public final class KidModeLauncher {

    private KidModeLauncher() {}

    public static void enter(Fragment fragment, KidProfileEntity kid) {
        Activity activity = fragment.requireActivity();
        Executors.newSingleThreadExecutor().execute(() -> {
            String hash = AppDatabase.getDatabase(activity.getApplicationContext())
                    .parentDao().getPinHash(kid.parentId);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Runnable start = () -> {
                    Intent intent = new Intent(activity, KidModeActivity.class);
                    intent.putExtra("KID_ID", kid.id);
                    activity.startActivity(intent);
                };
                if (hash == null || hash.isEmpty()) {
                    Toast.makeText(activity, "Hãy đặt mã PIN trước khi vào chế độ trẻ em", Toast.LENGTH_LONG).show();
                    PinDialogs.showSetPin(activity, kid.parentId, start);
                } else {
                    start.run();
                }
            });
        });
    }
}
