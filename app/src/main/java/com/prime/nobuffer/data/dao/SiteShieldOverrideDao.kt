package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.SiteShieldOverride
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteShieldOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: SiteShieldOverride)

    @Query("SELECT * FROM site_shield_overrides WHERE host = :host")
    suspend fun findByHost(host: String): SiteShieldOverride?

    @Query("DELETE FROM site_shield_overrides WHERE host = :host")
    suspend fun delete(host: String)

    @Query("SELECT * FROM site_shield_overrides")
    fun observeAll(): Flow<List<SiteShieldOverride>>
}
