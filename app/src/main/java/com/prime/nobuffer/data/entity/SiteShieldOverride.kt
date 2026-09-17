package com.prime.nobuffer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-site shield overrides — null fields inherit the global default (Phase 20). */
@Entity(tableName = "site_shield_overrides")
data class SiteShieldOverride(
    @PrimaryKey val host: String,
    val adBlock: Boolean? = null,
    val trackerBlock: Boolean? = null,
    val scriptsEnabled: Boolean? = null,
    val fingerprintProtection: Boolean? = null
)
