package com.prime.nobuffer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_sites")
data class BlockedSite(
    @PrimaryKey val host: String,
    val createdAt: Long = System.currentTimeMillis(),
    val unlockUntil: Long = 0L,
    val dailyBudgetMinutes: Int = 0,
    val usedMillisToday: Long = 0L,
    val budgetDayEpoch: Long = 0L,
    val scheduleStartMinute: Int = -1,
    val scheduleEndMinute: Int = -1,
    val scheduleDaysMask: Int = 0b1111111,
    val videoAllowed: Boolean = false
)
