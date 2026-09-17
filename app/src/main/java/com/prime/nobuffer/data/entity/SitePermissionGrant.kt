package com.prime.nobuffer.data.entity

import androidx.room.Entity

enum class PermissionType { CAMERA, MICROPHONE, LOCATION }

/** Time-limited permission grant for a host (Phase 21) — replaces re-prompting until expiresAt. */
@Entity(tableName = "site_permission_grants", primaryKeys = ["host", "permissionType"])
data class SitePermissionGrant(
    val host: String,
    val permissionType: PermissionType,
    val grantedAt: Long,
    val expiresAt: Long
)
