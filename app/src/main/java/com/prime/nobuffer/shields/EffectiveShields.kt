package com.prime.nobuffer.shields

/** Resolved shield settings for one host — combines global defaults with any per-site override. */
data class EffectiveShields(
    val adBlockEnabled: Boolean,
    val trackerBlockEnabled: Boolean,
    val scriptsEnabled: Boolean,
    val fingerprintProtectionEnabled: Boolean
) {
    companion object {
        fun allEnabled() = EffectiveShields(
            adBlockEnabled = false,
            trackerBlockEnabled = false,
            scriptsEnabled = true,
            fingerprintProtectionEnabled = false
        )
    }
}
