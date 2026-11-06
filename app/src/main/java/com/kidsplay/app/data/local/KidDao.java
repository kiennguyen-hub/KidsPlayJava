package com.kidsplay.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.kidsplay.app.data.local.entities.KidProfileEntity;
import com.kidsplay.app.data.local.entities.UsageLogEntity;

import java.util.List;

@Dao
public interface KidDao {
    @Query("SELECT * FROM kid_profiles WHERE id = :kidId LIMIT 1")
    KidProfileEntity getKidById(long kidId);

    @Query("SELECT * FROM kid_profiles WHERE id = :kidId LIMIT 1")
    LiveData<KidProfileEntity> getKidLive(long kidId);

    @Query("SELECT * FROM kid_profiles WHERE parentId = :parentId ORDER BY name")
    LiveData<List<KidProfileEntity>> getKidsByParent(long parentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertKid(KidProfileEntity kid);

    @Update
    void updateKid(KidProfileEntity kid);

    @Delete
    void deleteKid(KidProfileEntity kid);

    @Query("DELETE FROM usage_logs WHERE kidId = :kidId")
    void deleteLogsForKid(long kidId);

    @Query("SELECT COUNT(*) FROM usage_logs WHERE kidId = :kidId AND eventType = :type")
    int countLogs(long kidId, String type);

    @Insert
    void insertLog(UsageLogEntity log);

    @Query("SELECT * FROM usage_logs WHERE kidId = :kidId ORDER BY timestamp DESC")
    LiveData<List<UsageLogEntity>> getLogsForKid(long kidId);
}
