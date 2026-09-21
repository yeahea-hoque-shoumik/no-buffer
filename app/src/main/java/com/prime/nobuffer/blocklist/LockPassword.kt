package com.prime.nobuffer.blocklist

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Lock-password policy + PBKDF2 storage. The plaintext is never persisted — only a random salt
 * and the derived hash. [matches] uses a constant-time compare.
 */
object LockPassword {
    const val MIN_LENGTH = 20
    const val MAX_LENGTH = 128
    const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val PBKDF2 = "PBKDF2WithHmacSHA256"

    data class PolicyCheck(
        val minLength: Boolean,
        val maxLength: Boolean,
        val upper: Boolean,
        val lower: Boolean,
        val digit: Boolean,
        val special: Boolean,
        val noWhitespace: Boolean
    ) {
        val isValid: Boolean
            get() = minLength && maxLength && upper && lower && digit && special && noWhitespace

        fun missingHints(): List<String> = buildList {
            if (!minLength) add("At least $MIN_LENGTH characters")
            if (!maxLength) add("At most $MAX_LENGTH characters")
            if (!upper) add("One uppercase letter")
            if (!lower) add("One lowercase letter")
            if (!digit) add("One number")
            if (!special) add("One symbol (e.g. !@#\$%)")
            if (!noWhitespace) add("No spaces")
        }
    }

    fun evaluate(password: String): PolicyCheck = PolicyCheck(
        minLength = password.length >= MIN_LENGTH,
        maxLength = password.length <= MAX_LENGTH,
        upper = password.any { it.isUpperCase() },
        lower = password.any { it.isLowerCase() },
        digit = password.any { it.isDigit() },
        special = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
        noWhitespace = password.none { it.isWhitespace() }
    )

    data class StoredSecret(
        val saltB64: String,
        val hashB64: String,
        val iterations: Int
    )

    fun create(password: String, random: SecureRandom = SecureRandom()): StoredSecret {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        return StoredSecret(
            saltB64 = encode(salt),
            hashB64 = encode(hash),
            iterations = ITERATIONS
        )
    }

    fun matches(password: String, stored: StoredSecret): Boolean {
        val salt = decode(stored.saltB64) ?: return false
        val expected = decode(stored.hashB64) ?: return false
        val actual = pbkdf2(password, salt, stored.iterations.coerceAtLeast(1))
        return MessageDigest.isEqual(expected, actual)
    }

    /**
     * After [threshold] failures, lockout doubles from 30s each extra failure, capped at 1 hour.
     * Attempt 5 → 30s, 6 → 60s, 7 → 2m, … attempt 12+ → 1h.
     */
    fun lockoutMillis(failedAttempts: Int, threshold: Int = 5): Long {
        if (failedAttempts < threshold) return 0L
        val shift = (failedAttempts - threshold).coerceAtMost(7)
        return 30_000L * (1L shl shift)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray? = runCatching {
        Base64.getDecoder().decode(value)
    }.getOrNull()
}
