package com.prime.nobuffer.blocklist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.lockPasswordDataStore by preferencesDataStore(name = "site_lock_password")

sealed class PasswordVerifyResult {
    data object Ok : PasswordVerifyResult()
    data object NoPassword : PasswordVerifyResult()
    data class Wrong(val failedAttempts: Int, val lockoutMillis: Long) : PasswordVerifyResult()
    data class Locked(val remainingMillis: Long) : PasswordVerifyResult()
}

class LockPasswordStore(private val context: Context) {

    private object Keys {
        val SALT = stringPreferencesKey("salt")
        val HASH = stringPreferencesKey("hash")
        val ITERATIONS = intPreferencesKey("iterations")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
    }

    val hasPassword: Flow<Boolean> = context.lockPasswordDataStore.data.map { prefs ->
        !prefs[Keys.HASH].isNullOrBlank() && !prefs[Keys.SALT].isNullOrBlank()
    }

    suspend fun hasPasswordNow(): Boolean {
        val prefs = context.lockPasswordDataStore.data.first()
        return !prefs[Keys.HASH].isNullOrBlank() && !prefs[Keys.SALT].isNullOrBlank()
    }

    suspend fun setPassword(password: String): String? {
        val policy = LockPassword.evaluate(password)
        if (!policy.isValid) return policy.missingHints().joinToString(" · ")
        val stored = LockPassword.create(password)
        context.lockPasswordDataStore.edit { prefs ->
            prefs[Keys.SALT] = stored.saltB64
            prefs[Keys.HASH] = stored.hashB64
            prefs[Keys.ITERATIONS] = stored.iterations
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs.remove(Keys.LOCKOUT_UNTIL)
        }
        return null
    }

    suspend fun changePassword(current: String, next: String): String? {
        when (val result = verify(current)) {
            PasswordVerifyResult.Ok -> Unit
            PasswordVerifyResult.NoPassword -> return "No lock password is set"
            is PasswordVerifyResult.Wrong -> return "Current password is incorrect"
            is PasswordVerifyResult.Locked -> return lockoutMessage(result.remainingMillis)
        }
        return setPassword(next)
    }

    suspend fun verify(password: String): PasswordVerifyResult {
        val prefs = context.lockPasswordDataStore.data.first()
        val salt = prefs[Keys.SALT]
        val hash = prefs[Keys.HASH]
        val iterations = prefs[Keys.ITERATIONS] ?: LockPassword.ITERATIONS
        if (salt.isNullOrBlank() || hash.isNullOrBlank()) return PasswordVerifyResult.NoPassword

        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        if (now < lockoutUntil) {
            return PasswordVerifyResult.Locked(lockoutUntil - now)
        }

        val stored = LockPassword.StoredSecret(salt, hash, iterations)
        if (LockPassword.matches(password, stored)) {
            context.lockPasswordDataStore.edit {
                it[Keys.FAILED_ATTEMPTS] = 0
                it.remove(Keys.LOCKOUT_UNTIL)
            }
            return PasswordVerifyResult.Ok
        }

        val attempts = (prefs[Keys.FAILED_ATTEMPTS] ?: 0) + 1
        val lockout = LockPassword.lockoutMillis(attempts)
        context.lockPasswordDataStore.edit {
            it[Keys.FAILED_ATTEMPTS] = attempts
            if (lockout > 0) it[Keys.LOCKOUT_UNTIL] = now + lockout
        }
        return PasswordVerifyResult.Wrong(failedAttempts = attempts, lockoutMillis = lockout)
    }

    companion object {
        fun lockoutMessage(remainingMillis: Long): String {
            val seconds = (remainingMillis / 1000L).coerceAtLeast(1L)
            return if (seconds < 60) {
                "Too many attempts. Try again in ${seconds}s"
            } else {
                val minutes = (seconds + 59) / 60
                "Too many attempts. Try again in ${minutes}m"
            }
        }
    }
}
