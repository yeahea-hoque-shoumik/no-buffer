package com.prime.nobuffer.shields

/**
 * Curated CSS selectors for known ad-container / social-embed-placeholder classes and ids,
 * applied via `display:none` on top of the network-level blocking in [AdTrackerBlocklist]
 * (Phase 17 cosmetic filtering).
 */
object CosmeticSelectors {
    val GLOBAL: List<String> = listOf(
        // generic ad-slot conventions
        "[id^=\"google_ads_\"]", "[id^=\"div-gpt-ad\"]", "ins.adsbygoogle",
        ".ad-container", ".ad-slot", ".ad-wrapper", ".ads-container",
        "[class*=\"-ad-\"]", "[id*=\"-ad-\"]",
        // social embed placeholders
        ".fb-like", ".fb-share-button", ".fb-page", "div[class*=\"twitter-tweet\"]",
        ".linkedin-share-button", ".addthis_toolbox"
    )

    fun buildHideJs(selectors: List<String>): String {
        if (selectors.isEmpty()) return ""
        val selectorList = selectors.joinToString(",") { it.replace("\\", "\\\\").replace("'", "\\'") }
        return """
            (function() {
                var selectors = '$selectorList';
                function hide() {
                    try {
                        document.querySelectorAll(selectors).forEach(function(el) { el.style.setProperty('display', 'none', 'important'); });
                    } catch (e) {}
                }
                hide();
                var observer = new MutationObserver(hide);
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
            })();
        """.trimIndent()
    }
}
