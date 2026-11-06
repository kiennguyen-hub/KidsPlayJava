package com.kidsplay.app.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kidsplay.app.data.local.entities.ParentEntity;
import com.kidsplay.app.data.repository.AuthRepository;
import com.kidsplay.app.utils.SessionManager;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final SessionManager session;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
        session = new SessionManager(application);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getSuccess() { return success; }

    public void login(String email, String password) {
        loading.setValue(true);
        repository.login(email, password, this::handleResult);
    }

    public void register(String fullName, String email, String password) {
        loading.setValue(true);
        repository.register(fullName, email, password, this::handleResult);
    }

    private void handleResult(ParentEntity parent, String err) {
        loading.postValue(false);
        if (parent != null) {
            session.login(parent.id, parent.fullName, parent.firebaseUid);
            success.postValue(true);
        } else {
            error.postValue(err);
        }
    }

    public void consumeError() {
        error.setValue(null);
    }
}
