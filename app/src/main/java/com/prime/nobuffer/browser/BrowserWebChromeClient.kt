package com.prime.nobuffer.browser

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class BrowserWebChromeClient(
    private val onProgressChanged: (progress: Int) -> Unit = {},
    private val onReceivedTitle: (title: String) -> Unit = {},
    private val onReceivedIcon: (icon: Bitmap) -> Unit = {},
    private val onShowFileChooserRequest: (ValueCallback<Array<Uri>>, FileChooserParams) -> Boolean = { _, _ -> false },
    private val onPermissionRequested: (PermissionRequest) -> Unit = { it.deny() },
    private val onGeolocationPermissionRequested: (String, GeolocationPermissions.Callback) -> Unit = { _, callback -> callback.invoke(null, false, false) },
    private val isVideoAllowed: (host: String?) -> Boolean = { false }
) : WebChromeClient() {

    private var lastWebView: WebView? = null

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        lastWebView = view
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        lastWebView = view
        super.onReceivedTitle(view, title)
        onReceivedTitle(title)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        lastWebView = view
        super.onReceivedIcon(view, icon)
        onReceivedIcon(icon)
    }

    // Layer 3: fullscreen video suppression — skip when the current host is allowlisted
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val host = lastWebView?.url?.let { runCatching { Uri.parse(it).host }.getOrNull() }
        if (isVideoAllowed(host)) {
            super.onShowCustomView(view, callback)
            return
        }
        callback.onCustomViewHidden()
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean = onShowFileChooserRequest(filePathCallback, fileChooserParams)

    override fun onPermissionRequest(request: PermissionRequest) {
        onPermissionRequested(request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        onGeolocationPermissionRequested(origin, callback)
    }
}
