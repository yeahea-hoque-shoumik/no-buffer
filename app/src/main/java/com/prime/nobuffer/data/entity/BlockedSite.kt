package com.prime.nobuffer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_sites")
data class BlockedSite(
    @PrimaryKey val host: String,
    val createdAt: Long = System.currentTimeMillis()
)
