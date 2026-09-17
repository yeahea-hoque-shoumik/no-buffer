package com.prime.nobuffer.shields

/** Bundles the Phase 17-19 lambdas [BrowserWebViewClient] needs, so call sites don't thread a dozen params by hand. */
data class WebShieldsContext(
    val effectiveShields: (host: String?) -> EffectiveShields,
    val isHostBlocked: (host: String?) -> Boolean,
    val cosmeticSelectors: (host: String?) -> List<String>,
    val navigationHeaders: () -> Map<String, String>,
    val httpsUpgradeEnabled: Boolean = true,
    val trackingParamStrippingEnabled: Boolean = true,
    val redirectorUnwrapEnabled: Boolean = true,
    val deAmpEnabled: Boolean = true
) {
    companion object {
        fun disabled() = WebShieldsContext(
            effectiveShields = { EffectiveShields.allEnabled() },
            isHostBlocked = { false },
            cosmeticSelectors = { emptyList() },
            navigationHeaders = { emptyMap() },
            httpsUpgradeEnabled = false,
            trackingParamStrippingEnabled = false,
            redirectorUnwrapEnabled = false,
            deAmpEnabled = false
        )
    }
}
