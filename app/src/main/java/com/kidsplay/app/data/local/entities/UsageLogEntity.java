package com.kidsplay.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usage_logs")
public class UsageLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;
    public long kidId;
    public String eventType;
    public long timestamp = System.currentTimeMillis();

    public UsageLogEntity(long kidId, String eventType) {
        this.kidId = kidId;
        this.eventType = eventType;
    }
}
