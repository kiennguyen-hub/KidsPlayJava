package com.kidsplay.app.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kidsplay.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/** Ảnh đại diện: ảnh người dùng chọn (bo tròn) hoặc vòng tròn màu mặc định như thiết kế. */
public final class AvatarUtils {

    private AvatarUtils() {}

    private static final int[] BACKGROUNDS = {
            R.drawable.bg_avatar_pink, R.drawable.bg_avatar_yellow, R.drawable.bg_avatar_purple
    };

    /** seed (ví dụ id của bé) quyết định màu mặc định để mỗi bé có màu riêng, ổn định. */
    public static void bind(ImageView view, String avatarPath, long seed) {
        view.setBackgroundResource(BACKGROUNDS[(int) (Math.abs(seed) % BACKGROUNDS.length)]);
        if (avatarPath != null && new File(avatarPath).exists()) {
            Glide.with(view).load(new File(avatarPath)).circleCrop().into(view);
        } else {
            Glide.with(view).clear(view);
            view.setImageDrawable(null);
        }
    }

    /** Sao chép ảnh vào bộ nhớ riêng của app. Gọi trên luồng nền. */
    public static String copyAvatar(Context context, Uri uri) throws IOException {
        File dir = new File(context.getFilesDir(), "avatars");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Không tạo được thư mục ảnh đại diện");
        File out = new File(dir, UUID.randomUUID() + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("Không mở được ảnh");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) os.write(buffer, 0, n);
        }
        return out.getAbsolutePath();
    }
}
