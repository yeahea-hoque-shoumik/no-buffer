package com.prime.nobuffer.tabs

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import com.prime.nobuffer.browser.BrowserWebView
import com.prime.nobuffer.browser.SiteCookies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClosedTab(val tab: BrowserTab, val index: Int, val wasActive: Boolean)

class TabManager(
    private val context: Context,
    recentlyClosedInitial: List<RecentlyClosedTab> = emptyList()
) {

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeIndex = MutableStateFlow(-1)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    private val recentlyClosedStore = RecentlyClosedStore(recentlyClosedInitial)
    val recentlyClosed: StateFlow<List<RecentlyClosedTab>> = recentlyClosedStore.items

    private val incognitoVisitedHosts = LinkedHashSet<String>()

    val activeTab: BrowserTab?
        get() = _tabs.value.getOrNull(_activeIndex.value)

    fun popRecentlyClosed(): RecentlyClosedTab? = recentlyClosedStore.pop()

    fun replaceRecentlyClosed(list: List<RecentlyClosedTab>) {
        recentlyClosedStore.replaceAll(list)
    }

    fun updateDesktopSite(tabId: String, enabled: Boolean) {
        updateTab(tabId) { it.copy(desktopSite = enabled) }
    }

    private fun recordClosed(tab: BrowserTab) {
        recentlyClosedStore.push(
            RecentlyClosedTab(
                url = tab.url,
                title = tab.title.ifBlank { tab.url },
                isIncognito = tab.isIncognito,
                closedAt = System.currentTimeMillis()
            )
        )
    }

    fun newTab(url: String = "", isIncognito: Boolean = false): BrowserTab {
        val webView = BrowserWebView(context)
        if (isIncognito) {
            webView.enableIncognitoIsolation()
        }
        val tab = BrowserTab(
            webView = webView,
            url = url,
            isIncognito = isIncognito
        )
        noteIncognitoUrl(tab)
        _tabs.update { it + tab }
        _activeIndex.value = _tabs.value.lastIndex
        return tab
    }

    fun restoreTabs(tabs: List<BrowserTab>, activeIndex: Int) {
        if (tabs.isEmpty()) return
        _tabs.value = tabs
        _activeIndex.value = activeIndex.coerceIn(tabs.indices)
    }

    fun attachWebView(tabId: String, webView: BrowserWebView) {
        updateTab(tabId) { tab -> if (tab.webView == null) tab.copy(webView = webView) else tab }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        val index = currentTabs.indexOfFirst { it.id == tabId }
        if (index == -1) return

        val closedTab = currentTabs[index]
        recordClosed(closedTab)
        closedTab.webView?.destroy()
        val updatedTabs = currentTabs.toMutableList().apply { removeAt(index) }
        _tabs.value = updatedTabs

        _activeIndex.value = when {
            updatedTabs.isEmpty() -> -1
            index < _activeIndex.value -> _activeIndex.value - 1
            index == _activeIndex.value -> index.coerceAtMost(updatedTabs.lastIndex)
            else -> _activeIndex.value
        }

        if (closedTab.isIncognito && updatedTabs.none { it.isIncognito }) {
            endIncognitoSession(updatedTabs)
        }

        if (updatedTabs.isEmpty()) {
            newTab("about:blank")
        }
    }

    /** Removes a tab from the list without destroying its WebView or backfilling an empty list — used to support undo. */
    fun removeTabForClose(tabId: String): ClosedTab? {
        val currentTabs = _tabs.value
        val index = currentTabs.indexOfFirst { it.id == tabId }
        if (index == -1) return null

        val closedTab = currentTabs[index]
        val wasActive = index == _activeIndex.value
        val updatedTabs = currentTabs.toMutableList().apply { removeAt(index) }
        _tabs.value = updatedTabs

        _activeIndex.value = when {
            updatedTabs.isEmpty() -> -1
            index < _activeIndex.value -> _activeIndex.value - 1
            index == _activeIndex.value -> index.coerceAtMost(updatedTabs.lastIndex)
            else -> _activeIndex.value
        }

        return ClosedTab(closedTab, index, wasActive)
    }

    /** Reinserts a tab previously removed via [removeTabForClose] at its original position. */
    fun restoreClosedTab(closed: ClosedTab) {
        val currentTabs = _tabs.value.toMutableList()
        val insertIndex = closed.index.coerceIn(0, currentTabs.size)
        currentTabs.add(insertIndex, closed.tab)
        _tabs.value = currentTabs

        _activeIndex.value = when {
            closed.wasActive -> insertIndex
            insertIndex <= _activeIndex.value -> _activeIndex.value + 1
            else -> _activeIndex.value
        }
    }

    /** Permanently disposes a tab removed via [removeTabForClose] once its undo window has elapsed. */
    fun finalizeRemovedTab(closed: ClosedTab) {
        recordClosed(closed.tab)
        closed.tab.webView?.destroy()

        if (closed.tab.isIncognito && _tabs.value.none { it.isIncognito }) {
            endIncognitoSession(_tabs.value)
        }

        if (_tabs.value.isEmpty()) {
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
            val updated = tab.copy(
                url = url ?: tab.url,
                title = title ?: tab.title,
                favicon = favicon ?: tab.favicon,
                blockedCount = if (url != null && url != tab.url) 0 else tab.blockedCount
            )
            noteIncognitoUrl(updated)
            updated
        }
    }

    private fun noteIncognitoUrl(tab: BrowserTab) {
        if (!tab.isIncognito) return
        SiteCookies.hostFrom(tab.url)?.let { incognitoVisitedHosts.add(it) }
    }

    /**
     * Drop cookies for hosts that were only used in private tabs. Regular tabs keep their
     * cookies and WebStorage — system WebView has no per-profile jar, so we must not wipe globally.
     */
    private fun endIncognitoSession(remainingTabs: List<BrowserTab>) {
        val regularHosts = remainingTabs
            .filter { !it.isIncognito }
            .mapNotNull { SiteCookies.hostFrom(it.url) }
            .toSet()
        val toPurge = incognitoVisitedHosts.filter { it !in regularHosts }
        SiteCookies.expireHosts(toPurge)
        incognitoVisitedHosts.clear()
        CookieManager.getInstance().flush()
    }

    fun incrementBlockedCount(tabId: String) {
        updateTab(tabId) { it.copy(blockedCount = it.blockedCount + 1) }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { tabs ->
            tabs.map { if (it.id == tabId) transform(it) else it }
        }
    }
}
