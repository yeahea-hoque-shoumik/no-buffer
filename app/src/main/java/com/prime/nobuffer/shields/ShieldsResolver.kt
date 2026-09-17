package com.prime.nobuffer.shields

import com.prime.nobuffer.data.BrowserRepository
import com.prime.nobuffer.data.entity.SiteShieldOverride
import com.prime.nobuffer.settings.BrowserSettings
import com.prime.nobuffer.settings.SettingsRepository
import com.prime.nobuffer.settings.ShieldsMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Single source of truth for "what should this host be allowed to do", layering a per-site
 * [SiteShieldOverride] on top of the global [ShieldsMode]/settings default (Phase 20).
 *
 * Reads from [android.webkit.WebViewClient] callbacks must be synchronous and non-blocking, so
 * both settings and overrides are mirrored into plain in-memory snapshots kept fresh by
 * background collectors, rather than queried from Room/DataStore on the hot path.
 */
class ShieldsResolver(
    settingsRepository: SettingsRepository,
    private val repository: BrowserRepository,
    scope: CoroutineScope
) {
    @Volatile private var settingsSnapshot: BrowserSettings = BrowserSettings()
    @Volatile private var overridesByHost: Map<String, SiteShieldOverride> = emptyMap()

    init {
        settingsRepository.settings.onEach { settingsSnapshot = it }.launchIn(scope)
        repository.observeSiteShieldOverrides()
            .onEach { list -> overridesByHost = list.associateBy { it.host } }
            .launchIn(scope)
    }

    fun effectiveShields(host: String?): EffectiveShields {
        val override = overridesByHost[host?.lowercase()]
        val settings = settingsSnapshot
        // AGGRESSIVE forces every shield on regardless of the individual toggles; STANDARD respects
        // them; DISABLED forces everything off except scripts (a broken page, not a privacy control).
        return EffectiveShields(
            adBlockEnabled = override?.adBlock ?: when (settings.shieldsMode) {
                ShieldsMode.DISABLED -> false
                ShieldsMode.AGGRESSIVE -> true
                ShieldsMode.STANDARD -> settings.adBlockerEnabled
            },
            trackerBlockEnabled = override?.trackerBlock ?: (settings.shieldsMode != ShieldsMode.DISABLED),
            scriptsEnabled = override?.scriptsEnabled ?: settings.javaScriptEnabled,
            fingerprintProtectionEnabled = override?.fingerprintProtection ?: when (settings.shieldsMode) {
                ShieldsMode.DISABLED -> false
                ShieldsMode.AGGRESSIVE -> true
                ShieldsMode.STANDARD -> settings.antiFingerprintingEnabled
            }
        )
    }

    /** Replaces the full override row for [host] — each field is tri-state: null means "inherit global default". */
    suspend fun setOverride(
        host: String,
        adBlock: Boolean?,
        trackerBlock: Boolean?,
        scriptsEnabled: Boolean?,
        fingerprintProtection: Boolean?
    ) {
        if (adBlock == null && trackerBlock == null && scriptsEnabled == null && fingerprintProtection == null) {
            clearOverride(host)
            return
        }
        repository.upsertSiteShieldOverride(
            SiteShieldOverride(
                host = host.lowercase(),
                adBlock = adBlock,
                trackerBlock = trackerBlock,
                scriptsEnabled = scriptsEnabled,
                fingerprintProtection = fingerprintProtection
            )
        )
    }

    suspend fun clearOverride(host: String) = repository.deleteSiteShieldOverride(host.lowercase())

    fun currentOverride(host: String): SiteShieldOverride? = overridesByHost[host.lowercase()]
}
