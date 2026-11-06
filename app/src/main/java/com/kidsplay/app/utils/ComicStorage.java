package com.kidsplay.app.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lưu các trang truyện tranh vào bộ nhớ riêng của app: filesDir/comics/<id>/0001.jpg, 0002.jpg...
 * Tên file đánh số có đệm số 0 nên sắp xếp theo tên là ra đúng thứ tự trang.
 */
public final class ComicStorage {

    private ComicStorage() {}

    public static final class Result {
        public final String folderPath;
        public final int pageCount;

        Result(String folderPath, int pageCount) {
            this.folderPath = folderPath;
            this.pageCount = pageCount;
        }
    }

    /** uris phải được sắp xếp đúng thứ tự trang trước khi gọi. */
    public static Result copyPages(Context context, List<Uri> uris, List<String> names) throws IOException {
        File folder = new File(new File(context.getFilesDir(), "comics"), UUID.randomUUID().toString());
        if (!folder.mkdirs()) throw new IOException("Không tạo được thư mục truyện");

        try {
            for (int i = 0; i < uris.size(); i++) {
                File out = new File(folder, String.format(Locale.US, "%04d.%s", i + 1, extensionOf(names.get(i))));
                try (InputStream in = context.getContentResolver().openInputStream(uris.get(i));
                     OutputStream os = new FileOutputStream(out)) {
                    if (in == null) throw new IOException("Không mở được ảnh trang " + (i + 1));
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        os.write(buffer, 0, n);
                    }
                }
            }
        } catch (IOException e) {
            deleteFolder(folder.getAbsolutePath()); // lỗi giữa chừng thì dọn sạch, không để truyện dở dang
            throw e;
        }
        return new Result(folder.getAbsolutePath(), uris.size());
    }

    public static void deleteFolder(String path) {
        if (path == null) return;
        File dir = new File(path);
        File[] children = dir.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f.isDirectory()) deleteFolder(f.getAbsolutePath());
                else if (!f.delete()) f.deleteOnExit();
            }
        }
        if (dir.exists() && !dir.delete()) dir.deleteOnExit();
    }

    /** So sánh tên file theo "thứ tự tự nhiên": page2 đứng trước page10. */
    public static int naturalCompare(String a, String b) {
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i), cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int si = i, sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) i++;
                while (j < b.length() && Character.isDigit(b.charAt(j))) j++;
                String na = a.substring(si, i).replaceFirst("^0+(?=.)", "");
                String nb = b.substring(sj, j).replaceFirst("^0+(?=.)", "");
                if (na.length() != nb.length()) return na.length() - nb.length();
                int c = na.compareTo(nb);
                if (c != 0) return c;
            } else {
                int c = Character.toLowerCase(ca) - Character.toLowerCase(cb);
                if (c != 0) return c;
                i++;
                j++;
            }
        }
        return (a.length() - i) - (b.length() - j);
    }

    private static String extensionOf(String name) {
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")
                        || ext.equals("webp") || ext.equals("gif")) {
                    return ext;
                }
            }
        }
        return "jpg";
    }
}
