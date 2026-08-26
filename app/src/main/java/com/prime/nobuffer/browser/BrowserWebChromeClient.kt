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
    private val onGeolocationPermissionRequested: (String, GeolocationPermissions.Callback) -> Unit = { _, callback -> callback.invoke(null, false, false) }
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        onReceivedTitle(title)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
        onReceivedIcon(icon)
    }

    // Layer 3: fullscreen video suppression — immediately hide custom view instead of showing it
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        callback.onCustomViewHidden()
    }

    override fun onHideCustomView() {
        // no-op: we never show it
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
