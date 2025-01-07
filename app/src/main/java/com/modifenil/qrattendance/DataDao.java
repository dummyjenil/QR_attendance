package com.modifenil.qrattendance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataDao {
    @Insert
    void insert(DataEntity vectorStoreEntity);

    @Query("SELECT * FROM data_store")
    List<DataEntity> getAllData();
}
