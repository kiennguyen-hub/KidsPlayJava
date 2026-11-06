package com.kidsplay.app.ui.kid;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.kidsplay.app.databinding.ActivityStoryReaderBinding;
import com.kidsplay.app.sensor.LightSensorManager;
import com.kidsplay.app.sensor.ShakeDetector;
import com.kidsplay.app.utils.KidModeHelper;
import com.kidsplay.app.utils.UsageTracker;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StoryReaderActivity extends AppCompatActivity {

    private ActivityStoryReaderBinding binding;
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;
    private LightSensorManager lightSensorManager;

    private final List<String> pages = Arrays.asList(
            "Trang 1: Ngày xửa ngày xưa...",
            "Trang 2: Hoàng tử bắt đầu cuộc hành trình...",
            "Trang 3: Kết thúc có hậu!"
    );
    private int currentPageIndex = 0;
    private long kidId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        kidId = getIntent().getLongExtra("KID_ID", -1);

        KidModeHelper.enableKidMode(this);

        updatePageContent();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        shakeDetector = new ShakeDetector(() -> runOnUiThread(() -> changePage(1, true)));

        // Nút lật trang dự phòng: dùng khi máy không có cảm biến, máy giả lập, hoặc bé chưa quen lắc
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNext.setOnClickListener(v -> changePage(1, false));
        binding.btnPrev.setOnClickListener(v -> changePage(-1, false));

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            Toast.makeText(this, "Máy không có cảm biến gia tốc, hãy dùng nút Trang tiếp", Toast.LENGTH_LONG).show();
        }

        lightSensorManager = new LightSensorManager(sensorManager, isDark ->
                runOnUiThread(() -> binding.darkOverlayView.setVisibility(isDark ? View.VISIBLE : View.GONE))
        );

        // Thay thế onBackPressed() cũ
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Quay về danh sách nội dung của Kid Mode (vẫn ở trong app, không phải thoát app)
                finish();
            }
        });
    }

    private void changePage(int delta, boolean fromShake) {
        int target = currentPageIndex + delta;
        if (target < 0 || target >= pages.size()) {
            if (fromShake) Toast.makeText(this, "Đã đến trang cuối rồi!", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPageIndex = target;
        updatePageContent();
        if (fromShake) Toast.makeText(this, "Đã lắc lật sang trang mới!", Toast.LENGTH_SHORT).show();
    }

    private void updatePageContent() {
        binding.tvStoryContent.setText(pages.get(currentPageIndex));
        // Sửa lỗi Nối chuỗi trong setText -> Sử dụng String.format
        binding.tvPageNumber.setText(String.format(Locale.getDefault(), "%d/%d", currentPageIndex + 1, pages.size()));
        binding.btnPrev.setEnabled(currentPageIndex > 0);
        binding.btnNext.setEnabled(currentPageIndex < pages.size() - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelSensor != null) {
            sensorManager.registerListener(shakeDetector, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        lightSensorManager.start();
        UsageTracker.start(getApplicationContext(), kidId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeDetector);
        lightSensorManager.stop();
        UsageTracker.stop(this, kidId);
    }
}