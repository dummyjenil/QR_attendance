package com.modifenil.qrattendance;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "data_store")
public class DataEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private final String mobile;
    private final String name;
    private final long timestamp;

    // Constructor
    public DataEntity(String mobile, String name, long timestamp) {
        this.mobile = mobile;
        this.name = name;
        this.timestamp = timestamp;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public String getMobile() {
        return mobile;
    }
    public long getTimestamp() {
        return timestamp;
    }

}
