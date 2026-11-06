package com.kidsplay.app.ui.parent;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.data.repository.ContentRepository;
import com.kidsplay.app.utils.SessionManager;

import java.util.List;

/** Dùng chung cho ContentManageFragment và AddContentFragment (gắn với Activity). */
public class ContentViewModel extends AndroidViewModel {

    private final ContentRepository repository;
    private final LiveData<List<ContentEntity>> allContents;

    private final MutableLiveData<Boolean> uploading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> done = new MutableLiveData<>();
    private final MutableLiveData<String> cloudMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> syncing = new MutableLiveData<>(false);

    public ContentViewModel(@NonNull Application application) {
        super(application);
        repository = new ContentRepository(application, new SessionManager(application).getParentId());
        allContents = repository.getAll();
        repository.setCloudListener(cloudMessage::postValue);
    }

    public LiveData<List<ContentEntity>> getAllContents() { return allContents; }
    public LiveData<Boolean> getUploading() { return uploading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> getDone() { return done; }
    public LiveData<String> getCloudMessage() { return cloudMessage; }
    public LiveData<Boolean> getSyncing() { return syncing; }

    /** Đồng bộ với Firebase. silent = true: chỉ báo khi thật sự có thay đổi hoặc lỗi. */
    public void syncNow(boolean silent) {
        if (Boolean.TRUE.equals(syncing.getValue())) return;
        syncing.setValue(true);
        repository.syncNow(silent, () -> syncing.postValue(false));
    }

    public void consumeCloudMessage() {
        cloudMessage.setValue(null);
    }

    public void uploadVideo(Uri uri, ContentRepository.Meta meta) {
        uploading.setValue(true);
        repository.addVideo(uri, meta, this::onResult);
    }

    public void uploadComic(List<Uri> pages, ContentRepository.Meta meta) {
        uploading.setValue(true);
        repository.addComic(pages, meta, this::onResult);
    }

    public void addStory(ContentRepository.Meta meta, String body) {
        uploading.setValue(true);
        repository.addStory(meta, body, this::onResult);
    }

    public void setPublished(ContentEntity content, boolean published) {
        repository.setPublished(content, published);
    }

    public void delete(ContentEntity content) {
        repository.delete(content);
    }

    private void onResult(boolean success, String msg) {
        uploading.postValue(false);
        message.postValue(msg);
        if (success) done.postValue(true);
    }

    /** Gọi sau khi UI đã xử lý sự kiện để không bị phát lại khi mở lại màn hình. */
    public void consumeEvents() {
        message.setValue(null);
        done.setValue(null);
    }
}
