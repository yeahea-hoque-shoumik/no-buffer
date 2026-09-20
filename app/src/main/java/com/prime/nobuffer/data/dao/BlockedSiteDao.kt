package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.BlockedSite
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedSiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: BlockedSite)

    @Query("DELETE FROM blocked_sites WHERE host = :host")
    suspend fun delete(host: String)

    @Query("DELETE FROM blocked_sites")
    suspend fun clearAll()

    @Query("SELECT * FROM blocked_sites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlockedSite>>

    @Query("SELECT host FROM blocked_sites")
    suspend fun getHosts(): List<String>
}
