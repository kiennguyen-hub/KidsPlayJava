package com.kidsplay.app.data.cloud;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.ContentDao;
import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.utils.ComicStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Đồng bộ nội dung của phụ huynh với Firebase:
 *  - Firestore:  users/{uid}/contents/{cloudId}   (thông tin: tiêu đề, loại, tuổi, danh sách file...)
 *  - Storage:    users/{uid}/content/{cloudId}/{tên file}   (video, các trang ảnh truyện tranh)
 *
 * Kid Mode vẫn đọc file trong máy (chạy được khi offline). Cloud đóng vai trò sao lưu và
 * đồng bộ giữa nhiều thiết bị: thêm mới -> tự tải lên; đăng nhập máy khác -> tải về.
 * Mọi thao tác chạy trên một luồng nền duy nhất nên không tải trùng.
 */
public class CloudContentSync {

    public interface Listener {
        void onMessage(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean SYNCING = new AtomicBoolean(false);
    private static final long FIRESTORE_TIMEOUT_S = 60;

    private final Application app;
    private final long parentId;
    private final ContentDao dao;

    public CloudContentSync(Application app, long parentId) {
        this.app = app;
        this.parentId = parentId;
        this.dao = AppDatabase.getDatabase(app).contentDao();
    }

    private static String currentUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    private boolean canSync() {
        return parentId > 0 && currentUid() != null;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static void notify(Listener l, String msg) {
        if (l != null) l.onMessage(msg);
    }

    // ------------------------------------------------------------------ tải lên

    /** Tải một nội dung vừa thêm lên cloud (nếu đã đăng nhập Firebase và có mạng). */
    public void uploadAsync(ContentEntity content, Listener listener) {
        EXECUTOR.execute(() -> {
            String uid = currentUid();
            if (!canSync() || uid == null) {
                notify(listener, "Chưa đăng nhập Firebase: nội dung chỉ được lưu trong máy");
                return;
            }
            if (!isOnline()) {
                notify(listener, "Không có mạng: đã lưu trong máy, bấm Đồng bộ khi có mạng");
                return;
            }
            try {
                notify(listener, "Đang tải \"" + content.title + "\" lên cloud...");
                uploadBlocking(uid, content);
                notify(listener, "Đã đồng bộ \"" + content.title + "\" lên cloud");
            } catch (Exception e) {
                notify(listener, "Chưa đồng bộ được lên cloud: " + describe(e) + ". Bấm Đồng bộ để thử lại");
            }
        });
    }

    private void uploadBlocking(String uid, ContentEntity c) throws Exception {
        if (dao.getCloudId(c.id) != null) return; // đã đồng bộ rồi, tránh tải trùng

        List<File> files = filesOf(c);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storage.setMaxUploadRetryTimeMillis(120_000);
        storage.setMaxOperationRetryTimeMillis(60_000);

        DocumentReference doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("contents").document();
        String cloudId = doc.getId();

        List<String> names = new ArrayList<>();
        for (File f : files) {
            if (!f.exists()) throw new IOException("Thiếu file " + f.getName());
            StorageReference ref = storage.getReference().child(storagePath(uid, cloudId, f.getName()));
            Tasks.await(ref.putFile(Uri.fromFile(f)));
            names.add(f.getName());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", c.title);
        data.put("type", c.type);
        data.put("minAge", c.minAge);
        data.put("durationMs", c.durationMs);
        data.put("pageCount", c.pageCount);
        data.put("body", c.body);
        data.put("description", c.description);
        data.put("category", c.category);
        data.put("maxAge", c.maxAge);
        data.put("published", c.published);
        data.put("files", names);
        data.put("createdAt", c.createdAt);
        Tasks.await(doc.set(data), FIRESTORE_TIMEOUT_S, TimeUnit.SECONDS);

        dao.setCloudId(c.id, cloudId);
    }

    private static List<File> filesOf(ContentEntity c) {
        if (ContentEntity.TYPE_VIDEO.equals(c.type) && c.filePath != null) {
            return Collections.singletonList(new File(c.filePath));
        }
        if (ContentEntity.TYPE_COMIC.equals(c.type) && c.filePath != null) {
            File[] pages = new File(c.filePath).listFiles((d, n) ->
                    n.toLowerCase(Locale.ROOT).matches(".+\\.(jpg|jpeg|png|webp|gif)"));
            if (pages == null) return Collections.emptyList();
            Arrays.sort(pages, (a, b) -> ComicStorage.naturalCompare(a.getName(), b.getName()));
            return new ArrayList<>(Arrays.asList(pages));
        }
        return Collections.emptyList(); // truyện chữ: nằm hẳn trong tài liệu Firestore
    }

    private static String storagePath(String uid, String cloudId, String fileName) {
        return "users/" + uid + "/content/" + cloudId + "/" + fileName;
    }

    /** Cập nhật trạng thái xuất bản/ẩn trên cloud. */
    public void updatePublishedAsync(String cloudId, boolean published) {
        EXECUTOR.execute(() -> {
            String uid = currentUid();
            if (uid == null || !isOnline()) return; // lần đồng bộ sau sẽ không ghi đè vì mục đã có cloudId
            try {
                Tasks.await(FirebaseFirestore.getInstance().collection("users").document(uid)
                        .collection("contents").document(cloudId).update("published", published),
                        FIRESTORE_TIMEOUT_S, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // không cập nhật được trên cloud thì bản trong máy vẫn đúng
            }
        });
    }

    // --------------------------------------------------------------------- xóa

    /** Xóa nội dung khỏi cloud (tài liệu Firestore và các file trong Storage). */
    public void deleteAsync(String cloudId, Listener listener) {
        EXECUTOR.execute(() -> {
            String uid = currentUid();
            if (uid == null || !isOnline()) {
                notify(listener, "Đã xóa trong máy; bản trên cloud chưa xóa được (không có mạng hoặc chưa đăng nhập)");
                return;
            }
            try {
                DocumentReference doc = FirebaseFirestore.getInstance()
                        .collection("users").document(uid).collection("contents").document(cloudId);
                DocumentSnapshot snap = Tasks.await(doc.get(), FIRESTORE_TIMEOUT_S, TimeUnit.SECONDS);
                for (String name : stringList(snap, "files")) {
                    try {
                        Tasks.await(FirebaseStorage.getInstance().getReference()
                                .child(storagePath(uid, cloudId, name)).delete());
                    } catch (Exception ignored) {
                        // file không còn trên cloud thì bỏ qua
                    }
                }
                Tasks.await(doc.delete(), FIRESTORE_TIMEOUT_S, TimeUnit.SECONDS);
                notify(listener, "Đã xóa khỏi cloud");
            } catch (Exception e) {
                notify(listener, "Đã xóa trong máy; xóa trên cloud lỗi: " + describe(e));
            }
        });
    }

    // ------------------------------------------------------------------ đồng bộ

    /** Đẩy các mục chưa lên cloud, rồi tải về các mục có trên cloud mà máy này chưa có. */
    public void syncAll(Listener listener, boolean silent, Runnable onDone) {
        EXECUTOR.execute(() -> {
            try {
                String uid = currentUid();
                if (!canSync() || uid == null) {
                    if (!silent) notify(listener, "Chưa đăng nhập Firebase");
                    return;
                }
                if (!isOnline()) {
                    if (!silent) notify(listener, "Không có kết nối mạng");
                    return;
                }
                if (!SYNCING.compareAndSet(false, true)) {
                    if (!silent) notify(listener, "Đang đồng bộ, vui lòng đợi");
                    return;
                }

                int uploaded = 0, downloaded = 0, failed = 0;
                String lastError = null;
                try {
                    for (ContentEntity c : dao.getPendingCloud(parentId)) {
                        try {
                            uploadBlocking(uid, c);
                            uploaded++;
                        } catch (Exception e) {
                            failed++;
                            lastError = describe(e);
                        }
                    }

                    QuerySnapshot snap = Tasks.await(FirebaseFirestore.getInstance()
                            .collection("users").document(uid).collection("contents").get(),
                            FIRESTORE_TIMEOUT_S, TimeUnit.SECONDS);
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (dao.findByCloudId(d.getId(), parentId) != null) continue;
                        try {
                            if (downloadOne(uid, d)) downloaded++;
                        } catch (Exception e) {
                            failed++;
                            lastError = describe(e);
                        }
                    }
                } catch (Exception e) {
                    notify(listener, "Đồng bộ thất bại: " + describe(e));
                    return;
                } finally {
                    SYNCING.set(false);
                }

                if (uploaded == 0 && downloaded == 0 && failed == 0) {
                    if (!silent) notify(listener, "Mọi thứ đã được đồng bộ");
                } else {
                    String msg = "Đồng bộ xong: tải lên " + uploaded + ", tải về " + downloaded;
                    if (failed > 0) msg += ", lỗi " + failed + " mục (" + lastError + ")";
                    notify(listener, msg);
                }
            } finally {
                if (onDone != null) onDone.run();
            }
        });
    }

    /** Tải một mục từ cloud về máy. Trả về false nếu tài liệu không hợp lệ và bị bỏ qua. */
    private boolean downloadOne(String uid, DocumentSnapshot d) throws Exception {
        String type = d.getString("type");
        String title = d.getString("title");
        if (type == null || title == null) return false;

        ContentEntity e = new ContentEntity(type, title, d.getString("body"), null,
                intOf(d, "minAge", 3), longOf(d, "durationMs", 0));
        e.parentId = parentId;
        e.cloudId = d.getId();
        e.pageCount = intOf(d, "pageCount", 0);
        e.description = d.getString("description");
        e.category = d.getString("category");
        e.maxAge = intOf(d, "maxAge", 99);
        Boolean pub = d.getBoolean("published");
        e.published = pub == null || pub;
        long created = longOf(d, "createdAt", 0);
        if (created > 0) e.createdAt = created;

        List<String> names = stringList(d, "files");
        for (String n : names) requireSafeName(n);
        FirebaseStorage storage = FirebaseStorage.getInstance();

        if (ContentEntity.TYPE_VIDEO.equals(type)) {
            if (names.isEmpty()) return false;
            File dir = new File(app.getFilesDir(), "videos");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Không tạo được thư mục video");
            String name = names.get(0);
            int dot = name.lastIndexOf('.');
            File out = new File(dir, UUID.randomUUID() + (dot >= 0 ? name.substring(dot) : ".mp4"));
            try {
                Tasks.await(storage.getReference().child(storagePath(uid, d.getId(), name)).getFile(out));
            } catch (Exception ex) {
                if (out.exists() && !out.delete()) out.deleteOnExit();
                throw ex;
            }
            e.filePath = out.getAbsolutePath();
        } else if (ContentEntity.TYPE_COMIC.equals(type)) {
            if (names.isEmpty()) return false;
            File folder = new File(new File(app.getFilesDir(), "comics"), UUID.randomUUID().toString());
            if (!folder.mkdirs()) throw new IOException("Không tạo được thư mục truyện");
            try {
                for (String n : names) {
                    Tasks.await(storage.getReference().child(storagePath(uid, d.getId(), n))
                            .getFile(new File(folder, n)));
                }
            } catch (Exception ex) {
                ComicStorage.deleteFolder(folder.getAbsolutePath()); // không để lại truyện dở dang
                throw ex;
            }
            e.filePath = folder.getAbsolutePath();
            e.pageCount = names.size();
        }
        dao.insert(e);
        return true;
    }

    // ------------------------------------------------------------------ tiện ích

    /** Tên file lấy từ cloud phải là tên thuần, không chứa đường dẫn (chống ghi ra ngoài thư mục). */
    private static void requireSafeName(String name) throws IOException {
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals("..")) {
            throw new IOException("Tên file không hợp lệ trên cloud");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(DocumentSnapshot d, String key) {
        Object v = d.get(key);
        List<String> out = new ArrayList<>();
        if (v instanceof List) {
            for (Object o : (List<Object>) v) if (o instanceof String) out.add((String) o);
        }
        return out;
    }

    private static int intOf(DocumentSnapshot d, String key, int def) {
        Long v = d.getLong(key);
        return v == null ? def : v.intValue();
    }

    private static long longOf(DocumentSnapshot d, String key, long def) {
        Long v = d.getLong(key);
        return v == null ? def : v;
    }

    private static String describe(Throwable t) {
        Throwable c = t;
        if (t instanceof ExecutionException && t.getCause() != null) c = t.getCause();
        if (c instanceof StorageException) {
            int code = ((StorageException) c).getErrorCode();
            if (code == StorageException.ERROR_NOT_AUTHORIZED || code == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "Storage từ chối quyền (kiểm tra Storage Rules)";
            }
            if (code == StorageException.ERROR_BUCKET_NOT_FOUND || code == StorageException.ERROR_PROJECT_NOT_FOUND) {
                return "Chưa bật Cloud Storage cho project (có thể cần gói Blaze)";
            }
            if (code == StorageException.ERROR_RETRY_LIMIT_EXCEEDED) return "mạng yếu, quá thời gian chờ";
            if (code == StorageException.ERROR_QUOTA_EXCEEDED) return "đã hết hạn mức Storage";
        }
        if (c instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) c).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "Firestore từ chối quyền (kiểm tra Firestore Rules)";
        }
        if (c instanceof TimeoutException) return "hết thời gian chờ";
        String m = c.getMessage();
        return m != null ? m : c.getClass().getSimpleName();
    }
}
