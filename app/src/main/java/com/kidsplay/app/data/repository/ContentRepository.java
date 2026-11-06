package com.kidsplay.app.data.repository;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.kidsplay.app.data.cloud.CloudContentSync;
import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.ContentDao;
import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.utils.ComicStorage;
import com.kidsplay.app.utils.VideoStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentRepository {

    public interface ResultCallback {
        /** Được gọi trên luồng nền. */
        void onResult(boolean success, String message);
    }

    /** Thông tin chung của một nội dung khi thêm mới (theo form "Thêm truyện mới"). */
    public static class Meta {
        public String title;
        public String description;
        public String category;
        public int minAge = 0;
        public int maxAge = 99;
        public boolean published = true;
    }

    private final Application app;
    private final long parentId;
    private final ContentDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CloudContentSync cloud;
    private CloudContentSync.Listener cloudListener;

    public ContentRepository(Application application, long parentId) {
        this.app = application;
        this.parentId = parentId;
        this.dao = AppDatabase.getDatabase(application).contentDao();
        this.cloud = new CloudContentSync(application, parentId);
    }

    public void setCloudListener(CloudContentSync.Listener listener) {
        this.cloudListener = listener;
    }

    /** Đồng bộ hai chiều với Firebase (đẩy mục chưa lên cloud, tải mục còn thiếu về máy). */
    public void syncNow(boolean silent, Runnable onDone) {
        cloud.syncAll(cloudListener, silent, onDone);
    }

    public LiveData<List<ContentEntity>> getAll() {
        return dao.getAll(parentId);
    }

    /** Nội dung đã xuất bản của đúng phụ huynh sở hữu bé, phù hợp với tuổi bé. */
    public LiveData<List<ContentEntity>> getForAge(int age, long ownerParentId) {
        return dao.getForAge(age, ownerParentId);
    }

    private void apply(ContentEntity e, Meta m) {
        e.description = m.description;
        e.category = m.category;
        e.maxAge = m.maxAge;
        e.published = m.published;
        e.parentId = parentId;
    }

    public void addVideo(Uri uri, Meta meta, ResultCallback callback) {
        executor.execute(() -> {
            try {
                VideoStorage.Result r = VideoStorage.copyVideo(app, uri);
                ContentEntity video = new ContentEntity(ContentEntity.TYPE_VIDEO, meta.title, null, r.path, meta.minAge, r.durationMs);
                apply(video, meta);
                video.id = dao.insert(video);
                callback.onResult(true, "Đã lưu video vào thư viện");
                cloud.uploadAsync(video, cloudListener);
            } catch (Exception e) {
                callback.onResult(false, "Tải video thất bại: " + e.getMessage());
            }
        });
    }

    public void addStory(Meta meta, String body, ResultCallback callback) {
        executor.execute(() -> {
            try {
                ContentEntity story = new ContentEntity(ContentEntity.TYPE_STORY, meta.title, body, null, meta.minAge, 0);
                apply(story, meta);
                story.id = dao.insert(story);
                callback.onResult(true, "Đã lưu truyện vào thư viện");
                cloud.uploadAsync(story, cloudListener);
            } catch (Exception e) {
                callback.onResult(false, "Thêm truyện thất bại: " + e.getMessage());
            }
        });
    }

    /** Truyện tranh: sắp xếp ảnh theo tên file (thứ tự tự nhiên) rồi lưu thành các trang. */
    public void addComic(List<Uri> uris, Meta meta, ResultCallback callback) {
        executor.execute(() -> {
            try {
                List<Integer> order = new ArrayList<>();
                List<String> rawNames = new ArrayList<>();
                for (int i = 0; i < uris.size(); i++) {
                    order.add(i);
                    String n = VideoStorage.queryName(app, uris.get(i));
                    rawNames.add(n != null ? n : uris.get(i).toString());
                }
                Collections.sort(order, (x, y) -> ComicStorage.naturalCompare(rawNames.get(x), rawNames.get(y)));

                List<Uri> sortedUris = new ArrayList<>();
                List<String> sortedNames = new ArrayList<>();
                for (int idx : order) {
                    sortedUris.add(uris.get(idx));
                    sortedNames.add(rawNames.get(idx));
                }

                ComicStorage.Result r = ComicStorage.copyPages(app, sortedUris, sortedNames);
                ContentEntity comic = new ContentEntity(ContentEntity.TYPE_COMIC, meta.title, null, r.folderPath, meta.minAge, 0);
                comic.pageCount = r.pageCount;
                apply(comic, meta);
                comic.id = dao.insert(comic);
                callback.onResult(true, "Đã lưu truyện tranh (" + r.pageCount + " trang)");
                cloud.uploadAsync(comic, cloudListener);
            } catch (Exception e) {
                callback.onResult(false, "Tải truyện tranh thất bại: " + e.getMessage());
            }
        });
    }

    /** Xuất bản / ẩn một nội dung (trẻ chỉ thấy nội dung đã xuất bản). */
    public void setPublished(ContentEntity content, boolean published) {
        executor.execute(() -> {
            dao.setPublished(content.id, published);
            String cloudId = dao.getCloudId(content.id);
            if (cloudId != null) cloud.updatePublishedAsync(cloudId, published);
        });
    }

    public void delete(ContentEntity content) {
        executor.execute(() -> {
            String cloudId = dao.getCloudId(content.id);
            dao.delete(content);
            if (ContentEntity.TYPE_VIDEO.equals(content.type)) {
                VideoStorage.deleteFile(content.filePath);
            } else if (ContentEntity.TYPE_COMIC.equals(content.type)) {
                ComicStorage.deleteFolder(content.filePath);
            }
            if (cloudId != null) {
                cloud.deleteAsync(cloudId, cloudListener);
            }
        });
    }
}
