package com.prime.nobuffer.browser

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.downloads.DownloadLocation
import com.prime.nobuffer.shields.WebShieldsContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun BrowserWebViewComposable(
    url: String,
    modifier: Modifier = Modifier,
    existingWebView: BrowserWebView? = null,
    shields: WebShieldsContext = WebShieldsContext.disabled(),
    onRequestBlocked: () -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onPageStarted: (String) -> Unit = {},
    onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
    onTitleChanged: (String) -> Unit = {},
    onIconChanged: (Bitmap) -> Unit = {},
    onWebViewReady: (BrowserWebView) -> Unit = {},
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean = { _, _ -> false },
    onPermissionRequested: (PermissionRequest) -> Unit = { it.deny() },
    onGeolocationPermissionRequested: (String, GeolocationPermissions.Callback) -> Unit = { _, callback -> callback.invoke(null, false, false) },
    isSiteLocked: (String) -> Boolean = { false },
    onSiteLocked: (String) -> Unit = {},
    isIncognito: Boolean = false,
    onOpenInNewTab: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRefreshing by remember(existingWebView) { mutableStateOf(false) }

    val webView = remember(existingWebView) {
        (existingWebView ?: BrowserWebView(context)).apply {
            webViewClient = BrowserWebViewClient(
                onPageStarted = { pageUrl ->
                    val host = runCatching { Uri.parse(pageUrl).host }.getOrNull()
                    setFingerprintProtectionEnabled(shields.effectiveShields(host).fingerprintProtectionEnabled)
                    onPageStarted(pageUrl)
                },
                onPageFinished = { pageUrl, title ->
                    isRefreshing = false
                    onPageFinished(pageUrl, title)
                },
                onSslError = { handler, error ->
                    showSslErrorDialog(this, handler, error)
                },
                onReceivedError = { isRefreshing = false },
                effectiveShields = shields.effectiveShields,
                isHostBlocked = shields.isHostBlocked,
                onRequestBlocked = onRequestBlocked,
                cosmeticSelectors = shields.cosmeticSelectors,
                httpsUpgradeEnabled = shields.httpsUpgradeEnabled,
                trackingParamStrippingEnabled = shields.trackingParamStrippingEnabled,
                redirectorUnwrapEnabled = shields.redirectorUnwrapEnabled,
                deAmpEnabled = shields.deAmpEnabled,
                onHttpsLoadFailed = {
                    Toast.makeText(context, "Couldn't load this site over HTTPS", Toast.LENGTH_SHORT).show()
                },
                navigationHeaders = shields.navigationHeaders,
                isSiteLocked = isSiteLocked,
                onSiteLocked = onSiteLocked,
                isVideoAllowed = shields.isVideoAllowed
            )
            webChromeClient = BrowserWebChromeClient(
                onProgressChanged = onProgressChanged,
                onReceivedTitle = onTitleChanged,
                onReceivedIcon = onIconChanged,
                onShowFileChooserRequest = onShowFileChooser,
                onPermissionRequested = onPermissionRequested,
                onGeolocationPermissionRequested = onGeolocationPermissionRequested,
                isVideoAllowed = shields.isVideoAllowed
            )
            setOnLongClickListener { view ->
                val wv = view as WebView
                val result = wv.hitTestResult
                when (result.type) {
                    WebView.HitTestResult.SRC_ANCHOR_TYPE ->
                        result.extra?.let { showLinkMenu(context, it, onOpenInNewTab) } != null
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        val handler = android.os.Handler(android.os.Looper.getMainLooper()) { msg ->
                            msg.data?.getString("url")?.takeIf { it.isNotBlank() }
                                ?.let { showLinkMenu(context, it, onOpenInNewTab) }
                            true
                        }
                        wv.requestFocusNodeHref(handler.obtainMessage())
                        true
                    }
                    else -> false
                }
            }
            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
                when {
                    isIncognito ->
                        Toast.makeText(context, "Downloads are disabled in private tabs", Toast.LENGTH_SHORT).show()
                    mimeType?.startsWith("video/") == true ->
                        Toast.makeText(context, "Video downloads are blocked", Toast.LENGTH_SHORT).show()
                    else ->
                        downloadFile(context, downloadUrl, userAgent, contentDisposition, mimeType)
                }
            }
        }
    }

    LaunchedEffect(webView) { onWebViewReady(webView) }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_DESTROY -> if (existingWebView == null) webView.destroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    key(webView) {
        AndroidView(
            factory = {
                SwipeRefreshLayout(context).apply {
                    setColorSchemeColors(0xFF7B6EF5.toInt())
                    setOnRefreshListener {
                        isRefreshing = true
                        webView.reload()
                    }
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    addView(
                        webView,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = modifier,
            update = { layout ->
                layout.isRefreshing = isRefreshing
                if (url.isNotBlank() && webView.url != url) {
                    val client = webView.webViewClient as? BrowserWebViewClient
                    val resolved = client?.resolveNavigationUrl(url) ?: url
                    if (isSiteLocked(resolved)) {
                        onSiteLocked(resolved)
                    } else {
                        webView.loadUrl(resolved, client?.currentNavigationHeaders() ?: emptyMap())
                    }
                }
            }
        )
    }
}

private fun showLinkMenu(context: Context, url: String, onOpenInNewTab: (String) -> Unit) {
    AlertDialog.Builder(context)
        .setTitle(url)
        .setItems(arrayOf("Open in new tab", "Copy link")) { _, which ->
            when (which) {
                0 -> onOpenInNewTab(url)
                1 -> {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("link", url))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .show()
}

private fun downloadFile(context: Context, url: String, userAgent: String, contentDisposition: String, mimeType: String?) {
    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
    val request = DownloadManager.Request(Uri.parse(url)).apply {
        setMimeType(mimeType)
        addRequestHeader("User-Agent", userAgent)
        setTitle(fileName)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val location = runBlocking {
            val app = context.applicationContext as? BrowserApplication
            app?.settingsRepository?.settings?.first()?.downloadsLocation ?: "Downloads"
        }
        DownloadLocation.apply(this, fileName, location)
    }
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
    Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()
}

private fun showSslErrorDialog(webView: WebView, handler: SslErrorHandler, error: SslError) {
    val context = webView.context
    AlertDialog.Builder(context)
        .setTitle("Security Warning")
        .setMessage(
            "This site's security certificate is not trusted.\n\n" +
            "Error: ${sslErrorDescription(error.primaryError)}\n\n" +
            "Do you want to continue anyway?"
        )
        .setPositiveButton("Continue") { _, _ -> handler.proceed() }
        .setNegativeButton("Go Back") { _, _ -> handler.cancel() }
        .setOnCancelListener { handler.cancel() }
        .show()
}

private fun sslErrorDescription(primaryError: Int): String = when (primaryError) {
    SslError.SSL_EXPIRED -> "Certificate has expired"
    SslError.SSL_IDMISMATCH -> "Hostname mismatch"
    SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
    SslError.SSL_UNTRUSTED -> "Certificate authority is not trusted"
    else -> "Unknown SSL error"
}
