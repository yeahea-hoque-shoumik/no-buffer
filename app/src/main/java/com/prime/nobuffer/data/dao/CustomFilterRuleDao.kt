package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.CustomFilterRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFilterRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<CustomFilterRule>)

    @Query("DELETE FROM custom_filter_rules")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM custom_filter_rules")
    suspend fun count(): Int

    @Query("SELECT * FROM custom_filter_rules")
    fun observeAll(): Flow<List<CustomFilterRule>>
}
