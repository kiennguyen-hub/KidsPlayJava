package com.kidsplay.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Sao chép video người dùng chọn vào bộ nhớ RIÊNG của ứng dụng
 * (filesDir/videos). Các app khác không đọc được file này -> an toàn hơn
 * so với giữ đường dẫn ra thư viện ảnh/video chung.
 */
public final class VideoStorage {

    private VideoStorage() {}

    public static final class Result {
        public final String path;
        public final long durationMs;

        Result(String path, long durationMs) {
            this.path = path;
            this.durationMs = durationMs;
        }
    }

    public static Result copyVideo(Context context, Uri uri) throws IOException {
        File dir = new File(context.getFilesDir(), "videos");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Không tạo được thư mục lưu video");
        }
        File out = new File(dir, UUID.randomUUID() + ".mp4");

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Không mở được video đã chọn");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                os.write(buffer, 0, n);
            }
        }

        long duration = 0;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(out.getAbsolutePath());
            String d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) duration = Long.parseLong(d);
        } catch (Exception ignored) {
            // không đọc được thời lượng thì để 0, video vẫn dùng được
        } finally {
            retriever.release();
        }
        return new Result(out.getAbsolutePath(), duration);
    }

    public static void deleteFile(String path) {
        if (path == null) return;
        File f = new File(path);
        if (f.exists() && !f.delete()) {
            f.deleteOnExit();
        }
    }

    public static String queryName(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {
            // bỏ qua, dùng tên mặc định
        }
        return null;
    }
}
