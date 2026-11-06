package com.kidsplay.app.worker;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.KidDao;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.local.entities.UsageLogEntity;
import com.kidsplay.app.ui.lock.LockScreenActivity;
import com.kidsplay.app.utils.UsageTracker;

/**
 * Mỗi lần chạy = 1 phút bé đã dùng app. Cộng dồn vào database, nếu chạm giới hạn ngày thì
 * mở màn hình khóa và dừng chuỗi; nếu còn giờ thì tự lên lịch cho phút kế tiếp.
 */
public class UsageTrackingWorker extends Worker {

    public UsageTrackingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long kidId = getInputData().getLong(UsageTracker.KEY_KID_ID, -1);
        if (kidId == -1) return Result.failure();

        // Bé không còn ở trong app (app bị đóng / chuyển sang app khác) -> không đếm, dừng chuỗi.
        if (!UsageTracker.isTracking(kidId)) return Result.success();

        KidDao dao = AppDatabase.getDatabase(getApplicationContext()).kidDao();
        KidProfileEntity kid = UsageTracker.loadAndReset(dao, kidId);
        if (kid == null) return Result.failure();

        kid.usedMinutesToday += 1;
        dao.updateKid(kid);

        if (kid.usedMinutesToday >= kid.dailyLimitMinutes) {
            dao.insertLog(new UsageLogEntity(kidId, "TIME_LIMIT_REACHED"));
            Intent intent = new Intent(getApplicationContext(), LockScreenActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("KID_ID", kidId);
            getApplicationContext().startActivity(intent);
            return Result.success(); // dừng chuỗi: không lên lịch tiếp
        }

        UsageTracker.scheduleNext(getApplicationContext(), kidId);
        return Result.success();
    }
}
