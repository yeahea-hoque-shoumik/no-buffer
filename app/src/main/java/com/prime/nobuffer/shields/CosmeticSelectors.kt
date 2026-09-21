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

    val COOKIE_BANNERS: List<String> = listOf(
        "#cookie-banner", ".cookie-banner", ".cc-window", ".cc-banner",
        "#onetrust-banner-sdk", "#onetrust-consent-sdk", ".ot-sdk-container",
        ".qc-cmp2-container", "#qc-cmp2-container",
        "#CybotCookiebotDialog", "#CybotCookiebotDialogBodyUnderlay",
        "[id*=\"cookie-consent\"]", "[class*=\"cookie-consent\"]",
        "[id*=\"cookieConsent\"]", "#cookieConsent", ".cookie-consent",
        "[id*=\"CookieBanner\"]", "[class*=\"CookieBanner\"]",
        "#cookie-notice", ".cookie-notice", "#cookieNotice",
        "#consent-banner", ".consent-banner", "#gdpr-banner", ".gdpr-banner",
        "#didomi-host", ".didomi-popup-container",
        "#usercentrics-root", ".osano-cm-window",
        "#iubenda-cs-banner", ".iubenda-cs-container",
        "#tarteaucitronRoot", "#truste-consent-track",
        "#cookie-law-info-bar", "#moove_gdpr_cookie_info_bar",
        "#eu-cookie-law", ".eu-cookie-compliance-banner",
        "#cookiescript_injected", ".sp-message-container",
        "#cmp-app-container", ".fc-consent-root",
        ".js-cookie-banner"
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
