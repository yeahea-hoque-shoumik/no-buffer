package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.TabEntity

@Dao
interface TabDao {

    @Query("SELECT * FROM tabs ORDER BY position ASC")
    suspend fun getAll(): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabEntity>)

    @Query("DELETE FROM tabs")
    suspend fun clearAll()
}
