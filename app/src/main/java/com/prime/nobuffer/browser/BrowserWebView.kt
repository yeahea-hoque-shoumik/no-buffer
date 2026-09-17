package com.prime.nobuffer.browser

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.prime.nobuffer.shields.FingerprintProtectionJs
import kotlin.random.Random

@SuppressLint("SetJavaScriptEnabled")
class BrowserWebView(context: Context) : WebView(context) {

    private var fingerprintScriptHandler: ScriptHandler? = null
    private val fingerprintSeed = Random.nextInt(1, Int.MAX_VALUE)

    private var elementPickerCallback: ((String) -> Unit)? = null

    private inner class ElementPickerBridge {
        @JavascriptInterface
        fun onSelectorPicked(selector: String) {
            post {
                val callback = elementPickerCallback
                elementPickerCallback = null
                evaluateJavascript(ELEMENT_PICKER_EXIT_JS, null)
                callback?.invoke(selector)
            }
        }
    }

    init {
        addJavascriptInterface(ElementPickerBridge(), "NoBufferElementPicker")
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            userAgentString = CHROME_UA
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_AUTO)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
        }

        WebView.startSafeBrowsing(context, null)
    }

    /** Phase 18 — toggles the document-start anti-fingerprinting script for this WebView instance. */
    fun setFingerprintProtectionEnabled(enabled: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        if (enabled) {
            if (fingerprintScriptHandler == null) {
                fingerprintScriptHandler = WebViewCompat.addDocumentStartJavaScript(
                    this, FingerprintProtectionJs.script(fingerprintSeed), setOf("*")
                )
            }
        } else {
            fingerprintScriptHandler?.remove()
            fingerprintScriptHandler = null
        }
    }

    /** Phase 17 — enters tap-to-block picker mode; [onSelectorPicked] fires once with the tapped element's CSS selector. */
    fun startElementPicker(onSelectorPicked: (String) -> Unit) {
        elementPickerCallback = onSelectorPicked
        evaluateJavascript(ELEMENT_PICKER_ENTER_JS, null)
    }

    fun stopElementPicker() {
        elementPickerCallback = null
        evaluateJavascript(ELEMENT_PICKER_EXIT_JS, null)
    }

    companion object {
        const val CHROME_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

        private const val ELEMENT_PICKER_ENTER_JS = """
            (function() {
                if (window.__nbPickerActive) return;
                window.__nbPickerActive = true;
                function computeSelector(el) {
                    if (el.id) return '#' + el.id;
                    var path = [];
                    var node = el;
                    while (node && node.nodeType === 1 && path.length < 5) {
                        var selector = node.tagName.toLowerCase();
                        if (node.className && typeof node.className === 'string' && node.className.trim()) {
                            selector += '.' + node.className.trim().split(/\s+/).slice(0, 2).join('.');
                        }
                        path.unshift(selector);
                        node = node.parentElement;
                    }
                    return path.join(' > ');
                }
                window.__nbPickerTap = function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    var selector = computeSelector(e.target);
                    if (window.NoBufferElementPicker) window.NoBufferElementPicker.onSelectorPicked(selector);
                };
                document.addEventListener('click', window.__nbPickerTap, true);
            })();
        """

        private const val ELEMENT_PICKER_EXIT_JS = """
            (function() {
                window.__nbPickerActive = false;
                if (window.__nbPickerTap) {
                    document.removeEventListener('click', window.__nbPickerTap, true);
                    window.__nbPickerTap = null;
                }
            })();
        """
    }
}
