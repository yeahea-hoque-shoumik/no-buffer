package com.prime.nobuffer.blocklist

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock")

class AppLockStore(context: Context) {
    private val dataStore = context.applicationContext.appLockDataStore

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val LAST_UNLOCK_AT = longPreferencesKey("last_unlock_at")
        val LOCK_TIMEOUT_MILLIS = longPreferencesKey("lock_timeout_millis")
    }

    val enabled: Flow<Boolean> = dataStore.data.map { it[Keys.ENABLED] == true }

    val lastUnlockAt: Flow<Long> = dataStore.data.map { it[Keys.LAST_UNLOCK_AT] ?: 0L }

    /** 0 = lock every cold start / onStop (no grace window). */
    val lockTimeoutMillis: Flow<Long> = dataStore.data.map { it[Keys.LOCK_TIMEOUT_MILLIS] ?: 0L }

    suspend fun isEnabledNow(): Boolean = dataStore.data.first()[Keys.ENABLED] == true

    suspend fun lastUnlockAtNow(): Long = dataStore.data.first()[Keys.LAST_UNLOCK_AT] ?: 0L

    suspend fun lockTimeoutMillisNow(): Long = dataStore.data.first()[Keys.LOCK_TIMEOUT_MILLIS] ?: 0L

    suspend fun setEnabled(value: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setLastUnlockAt(millis: Long) {
        dataStore.edit { it[Keys.LAST_UNLOCK_AT] = millis }
    }

    suspend fun setLockTimeoutMillis(millis: Long) {
        dataStore.edit { it[Keys.LOCK_TIMEOUT_MILLIS] = millis.coerceAtLeast(0L) }
    }
}
