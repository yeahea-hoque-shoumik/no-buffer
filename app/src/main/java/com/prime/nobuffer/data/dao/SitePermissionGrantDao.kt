package com.prime.nobuffer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prime.nobuffer.data.entity.PermissionType
import com.prime.nobuffer.data.entity.SitePermissionGrant

@Dao
interface SitePermissionGrantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(grant: SitePermissionGrant)

    @Query("SELECT * FROM site_permission_grants WHERE host = :host AND permissionType = :type AND expiresAt > :now LIMIT 1")
    suspend fun findActive(host: String, type: PermissionType, now: Long): SitePermissionGrant?

    @Query("DELETE FROM site_permission_grants WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM site_permission_grants WHERE host = :host")
    suspend fun deleteForHost(host: String)
}
