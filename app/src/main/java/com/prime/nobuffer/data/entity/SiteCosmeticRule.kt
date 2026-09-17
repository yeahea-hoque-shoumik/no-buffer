package com.prime.nobuffer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Element-picker-generated cosmetic hide rule for one host (Phase 17). */
@Entity(tableName = "site_cosmetic_rules")
data class SiteCosmeticRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val selector: String
)
