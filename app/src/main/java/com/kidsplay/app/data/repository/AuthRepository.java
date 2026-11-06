package com.kidsplay.app.data.repository;

import android.app.Application;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.kidsplay.app.data.local.AppDatabase;
import com.kidsplay.app.data.local.ParentDao;
import com.kidsplay.app.data.local.entities.ParentEntity;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Đăng ký / đăng nhập bằng Firebase Authentication (Email + Mật khẩu).
 * Sau khi Firebase xác thực, tài khoản được "phản chiếu" xuống Room để các bảng
 * hồ sơ bé và nội dung vẫn liên kết theo parentId cục bộ. Mật khẩu KHÔNG lưu trong máy.
 */
public class AuthRepository {

    public interface Callback {
        /** parent = null khi thất bại. Có thể gọi trên luồng bất kỳ. */
        void onResult(ParentEntity parent, String error);
    }

    private final ParentDao dao;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AuthRepository(Application application) {
        dao = AppDatabase.getDatabase(application).parentDao();
    }

    public void register(String fullName, String email, String password, Callback callback) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String name = fullName.trim();
        auth.createUserWithEmailAndPassword(normalized, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onResult(null, "Không tạo được tài khoản");
                        return;
                    }
                    // Lưu tên hiển thị để đăng nhập trên máy khác vẫn thấy tên
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
                    mirrorAsync(user, name, callback);
                })
                .addOnFailureListener(e -> callback.onResult(null, mapError(e)));
    }

    public void login(String email, String password, Callback callback) {
        auth.signInWithEmailAndPassword(email.trim().toLowerCase(Locale.ROOT), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onResult(null, "Không lấy được thông tin tài khoản");
                        return;
                    }
                    mirrorAsync(user, null, callback);
                })
                .addOnFailureListener(e -> callback.onResult(null, mapError(e)));
    }

    private void mirrorAsync(FirebaseUser user, String nameHint, Callback callback) {
        executor.execute(() -> {
            try {
                callback.onResult(mirrorLocal(user, nameHint), null);
            } catch (Exception e) {
                callback.onResult(null, "Không lưu được tài khoản vào máy: " + e.getMessage());
            }
        });
    }

    /** Tạo hoặc cập nhật bản ghi cục bộ; nếu email đã có từ trước thì dùng lại để giữ nguyên hồ sơ bé. */
    private ParentEntity mirrorLocal(FirebaseUser user, String nameHint) {
        String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.ROOT);
        String name = nameHint;
        if (name == null || name.isEmpty()) name = user.getDisplayName();
        if (name == null || name.isEmpty()) name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;

        ParentEntity parent = dao.findByEmail(email);
        if (parent == null) {
            parent = new ParentEntity(name, email, ""); // không lưu mật khẩu trong máy
            parent.firebaseUid = user.getUid();
            parent.id = dao.insert(parent);
        } else {
            parent.firebaseUid = user.getUid();
            if (nameHint != null && !nameHint.isEmpty()) parent.fullName = nameHint;
            dao.update(parent);
        }
        return parent;
    }

    private static String mapError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) return "Email này đã được đăng ký";
        if (e instanceof FirebaseAuthWeakPasswordException) return "Mật khẩu quá yếu, hãy dùng ít nhất 6 ký tự";
        if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không đúng";
        }
        if (e instanceof FirebaseNetworkException) return "Không có kết nối mạng, hãy bật Internet để đăng nhập";
        if (e instanceof FirebaseTooManyRequestsException) return "Thử quá nhiều lần, vui lòng đợi một lúc rồi thử lại";
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("CONFIGURATION_NOT_FOUND") || msg.contains("OPERATION_NOT_ALLOWED")) {
            return "Chưa bật đăng nhập Email/Password trong Firebase Console (mục Authentication)";
        }
        return msg.isEmpty() ? "Đăng nhập thất bại" : msg;
    }
}
