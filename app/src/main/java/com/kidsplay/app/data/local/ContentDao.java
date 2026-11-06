package com.kidsplay.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.kidsplay.app.data.local.entities.ContentEntity;

import java.util.List;

@Dao
public interface ContentDao {
    @Insert
    long insert(ContentEntity content);

    @Delete
    void delete(ContentEntity content);

    @Query("UPDATE contents SET published = :published WHERE id = :id")
    void setPublished(long id, boolean published);

    @Query("UPDATE contents SET cloudId = :cloudId WHERE id = :id")
    void setCloudId(long id, String cloudId);

    @Query("SELECT cloudId FROM contents WHERE id = :id")
    String getCloudId(long id);

    @Query("SELECT * FROM contents WHERE parentId = :parentId AND cloudId IS NULL")
    List<ContentEntity> getPendingCloud(long parentId);

    @Query("SELECT * FROM contents WHERE cloudId = :cloudId AND parentId = :parentId LIMIT 1")
    ContentEntity findByCloudId(String cloudId, long parentId);

    @Query("SELECT * FROM contents WHERE parentId = :parentId ORDER BY createdAt DESC")
    LiveData<List<ContentEntity>> getAll(long parentId);

    @Query("SELECT * FROM contents WHERE minAge <= :age AND maxAge >= :age AND published = 1 AND parentId = :parentId ORDER BY createdAt DESC")
    LiveData<List<ContentEntity>> getForAge(int age, long parentId);
}
