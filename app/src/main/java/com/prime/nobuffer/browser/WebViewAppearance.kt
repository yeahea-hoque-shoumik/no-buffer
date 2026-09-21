package com.prime.nobuffer.browser

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/** Force-dark / algorithmic-darkening toggles. No-op when the WebView feature is absent. */
object WebViewAppearance {

    fun applyForceDark(webView: WebView, enabled: Boolean) {
        applyForceDark(webView.settings, enabled)
    }

    fun applyForceDark(settings: WebSettings, enabled: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(
                settings,
                if (enabled) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, enabled)
        }
    }
}
