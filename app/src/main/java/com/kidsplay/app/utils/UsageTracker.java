package com.kidsplay.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.WorkerThread;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.kidsplay.app.data.local.KidDao;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.worker.UsageTrackingWorker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Đếm giờ sử dụng của bé bằng WorkManager.
 *
 * - PeriodicWork tối thiểu 15 phút nên không dùng được. Thay vào đó, mỗi lần chạy xong
 *   Worker tự lên lịch cho phút kế tiếp (chuỗi OneTimeWork, mỗi mắt xích cách nhau 1 phút).
 * - Chỉ đếm khi bé thực sự đang ở trong app: các Activity gọi start() ở onResume()
 *   và stop() ở onPause(). stop() được trì hoãn ~1,5 giây để khi chuyển giữa hai màn hình
 *   của app (Kid Mode -> Đọc truyện) bộ đếm không bị ngắt rồi tính lại từ đầu.
 */
public final class UsageTracker {

    public static final String WORK_NAME = "kidsplay_usage_tracking";
    public static final String KEY_KID_ID = "KID_ID";
    private static final long STOP_DELAY_MS = 1500;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Runnable pendingStop;
    private static volatile long trackedKidId = -1;

    private UsageTracker() {}

    public static void start(Context context, long kidId) {
        if (kidId < 0) return;
        if (pendingStop != null) {
            MAIN.removeCallbacks(pendingStop);
            pendingStop = null;
        }
        if (trackedKidId == kidId) return; // chuỗi đếm giờ đang chạy sẵn
        trackedKidId = kidId;
        scheduleNext(context.getApplicationContext(), kidId);
    }

    public static void stop(Context context, long kidId) {
        if (kidId < 0) return;
        final Context app = context.getApplicationContext();
        if (pendingStop != null) MAIN.removeCallbacks(pendingStop);
        pendingStop = () -> {
            pendingStop = null;
            trackedKidId = -1;
            WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME);
        };
        MAIN.postDelayed(pendingStop, STOP_DELAY_MS);
    }

    /** Worker dùng để kiểm tra bé có còn đang dùng app không (tránh đếm khi app đã bị đóng). */
    public static boolean isTracking(long kidId) {
        return trackedKidId == kidId;
    }

    public static void scheduleNext(Context context, long kidId) {
        Data input = new Data.Builder().putLong(KEY_KID_ID, kidId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UsageTrackingWorker.class)
                .setInputData(input)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Đọc hồ sơ bé và tự reset số phút đã dùng nếu đã sang ngày mới. Gọi trên luồng nền. */
    @WorkerThread
    public static KidProfileEntity loadAndReset(KidDao dao, long kidId) {
        KidProfileEntity kid = dao.getKidById(kidId);
        if (kid == null) return null;
        String today = today();
        if (!today.equals(kid.lastResetDate)) {
            kid.usedMinutesToday = 0;
            kid.lastResetDate = today;
            dao.updateKid(kid);
        }
        return kid;
    }
}
