package com.prime.nobuffer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CustomFilterRuleType { DOMAIN, COSMETIC }

/** Rule imported from a user-supplied basic-ABP-subset filter list (Phase 17). */
@Entity(tableName = "custom_filter_rules")
data class CustomFilterRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CustomFilterRuleType,
    val domain: String,
    val selector: String? = null
)
