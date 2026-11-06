package com.kidsplay.app.ui.kid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.kidsplay.app.R;
import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.databinding.ActivityKidModeBinding;
import com.kidsplay.app.ui.lock.LockScreenActivity;
import com.kidsplay.app.utils.KidModeHelper;
import com.kidsplay.app.utils.UsageTracker;

import java.util.concurrent.Executors;

/**
 * Activity chủ của Kid Mode. Nội dung hiển thị qua Fragment:
 * KidHomeFragment -> VideoPlayerFragment / StoryTextFragment.
 * Đồng thời bật/tắt đếm giờ sử dụng theo vòng đời (onResume/onPause).
 */
public class KidModeActivity extends AppCompatActivity {

    private long kidId = -1;
    private boolean resumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityKidModeBinding binding = ActivityKidModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        kidId = getIntent().getLongExtra("KID_ID", -1);

        KidModeHelper.enableKidMode(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.kidContainer, KidHomeFragment.newInstance(kidId))
                    .commit();
        }

        // Back: nếu đang xem video/truyện thì quay về danh sách; nếu đã ở danh sách
        // thì coi là cố thoát khỏi Kid Mode -> chỉ ghi log, không cho thoát.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    KidModeHelper.logExitAttempt(KidModeActivity.this, kidId);
                    Toast.makeText(KidModeActivity.this,
                            "Bé đang ở Chế độ Trẻ em. Muốn thoát hãy nhờ bố mẹ nhé!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        checkLimitThenTrack();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        UsageTracker.stop(this, kidId);
    }

    /** Hết giờ rồi (hoặc sang ngày mới thì reset) -> khóa ngay hoặc bắt đầu đếm giờ. */
    private void checkLimitThenTrack() {
        if (kidId < 0) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            KidProfileEntity kid = UsageTracker.loadAndReset(
                    AppDatabase.getDatabase(getApplicationContext()).kidDao(), kidId);
            if (kid == null) return;
            boolean overLimit = kid.usedMinutesToday >= kid.dailyLimitMinutes;
            runOnUiThread(() -> {
                if (!resumed || isFinishing() || isDestroyed()) return;
                if (overLimit) {
                    openLockScreen();
                } else {
                    UsageTracker.start(getApplicationContext(), kidId);
                }
            });
        });
    }

    private void openLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.putExtra("KID_ID", kidId);
        startActivity(intent);
    }

    public long getKidId() {
        return kidId;
    }

    /** Mở một màn con (video/truyện), Back sẽ quay lại danh sách. */
    public void openFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.kidContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
