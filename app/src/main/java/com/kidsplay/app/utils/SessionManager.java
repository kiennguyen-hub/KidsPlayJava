package com.kidsplay.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

/** Ghi nhớ phụ huynh đang đăng nhập (chỉ lưu id và tên, không lưu mật khẩu). */
public class SessionManager {

    private static final String PREFS = "kidsplay_session";
    private static final String KEY_PARENT_ID = "parent_id";
    private static final String KEY_PARENT_NAME = "parent_name";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(long parentId, String name, String firebaseUid) {
        prefs.edit().putLong(KEY_PARENT_ID, parentId).putString(KEY_PARENT_NAME, name)
                .putString(KEY_FIREBASE_UID, firebaseUid).apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
    }

    public String getFirebaseUid() {
        return prefs.getString(KEY_FIREBASE_UID, null);
    }

    public boolean isLoggedIn() {
        return getParentId() > 0;
    }

    public long getParentId() {
        return prefs.getLong(KEY_PARENT_ID, -1);
    }

    public String getParentName() {
        return prefs.getString(KEY_PARENT_NAME, "");
    }
}
