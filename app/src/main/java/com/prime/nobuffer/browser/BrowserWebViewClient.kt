package com.prime.nobuffer.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.MutableState

class BrowserWebViewClient(
    private val onPageStarted: (url: String) -> Unit = {},
    private val onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
    private val onProgressChanged: (progress: Int) -> Unit = {},
    private val onSslError: (handler: SslErrorHandler, error: SslError) -> Unit = { handler, _ -> handler.cancel() },
    private val onReceivedError: () -> Unit = {}
) : WebViewClient() {

    // Layer 1: network-level URL blocking — blocks video by extension, MIME, and CDN patterns
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString().lowercase()
        if (isVideoUrl(url)) {
            return WebResourceResponse("video/mp4", "UTF-8", null)
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted(url)
    }

    // Layer 2: DOM removal + MutationObserver injected after page load
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        view.evaluateJavascript(VIDEO_BLOCK_JS, null)
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
        if (request.isForMainFrame) onReceivedError()
    }

    private fun isVideoUrl(url: String): Boolean {
        if (VIDEO_EXTENSIONS.any { url.endsWith(it) || url.contains("$it?") }) return true
        if (VIDEO_CDN_PATTERNS.any { url.contains(it) }) return true
        return false
    }

    companion object {
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
