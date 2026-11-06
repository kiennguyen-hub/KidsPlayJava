package com.kidsplay.app.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.KidDao;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.local.entities.UsageLogEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KidsPlayRepository {
    private final KidDao kidDao;
    private final LiveData<List<KidProfileEntity>> allKids;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface OnKidInsertedListener {
        void onInserted(long id);
    }

    public KidsPlayRepository(Application application, long parentId) {
        AppDatabase db = AppDatabase.getDatabase(application);
        kidDao = db.kidDao();
        allKids = kidDao.getKidsByParent(parentId);
    }

    public LiveData<List<KidProfileEntity>> getAllKids() {
        return allKids;
    }

    public void insertKid(KidProfileEntity kid, OnKidInsertedListener listener) {
        executorService.execute(() -> {
            long id = kidDao.insertKid(kid);
            if (listener != null) {
                listener.onInserted(id);
            }
        });
    }

    public void updateKid(KidProfileEntity kid) {
        executorService.execute(() -> kidDao.updateKid(kid));
    }

    public LiveData<KidProfileEntity> getKid(long id) {
        return kidDao.getKidLive(id);
    }

    public void deleteKid(KidProfileEntity kid, Runnable onDone) {
        executorService.execute(() -> {
            kidDao.deleteLogsForKid(kid.id);
            kidDao.deleteKid(kid);
            if (kid.avatarPath != null) {
                java.io.File f = new java.io.File(kid.avatarPath);
                if (f.exists() && !f.delete()) f.deleteOnExit();
            }
            if (onDone != null) onDone.run();
        });
    }

    public void insertLog(UsageLogEntity log) {
        executorService.execute(() -> kidDao.insertLog(log));
    }
}
