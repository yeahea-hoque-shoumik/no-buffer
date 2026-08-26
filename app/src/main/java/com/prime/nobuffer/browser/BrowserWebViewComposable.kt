package com.prime.nobuffer.browser

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
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

@Composable
fun BrowserWebViewComposable(
    url: String,
    modifier: Modifier = Modifier,
    existingWebView: BrowserWebView? = null,
    onProgressChanged: (Int) -> Unit = {},
    onPageStarted: (String) -> Unit = {},
    onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
    onTitleChanged: (String) -> Unit = {},
    onIconChanged: (Bitmap) -> Unit = {},
    onWebViewReady: (BrowserWebView) -> Unit = {},
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean = { _, _ -> false },
    onPermissionRequested: (PermissionRequest) -> Unit = { it.deny() },
    onGeolocationPermissionRequested: (String, GeolocationPermissions.Callback) -> Unit = { _, callback -> callback.invoke(null, false, false) }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRefreshing by remember(existingWebView) { mutableStateOf(false) }

    val webView = remember(existingWebView) {
        (existingWebView ?: BrowserWebView(context)).apply {
            webViewClient = BrowserWebViewClient(
                onPageStarted = onPageStarted,
                onPageFinished = { url, title ->
                    isRefreshing = false
                    onPageFinished(url, title)
                },
                onSslError = { handler, error ->
                    showSslErrorDialog(this, handler, error)
                },
                onReceivedError = { isRefreshing = false }
            )
            webChromeClient = BrowserWebChromeClient(
                onProgressChanged = onProgressChanged,
                onReceivedTitle = onTitleChanged,
                onReceivedIcon = onIconChanged,
                onShowFileChooserRequest = onShowFileChooser,
                onPermissionRequested = onPermissionRequested,
                onGeolocationPermissionRequested = onGeolocationPermissionRequested
            )
            setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
                if (mimeType?.startsWith("video/") == true) {
                    Toast.makeText(context, "Video downloads are blocked", Toast.LENGTH_SHORT).show()
                } else {
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
                    webView.loadUrl(url)
                }
            }
        )
    }
}

private fun downloadFile(context: Context, url: String, userAgent: String, contentDisposition: String, mimeType: String?) {
    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
    val request = DownloadManager.Request(Uri.parse(url)).apply {
        setMimeType(mimeType)
        addRequestHeader("User-Agent", userAgent)
        setTitle(fileName)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
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
