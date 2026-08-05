package com.prime.browser.tabs

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import com.prime.browser.browser.BrowserWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TabManager(private val context: Context) {

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeIndex = MutableStateFlow(-1)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    val activeTab: BrowserTab?
        get() = _tabs.value.getOrNull(_activeIndex.value)

    fun newTab(url: String = "", isIncognito: Boolean = false): BrowserTab {
        val webView = BrowserWebView(context)
        if (isIncognito) {
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            CookieManager.getInstance().setAcceptCookie(false)
        }
        val tab = BrowserTab(
            webView = webView,
            url = url,
            isIncognito = isIncognito
        )
        _tabs.update { it + tab }
        _activeIndex.value = _tabs.value.lastIndex
        return tab
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val index = currentTabs.indexOfFirst { it.id == tabId }
        if (index == -1) return

        val closedTab = currentTabs[index]
        closedTab.webView.destroy()
        val updatedTabs = currentTabs.toMutableList().apply { removeAt(index) }
        _tabs.value = updatedTabs

        _activeIndex.value = when {
            updatedTabs.isEmpty() -> -1
            index < _activeIndex.value -> _activeIndex.value - 1
            index == _activeIndex.value -> index.coerceAtMost(updatedTabs.lastIndex)
            else -> _activeIndex.value
        }

        if (closedTab.isIncognito && updatedTabs.none { it.isIncognito }) {
            CookieManager.getInstance().apply {
                removeSessionCookies(null)
                flush()
                setAcceptCookie(true)
            }
            WebStorage.getInstance().deleteAllData()
        }

        if (updatedTabs.isEmpty()) {
            newTab("about:blank")
        }
    }

    fun switchTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeIndex.value = index
        }
    }

    fun captureSnapshot(tabId: String, bitmap: Bitmap) {
        updateTab(tabId) { it.copy(snapshotBitmap = bitmap) }
    }

    fun updateTabInfo(tabId: String, url: String? = null, title: String? = null, favicon: Bitmap? = null) {
        updateTab(tabId) { tab ->
            tab.copy(
                url = url ?: tab.url,
                title = title ?: tab.title,
                favicon = favicon ?: tab.favicon
            )
        }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { tabs ->
            tabs.map { if (it.id == tabId) transform(it) else it }
        }
    }
}
