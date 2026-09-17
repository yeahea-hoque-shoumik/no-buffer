package com.prime.nobuffer.browser

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.prime.nobuffer.shields.CosmeticSelectors
import com.prime.nobuffer.shields.EffectiveShields
import com.prime.nobuffer.shields.UrlSanitizer

class BrowserWebViewClient(
    private val onPageStarted: (url: String) -> Unit = {},
    private val onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
    private val onProgressChanged: (progress: Int) -> Unit = {},
    private val onSslError: (handler: SslErrorHandler, error: SslError) -> Unit = { handler, _ -> handler.cancel() },
    private val onReceivedError: () -> Unit = {},
    // Phase 17 — content blocking
    private val effectiveShields: (host: String?) -> EffectiveShields = { EffectiveShields.allEnabled() },
    private val isHostBlocked: (String?) -> Boolean = { false },
    private val onRequestBlocked: () -> Unit = {},
    private val cosmeticSelectors: (host: String?) -> List<String> = { emptyList() },
    // Phase 19 — navigation hardening
    private val httpsUpgradeEnabled: Boolean = true,
    private val trackingParamStrippingEnabled: Boolean = true,
    private val redirectorUnwrapEnabled: Boolean = true,
    private val deAmpEnabled: Boolean = true,
    private val onInsecureFallback: (url: String) -> Unit = {},
    private val navigationHeaders: () -> Map<String, String> = { emptyMap() }
) : WebViewClient() {

    private var currentMainFrameHost: String? = null
    private val httpsUpgradeMap = HashMap<String, String>()
    private val upgradeAbandonedHosts = HashSet<String>()

    /** Applies tracking-param stripping, redirector unwrapping and HTTPS upgrading to a URL before it is loaded. */
    fun resolveNavigationUrl(url: String): String {
        var current = url
        if (redirectorUnwrapEnabled) UrlSanitizer.unwrapRedirector(current)?.let { current = it }
        if (trackingParamStrippingEnabled) current = UrlSanitizer.stripTrackingParams(current)
        if (httpsUpgradeEnabled && current.startsWith("http://")) {
            val host = runCatching { Uri.parse(current).host }.getOrNull()
            if (host != null && host !in upgradeAbandonedHosts) {
                val https = "https://" + current.removePrefix("http://")
                httpsUpgradeMap[https] = current
                current = https
            }
        }
        return current
    }

    fun currentNavigationHeaders(): Map<String, String> = navigationHeaders()

    // Layer 1: network-level URL blocking — video by extension/MIME/CDN, plus Phase 17 ad/tracker hosts
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val urlStr = request.url.toString()
        val lower = urlStr.lowercase()
        if (isVideoUrl(lower)) {
            return WebResourceResponse("video/mp4", "UTF-8", null)
        }
        if (!request.isForMainFrame) {
            val shields = effectiveShields(currentMainFrameHost)
            if ((shields.adBlockEnabled || shields.trackerBlockEnabled) && isHostBlocked(request.url.host)) {
                onRequestBlocked()
                return WebResourceResponse("text/plain", "UTF-8", null)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    // Phase 19: re-applied to in-page link taps / JS navigation / server redirects (the initial
    // app-driven loadUrl is sanitized by the caller via resolveNavigationUrl before this fires).
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        val original = request.url.toString()
        val resolved = resolveNavigationUrl(original)
        if (resolved == original) return false
        view.loadUrl(resolved, navigationHeaders())
        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        currentMainFrameHost = runCatching { Uri.parse(url).host }.getOrNull()
        onPageStarted(url)
    }

    // Layer 2: DOM removal + MutationObserver injected after page load, plus Phase 17 cosmetic hides
    // and Phase 19 de-AMP canonical-link redirect.
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        view.evaluateJavascript(VIDEO_BLOCK_JS, null)

        val shields = effectiveShields(currentMainFrameHost)
        if (shields.adBlockEnabled) {
            val selectors = CosmeticSelectors.GLOBAL + cosmeticSelectors(currentMainFrameHost)
            val js = CosmeticSelectors.buildHideJs(selectors)
            if (js.isNotEmpty()) view.evaluateJavascript(js, null)
        }

        if (deAmpEnabled && UrlSanitizer.isAmpUrl(url)) {
            view.evaluateJavascript(CANONICAL_LINK_JS) { rawResult ->
                val canonical = rawResult?.trim('"')?.takeIf { it.isNotEmpty() && it != "null" }
                if (canonical != null && canonical != url && !UrlSanitizer.isAmpUrl(canonical)) {
                    view.post { view.loadUrl(canonical, navigationHeaders()) }
                }
            }
        }

        onPageFinished(url, view.title)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        onSslError(handler, error)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (!request.isForMainFrame) return

        val failedUrl = request.url.toString()
        val fallback = httpsUpgradeMap.remove(failedUrl)
        if (fallback != null) {
            runCatching { Uri.parse(failedUrl).host }.getOrNull()?.let { upgradeAbandonedHosts.add(it) }
            onInsecureFallback(fallback)
            view.post { view.loadUrl(fallback, navigationHeaders()) }
            return
        }
        onReceivedError()
    }

    private fun isVideoUrl(url: String): Boolean {
        if (VIDEO_EXTENSIONS.any { url.endsWith(it) || url.contains("$it?") }) return true
        if (VIDEO_CDN_PATTERNS.any { url.contains(it) }) return true
        return false
    }

    companion object {
        private const val CANONICAL_LINK_JS =
            "(function(){var l=document.querySelector('link[rel=\"canonical\"]');return l?l.href:'';})();"

        private val VIDEO_EXTENSIONS = listOf(
            ".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".avi", ".mov", ".flv", ".ts"
        )

        private val VIDEO_CDN_PATTERNS = listOf(
            "googlevideo.com",
            "/videoplayback",
            "video.twimg.com",
            "vod-progressive.akamaized.net",
            "vimeo.com/video",
            "cdn.jwplayer.com",
            "cdn.plyr.io",
            "/hls/",
            "/dash/",
            "manifest.googlevideo.com"
        )

        private val VIDEO_BLOCK_JS = """
            (function() {
                function removeVideos() {
                    document.querySelectorAll('video, source').forEach(function(el) {
                        el.pause && el.pause();
                        el.src = '';
                        el.srcObject = null;
                        el.remove();
                    });
                }
                removeVideos();
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeName === 'VIDEO' || node.nodeName === 'SOURCE') {
                                node.pause && node.pause();
                                node.src = '';
                                node.remove();
                            }
                            if (node.querySelectorAll) {
                                node.querySelectorAll('video, source').forEach(function(el) {
                                    el.pause && el.pause();
                                    el.src = '';
                                    el.remove();
                                });
                            }
                        });
                    });
                });
                observer.observe(document.body || document.documentElement, {
                    childList: true,
                    subtree: true
                });
            })();
        """.trimIndent()
    }
}
