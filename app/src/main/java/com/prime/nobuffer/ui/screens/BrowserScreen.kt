package com.prime.nobuffer.ui.screens

import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prime.nobuffer.browser.BrowserWebView
import com.prime.nobuffer.browser.BrowserWebViewComposable
import com.prime.nobuffer.ui.components.FindInPageBar
import com.prime.nobuffer.ui.components.HomeIndicator
import com.prime.nobuffer.ui.components.PillBar
import com.prime.nobuffer.ui.components.StatusBar
import com.prime.nobuffer.ui.theme.Orion

/**
 * Single persistent shell for the browser tab: StatusBar/PillBar/HomeIndicator stay mounted
 * across home <-> page navigation. Only the content Box (WebView vs. home overlay) changes,
 * so the pill bar never flickers/remounts.
 */
@Composable
fun BrowserScreen(
    startUrl: String,
    tabCount: Int = 1,
    isIncognito: Boolean = false,
    webView: BrowserWebView? = null,
    findInPageSignal: Int = 0,
    onUrlChanged: (String) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    onOpenOmnibox: () -> Unit = {},
    onOpenTabSwitcher: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onExhausted: () -> Unit = {},
    onWebViewReady: (BrowserWebView) -> Unit = {},
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean = { _, _ -> false },
    onPermissionRequested: (PermissionRequest) -> Unit = { it.deny() },
    onGeolocationPermissionRequested: (String, GeolocationPermissions.Callback) -> Unit = { _, callback -> callback.invoke(null, false, false) },
    modifier: Modifier = Modifier
) {
    val colors = Orion.colors

    var webViewRef by remember { mutableStateOf<BrowserWebView?>(null) }
    var currentUrl by remember(startUrl) { mutableStateOf(startUrl) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var findInPageVisible by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    val isHome = currentUrl.isBlank() || currentUrl == "about:blank" || currentUrl == "about:newtab"

    LaunchedEffect(findInPageSignal) {
        if (findInPageSignal > 0) findInPageVisible = true
    }

    BackHandler(enabled = findInPageVisible) {
        findInPageVisible = false
        findQuery = ""
        webViewRef?.clearMatches()
    }
    BackHandler(enabled = !findInPageVisible && !isHome && canGoBack) {
        webViewRef?.goBack()
    }
    BackHandler(enabled = !findInPageVisible && !isHome && !canGoBack) {
        onUrlChanged("about:blank")
    }
    BackHandler(enabled = !findInPageVisible && isHome) {
        onExhausted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        StatusBar()
        PillBar(
            url = currentUrl,
            showBack = !isHome && canGoBack,
            tabCount = tabCount,
            onBackClick = { webViewRef?.goBack() },
            onFieldClick = onOpenOmnibox,
            onTabsClick = onOpenTabSwitcher,
            onMenuClick = onOpenMenu
        )

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .height(2.dp),
                color = colors.accent,
                trackColor = colors.border
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            BrowserWebViewComposable(
                url = startUrl,
                modifier = Modifier.fillMaxSize(),
                existingWebView = webView,
                onWebViewReady = { webViewRef = it; onWebViewReady(it) },
                onProgressChanged = { progress = it },
                onPageStarted = { url ->
                    currentUrl = url
                    onUrlChanged(url)
                    canGoBack = webViewRef?.canGoBack() ?: false
                },
                onPageFinished = { url, title ->
                    currentUrl = url
                    onUrlChanged(url)
                    title?.let(onTitleChanged)
                    canGoBack = webViewRef?.canGoBack() ?: false
                },
                onTitleChanged = onTitleChanged,
                onShowFileChooser = onShowFileChooser,
                onPermissionRequested = onPermissionRequested,
                onGeolocationPermissionRequested = onGeolocationPermissionRequested
            )

            if (isHome) {
                if (isIncognito) {
                    IncognitoNewTabContent(modifier = Modifier.fillMaxSize())
                } else {
                    NewTabContent(onNavigate = onUrlChanged, modifier = Modifier.fillMaxSize())
                }
            }

            if (findInPageVisible) {
                FindInPageBar(
                    query = findQuery,
                    activeMatch = 0,
                    totalMatches = 0,
                    onQueryChange = { query ->
                        findQuery = query
                        webViewRef?.findAllAsync(query)
                    },
                    onNext = { webViewRef?.findNext(true) },
                    onPrevious = { webViewRef?.findNext(false) },
                    onClose = {
                        findInPageVisible = false
                        findQuery = ""
                        webViewRef?.clearMatches()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HomeIndicator()
    }
}
