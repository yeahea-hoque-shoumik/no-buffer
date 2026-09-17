package com.prime.nobuffer.shields

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Phase 17 — network-level ad/tracker blocking. Loads a curated static host
 * list from assets and matches a request host against it (exact or any parent
 * domain), mirroring the existing video-URL blocklist pattern in
 * [com.prime.nobuffer.browser.BrowserWebViewClient].
 */
class AdTrackerBlocklist(context: Context) {

    private val blockedHosts: HashSet<String> = HashSet()

    private val _blockedCount = MutableStateFlow(0)
    val blockedCount: StateFlow<Int> = _blockedCount

    init {
        context.assets.open(ASSET_PATH).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    blockedHosts.add(trimmed.lowercase())
                }
            }
        }
    }

    fun isBlocked(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val lower = host.lowercase()
        if (blockedHosts.contains(lower)) return true
        var idx = lower.indexOf('.')
        while (idx != -1) {
            if (blockedHosts.contains(lower.substring(idx + 1))) return true
            idx = lower.indexOf('.', idx + 1)
        }
        return false
    }

    fun recordBlocked() {
        _blockedCount.update { it + 1 }
    }

    fun resetCount() {
        _blockedCount.value = 0
    }

    companion object {
        private const val ASSET_PATH = "blocklists/ad_tracker_hosts.txt"
    }
}
