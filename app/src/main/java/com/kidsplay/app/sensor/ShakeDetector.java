package com.kidsplay.app.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;
import android.util.Log;

/**
 * Phát hiện "lắc máy" bằng gia tốc kế.
 *
 * Cách làm: lọc bỏ trọng lực (bộ lọc thông thấp) để chỉ còn gia tốc do tay chuyển động,
 * rồi so với ngưỡng. Cách này không phụ thuộc tư thế cầm máy (đứng, ngang, nằm).
 * Ngưỡng thấp hơn bản cũ để bé lắc nhẹ cũng nhận, và để máy giả lập (cảm biến ảo
 * rất yếu) vẫn kích hoạt được.
 */
public class ShakeDetector implements SensorEventListener {

    private static final String TAG = "ShakeDetector";

    /** Gia tốc tuyến tính tối thiểu (m/s²) để tính là một cú lắc. Tăng lên nếu bị nhạy quá. */
    public static final float SHAKE_THRESHOLD = 5f;
    private static final long MIN_INTERVAL_MS = 800;
    private static final float ALPHA = 0.8f; // hệ số lọc trọng lực

    public interface OnShakeListener {
        void onShake();
    }

    private final OnShakeListener listener;
    private final float[] gravity = new float[3];
    private boolean gravityReady = false;
    private long lastShakeTime = 0;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float[] v = event.values;
        if (!gravityReady) {
            // Mẫu đầu tiên: coi toàn bộ là trọng lực
            System.arraycopy(v, 0, gravity, 0, 3);
            gravityReady = true;
            return;
        }

        for (int i = 0; i < 3; i++) {
            gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * v[i];
        }
        float lx = v[0] - gravity[0];
        float ly = v[1] - gravity[1];
        float lz = v[2] - gravity[2];
        double linear = Math.sqrt(lx * lx + ly * ly + lz * lz);

        if (linear > 2.0) {
            // Xem trong Logcat (lọc theo "ShakeDetector") để biết cảm biến có gửi dữ liệu không
            Log.d(TAG, "gia toc tuyen tinh = " + linear + " (nguong " + SHAKE_THRESHOLD + ")");
        }

        if (linear > SHAKE_THRESHOLD) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastShakeTime > MIN_INTERVAL_MS) {
                lastShakeTime = now;
                if (listener != null) listener.onShake();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
