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

    @Query("SELECT * FROM blocked_sites ORDER BY createdAt DESC")
    suspend fun getAll(): List<BlockedSite>

    @Query("SELECT * FROM blocked_sites WHERE host = :host LIMIT 1")
    suspend fun findByHost(host: String): BlockedSite?

    @Query("SELECT host FROM blocked_sites")
    suspend fun getHosts(): List<String>

    @Query("UPDATE blocked_sites SET unlockUntil = :unlockUntil WHERE host = :host")
    suspend fun updateUnlockUntil(host: String, unlockUntil: Long)

    @Query(
        "UPDATE blocked_sites SET scheduleStartMinute = :startMinute, " +
            "scheduleEndMinute = :endMinute, scheduleDaysMask = :daysMask WHERE host = :host"
    )
    suspend fun updateSchedule(host: String, startMinute: Int, endMinute: Int, daysMask: Int)

    @Query(
        "UPDATE blocked_sites SET dailyBudgetMinutes = :minutes, usedMillisToday = :usedMillisToday, " +
            "budgetDayEpoch = :budgetDayEpoch WHERE host = :host"
    )
    suspend fun updateDailyBudget(
        host: String,
        minutes: Int,
        usedMillisToday: Long,
        budgetDayEpoch: Long
    )

    @Query("UPDATE blocked_sites SET videoAllowed = :allowed WHERE host = :host")
    suspend fun updateVideoAllowed(host: String, allowed: Boolean)

    @Query(
        "UPDATE blocked_sites SET usedMillisToday = :usedMillisToday, budgetDayEpoch = :budgetDayEpoch, " +
            "unlockUntil = :unlockUntil WHERE host = :host"
    )
    suspend fun updateUsage(
        host: String,
        usedMillisToday: Long,
        budgetDayEpoch: Long,
        unlockUntil: Long
    )
}
