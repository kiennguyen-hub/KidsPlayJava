package com.kidsplay.app.ui.parent;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.utils.AppSettings;

/** Các hộp thoại nhỏ dùng chung ở khu Phụ huynh. */
public final class ParentDialogs {

    private ParentDialogs() {}

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }

    /** Đặt số phút được dùng mỗi ngày cho một bé. */
    public static void showTimeLimit(Context context, KidProfileEntity kid, ParentViewModel viewModel) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Số phút mỗi ngày");
        input.setText(String.valueOf(kid.dailyLimitMinutes));
        input.setSelection(input.getText().length());

        LinearLayout box = new LinearLayout(context);
        box.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), 0);
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Thời gian sử dụng của " + kid.name)
                .setMessage("Bé được dùng tối đa bao nhiêu phút mỗi ngày?")
                .setView(box)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int minutes;
            try {
                minutes = Integer.parseInt(input.getText().toString().trim());
            } catch (NumberFormatException e) {
                minutes = 0;
            }
            if (minutes < 1 || minutes > 600) {
                input.setError("Nhập từ 1 đến 600 phút");
                return;
            }
            kid.dailyLimitMinutes = minutes;
            viewModel.updateKid(kid);
            Toast.makeText(context, "Đã cập nhật: " + minutes + " phút/ngày", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    /** Bật/tắt "Khóa ứng dụng ngoài" (ghim màn hình khi bé dùng app). */
    public static void showLockSetting(Context context) {
        SwitchCompat toggle = new SwitchCompat(context);
        toggle.setText("Khóa ứng dụng ngoài khi bé dùng app");
        toggle.setChecked(AppSettings.isLockExternalApps(context));

        LinearLayout box = new LinearLayout(context);
        box.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), 0);
        box.addView(toggle);

        new AlertDialog.Builder(context)
                .setTitle("Khóa ứng dụng ngoài")
                .setMessage("Khi bật, chế độ trẻ em sẽ ghim màn hình để bé không thoát sang ứng dụng khác. "
                        + "Muốn thoát phải nhập mã PIN phụ huynh.")
                .setView(box)
                .setPositiveButton("Lưu", (d, w) -> {
                    AppSettings.setLockExternalApps(context, toggle.isChecked());
                    Toast.makeText(context, toggle.isChecked() ? "Đã bật khóa ứng dụng ngoài"
                            : "Đã tắt khóa ứng dụng ngoài", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
