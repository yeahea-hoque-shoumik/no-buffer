package com.prime.nobuffer.shields

import android.webkit.WebResourceRequest

/** Bundles the Phase 17-19 lambdas [BrowserWebViewClient] needs, so call sites don't thread a dozen params by hand. */
data class WebShieldsContext(
    val effectiveShields: (host: String?) -> EffectiveShields,
    val isHostBlocked: (host: String?) -> Boolean,
    /** Filter-list match: (request, firstPartyHost, adsEnabled, trackersEnabled). */
    val shouldBlockRequest: (request: WebResourceRequest, firstPartyHost: String?, ads: Boolean, trackers: Boolean) -> Boolean,
    /** CSS injected at document start for a host (empty when ad blocking is off for it). */
    val earlyCosmeticCss: (host: String) -> String,
    val cosmeticSelectors: (host: String?) -> List<String>,
    val navigationHeaders: () -> Map<String, String>,
    val httpsUpgradeEnabled: Boolean = true,
    val trackingParamStrippingEnabled: Boolean = true,
    val redirectorUnwrapEnabled: Boolean = true,
    val deAmpEnabled: Boolean = true,
    val isVideoAllowed: (host: String?) -> Boolean = { false }
) {
    companion object {
        fun disabled() = WebShieldsContext(
            effectiveShields = { EffectiveShields.allDisabled() },
            isHostBlocked = { false },
            shouldBlockRequest = { _, _, _, _ -> false },
            earlyCosmeticCss = { "" },
            cosmeticSelectors = { emptyList() },
            navigationHeaders = { emptyMap() },
            httpsUpgradeEnabled = false,
            trackingParamStrippingEnabled = false,
            redirectorUnwrapEnabled = false,
            deAmpEnabled = false
        )
    }
}
