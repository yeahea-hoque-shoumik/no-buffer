package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.SiteCosmeticRule
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteCosmeticRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SiteCosmeticRule)

    @Query("DELETE FROM site_cosmetic_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM site_cosmetic_rules WHERE host = :host")
    suspend fun findByHost(host: String): List<SiteCosmeticRule>

    @Query("SELECT * FROM site_cosmetic_rules")
    fun observeAll(): Flow<List<SiteCosmeticRule>>
}
