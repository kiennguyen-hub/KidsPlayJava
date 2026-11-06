package com.kidsplay.app.ui.kid;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.repository.ContentRepository;

import java.util.Collections;
import java.util.List;

/** Lấy hồ sơ bé theo id, rồi lọc thư viện theo độ tuổi của bé đó. */
public class KidHomeViewModel extends AndroidViewModel {

    private final MutableLiveData<Long> kidId = new MutableLiveData<>();
    private final LiveData<KidProfileEntity> kid;
    private final LiveData<List<ContentEntity>> contents;

    public KidHomeViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        ContentRepository repository = new ContentRepository(application, -1);
        MutableLiveData<List<ContentEntity>> empty = new MutableLiveData<>(Collections.emptyList());

        kid = Transformations.switchMap(kidId, id -> db.kidDao().getKidLive(id));
        contents = Transformations.switchMap(kid, k -> k == null ? empty : repository.getForAge(k.currentAge(), k.parentId));
    }

    public void setKidId(long id) {
        Long current = kidId.getValue();
        if (current == null || current != id) {
            kidId.setValue(id);
        }
    }

    public LiveData<KidProfileEntity> getKid() { return kid; }
    public LiveData<List<ContentEntity>> getContents() { return contents; }
}
