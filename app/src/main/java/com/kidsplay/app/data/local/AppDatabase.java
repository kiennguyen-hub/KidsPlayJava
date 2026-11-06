package com.kidsplay.app.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.kidsplay.app.data.local.entities.ContentEntity;
import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.local.entities.ParentEntity;
import com.kidsplay.app.data.local.entities.UsageLogEntity;

@Database(entities = {KidProfileEntity.class, UsageLogEntity.class, ContentEntity.class, ParentEntity.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract KidDao kidDao();
    public abstract ContentDao contentDao();
    public abstract ParentDao parentDao();

    /** v3 -> v4: thêm cột số trang cho truyện tranh, giữ nguyên tài khoản và hồ sơ đã có. */
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE contents ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 0");
        }
    };

    /** v4 -> v5: thêm uid Firebase cho tài khoản và id đồng bộ cloud cho nội dung. */
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE parents ADD COLUMN firebaseUid TEXT");
            db.execSQL("ALTER TABLE contents ADD COLUMN cloudId TEXT");
        }
    };

    /** v5 -> v6: giao diện mới (hồ sơ bé chi tiết, PIN theo tài khoản, thông tin truyện). */
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE kid_profiles ADD COLUMN birthDate TEXT");
            db.execSQL("ALTER TABLE kid_profiles ADD COLUMN gender TEXT");
            db.execSQL("ALTER TABLE kid_profiles ADD COLUMN avatarPath TEXT");
            db.execSQL("ALTER TABLE kid_profiles ADD COLUMN interests TEXT");
            db.execSQL("ALTER TABLE parents ADD COLUMN pinHash TEXT");
            db.execSQL("ALTER TABLE contents ADD COLUMN description TEXT");
            db.execSQL("ALTER TABLE contents ADD COLUMN category TEXT");
            db.execSQL("ALTER TABLE contents ADD COLUMN maxAge INTEGER NOT NULL DEFAULT 99");
            db.execSQL("ALTER TABLE contents ADD COLUMN published INTEGER NOT NULL DEFAULT 1");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "kidsplay_db")
                            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
