package com.kidsplay.app.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.kidsplay.app.data.local.entities.ParentEntity;

@Dao
public interface ParentDao {
    @Insert
    long insert(ParentEntity parent);

    @Query("SELECT * FROM parents WHERE email = :email LIMIT 1")
    ParentEntity findByEmail(String email);

    @Update
    void update(ParentEntity parent);

    @Query("SELECT pinHash FROM parents WHERE id = :id")
    String getPinHash(long id);

    @Query("UPDATE parents SET pinHash = :hash WHERE id = :id")
    void setPinHash(long id, String hash);

    @Query("UPDATE parents SET fullName = :name WHERE id = :id")
    void updateName(long id, String name);

    @Query("SELECT * FROM parents WHERE id = :id LIMIT 1")
    ParentEntity findById(long id);
}
