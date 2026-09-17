package com.prime.nobuffer.tabs

import android.graphics.Bitmap
import com.prime.nobuffer.browser.BrowserWebView
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val webView: BrowserWebView? = null,
    val url: String = "",
    val title: String = "",
    val favicon: Bitmap? = null,
    val isIncognito: Boolean = false,
    val snapshotBitmap: Bitmap? = null,
    /** Phase 17 — count of ad/tracker requests blocked on this tab's current page. */
    val blockedCount: Int = 0
)
