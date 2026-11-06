package com.kidsplay.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tài khoản phụ huynh. Email là duy nhất; mật khẩu chỉ lưu dạng băm (salt:hash). */
@Entity(tableName = "parents", indices = {@Index(value = {"email"}, unique = true)})
public class ParentEntity {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    public String fullName;
    public String email;
    public String passwordHash;
    public String pinHash;       // mã PIN phụ huynh (đã băm) để thoát chế độ trẻ em / mở khóa
    public String firebaseUid;   // uid trên Firebase Auth (null với tài khoản cũ chỉ có trong máy)
    public long createdAt = System.currentTimeMillis();

    public ParentEntity(String fullName, String email, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
