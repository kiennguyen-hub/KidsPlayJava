package com.kidsplay.app.utils;

import android.app.Activity;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.kidsplay.app.data.local.AppDatabase;

import java.util.concurrent.Executors;

/** Hộp thoại đặt / đổi mã PIN phụ huynh (PIN lưu dạng băm trong tài khoản). */
public final class PinDialogs {

    private PinDialogs() {}

    public static void showSetPin(Activity activity, long parentId, Runnable onSuccess) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String existing = AppDatabase.getDatabase(activity.getApplicationContext())
                    .parentDao().getPinHash(parentId);
            String hash = existing == null || existing.isEmpty() ? null : existing;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                buildDialog(activity, parentId, hash, onSuccess);
            });
        });
    }

    private static EditText field(Activity a, String hint) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        return e;
    }

    private static void buildDialog(Activity a, long parentId, String existingHash, Runnable onSuccess) {
        int pad = (int) (20 * a.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText current = existingHash != null ? field(a, "PIN hiện tại") : null;
        EditText pin1 = field(a, "PIN mới (4 - 6 số)");
        EditText pin2 = field(a, "Nhập lại PIN mới");
        if (current != null) box.addView(current);
        box.addView(pin1);
        box.addView(pin2);

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(existingHash == null ? "Đặt mã PIN phụ huynh" : "Đổi mã PIN phụ huynh")
                .setMessage("PIN dùng để thoát chế độ trẻ em và mở khóa khi bé hết giờ.")
                .setView(box)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (current != null && !PinHasher.matches(existingHash, current.getText().toString())) {
                current.setError("PIN hiện tại không đúng");
                return;
            }
            String p1 = pin1.getText().toString();
            if (!p1.matches("\\d{4,6}")) {
                pin1.setError("PIN gồm 4 đến 6 chữ số");
                return;
            }
            if (!p1.equals(pin2.getText().toString())) {
                pin2.setError("PIN nhập lại không khớp");
                return;
            }
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getDatabase(a.getApplicationContext()).parentDao()
                        .setPinHash(parentId, PinHasher.hash(p1));
                a.runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(a, "Đã lưu mã PIN", Toast.LENGTH_SHORT).show();
                    if (onSuccess != null) onSuccess.run();
                });
            });
        }));
        dialog.show();
    }
}
