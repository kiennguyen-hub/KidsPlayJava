package com.kidsplay.app.ui.lock;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.KidDao;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.local.entities.UsageLogEntity;
import com.kidsplay.app.databinding.ActivityLockScreenBinding;
import com.kidsplay.app.ui.parent.ParentDashboardActivity;
import com.kidsplay.app.utils.KidModeHelper;
import com.kidsplay.app.utils.PinHasher;

import java.util.concurrent.Executors;

/**
 * Màn hình khóa khi bé hết giờ. Bé không thoát được (Back bị chặn).
 * Phụ huynh nhập đúng PIN thì chọn: thêm giờ cho bé hoặc thoát về trang Phụ huynh.
 */
public class LockScreenActivity extends AppCompatActivity {

    private static final int EXTRA_MINUTES = 15;

    private long kidId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityLockScreenBinding binding = ActivityLockScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        kidId = getIntent().getLongExtra("KID_ID", -1);

        binding.btnRequestMoreTime.setOnClickListener(v -> requestMoreTime());
        binding.btnParentUnlock.setOnClickListener(v -> showPinDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(LockScreenActivity.this, "Đã hết thời lượng sử dụng trong ngày!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Bé bấm xin thêm giờ: ghi lại yêu cầu để phụ huynh xem trong nhật ký sử dụng. */
    private void requestMoreTime() {
        Executors.newSingleThreadExecutor().execute(() ->
                AppDatabase.getDatabase(getApplicationContext()).kidDao()
                        .insertLog(new UsageLogEntity(kidId, "EXTRA_TIME_REQUEST")));
        Toast.makeText(this, "Đã gửi yêu cầu thêm giờ tới phụ huynh!", Toast.LENGTH_LONG).show();
    }

    private void showPinDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle("Nhập mã PIN Phụ huynh")
                .setView(input)
                .setPositiveButton("Xác nhận", (dialog, which) -> verifyPin(input.getText().toString()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void verifyPin(String enteredPin) {
        Executors.newSingleThreadExecutor().execute(() -> {
            KidProfileEntity kid = AppDatabase.getDatabase(getApplicationContext()).kidDao().getKidById(kidId);
            String pinHash = kid == null ? null
                    : AppDatabase.getDatabase(getApplicationContext()).parentDao().getPinHash(kid.parentId);
            // Chưa đặt PIN (không xảy ra trong luồng bình thường) thì không chặn phụ huynh
            boolean ok = kid != null && (pinHash == null || pinHash.isEmpty() || PinHasher.matches(pinHash, enteredPin));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (ok) {
                    showUnlockOptions();
                } else {
                    Toast.makeText(this, "Mã PIN không chính xác!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showUnlockOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Xác thực thành công")
                .setItems(new String[]{
                        "Thêm " + EXTRA_MINUTES + " phút cho bé",
                        "Thoát về trang Phụ huynh"
                }, (dialog, which) -> {
                    if (which == 0) {
                        grantExtraTime();
                    } else {
                        exitToDashboard();
                    }
                })
                .setCancelable(false)
                .show();
    }

    /** Cộng thêm giờ rồi đóng màn hình khóa; Kid Mode phía sau tự đếm giờ tiếp ở onResume. */
    private void grantExtraTime() {
        Executors.newSingleThreadExecutor().execute(() -> {
            KidDao dao = AppDatabase.getDatabase(getApplicationContext()).kidDao();
            KidProfileEntity kid = dao.getKidById(kidId);
            if (kid != null) {
                kid.dailyLimitMinutes = Math.max(kid.dailyLimitMinutes, kid.usedMinutesToday) + EXTRA_MINUTES;
                dao.updateKid(kid);
                dao.insertLog(new UsageLogEntity(kidId, "EXTRA_TIME_GRANTED"));
            }
            runOnUiThread(this::finish);
        });
    }

    private void exitToDashboard() {
        KidModeHelper.disableKidMode(this);
        Intent intent = new Intent(this, ParentDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
