package com.prime.browser.tabs

import android.graphics.Bitmap
import com.prime.browser.browser.BrowserWebView
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val webView: BrowserWebView,
    val url: String = "",
    val title: String = "",
    val favicon: Bitmap? = null,
    val isIncognito: Boolean = false,
    val snapshotBitmap: Bitmap? = null
)
