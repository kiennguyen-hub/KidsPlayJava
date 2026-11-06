package com.kidsplay.app.ui.parent;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.repository.KidsPlayRepository;
import com.kidsplay.app.utils.SessionManager;

import java.util.List;

public class ParentViewModel extends AndroidViewModel {

    private final KidsPlayRepository repository;
    private final LiveData<List<KidProfileEntity>> allKids;

    public ParentViewModel(@NonNull Application application) {
        super(application);
        repository = new KidsPlayRepository(application, new SessionManager(application).getParentId());
        allKids = repository.getAllKids();
    }

    public LiveData<List<KidProfileEntity>> getAllKids() {
        return allKids;
    }

    public LiveData<KidProfileEntity> getKid(long id) {
        return repository.getKid(id);
    }

    public void updateKid(KidProfileEntity kid) {
        repository.updateKid(kid);
    }

    public void deleteKid(KidProfileEntity kid, Runnable onDone) {
        repository.deleteKid(kid, onDone);
    }

    public void createKidProfile(KidProfileEntity kid, KidsPlayRepository.OnKidInsertedListener listener) {
        repository.insertKid(kid, listener);
    }
}
