package com.kidsplay.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.kidsplay.app.utils.AgeUtils;

@Entity(tableName = "kid_profiles")
public class KidProfileEntity {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    public long parentId;   // phụ huynh sở hữu hồ sơ này
    public String name;
    public int age;
    public String allowedCategories;
    public int dailyLimitMinutes;
    public int usedMinutesToday = 0;
    public String parentPinHash;
    public String lastResetDate;
    public String birthDate;    // dd/MM/yyyy
    public String gender;       // Nam / Nữ / Khác
    public String avatarPath;   // ảnh đại diện lưu trong bộ nhớ riêng của app
    public String interests;    // sở thích, ngăn cách bằng dấu phẩy

    public KidProfileEntity(String name, int age, String allowedCategories, int dailyLimitMinutes, int usedMinutesToday, String parentPinHash, String lastResetDate) {
        this.name = name;
        this.age = age;
        this.allowedCategories = allowedCategories;
        this.dailyLimitMinutes = dailyLimitMinutes;
        this.usedMinutesToday = usedMinutesToday;
        this.parentPinHash = parentPinHash;
        this.lastResetDate = lastResetDate;
    }

    /** Tuổi hiện tại: tính theo ngày sinh nếu có (tự tăng theo thời gian), không thì dùng tuổi đã lưu. */
    public int currentAge() {
        int fromBirth = AgeUtils.ageOf(birthDate);
        return fromBirth >= 0 ? fromBirth : age;
    }
}
