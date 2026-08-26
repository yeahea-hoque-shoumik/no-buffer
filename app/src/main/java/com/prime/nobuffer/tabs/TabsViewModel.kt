package com.prime.nobuffer.tabs

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.browser.BrowserWebView
import com.prime.nobuffer.data.entity.HistoryEntry
import com.prime.nobuffer.data.entity.TabEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val TAB_CLOSE_UNDO_WINDOW_MS = 5000L

class TabsViewModel(application: Application) : AndroidViewModel(application) {

    private val tabManager = TabManager(application)
    private val repository = (application as BrowserApplication).repository

    val tabs: StateFlow<List<BrowserTab>> = tabManager.tabs
    val activeIndex: StateFlow<Int> = tabManager.activeIndex

    val activeTabFlow: StateFlow<BrowserTab?> = combine(tabs, activeIndex) { list, index ->
        list.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tabManager.activeTab)

    init {
        viewModelScope.launch {
            val saved = repository.getSavedTabs()
            if (saved.isNotEmpty()) {
                val restored = saved.map { entity ->
                    BrowserTab(id = entity.id, url = entity.url, title = entity.title)
                }
                val activeIndex = saved.indexOfFirst { it.isActive }.let { if (it >= 0) it else 0 }
                tabManager.restoreTabs(restored, activeIndex)
            } else if (tabManager.tabs.value.isEmpty()) {
                tabManager.newTab("about:blank")
            }
        }
    }

    val activeTab: BrowserTab?
        get() = tabManager.activeTab

    fun newTab(url: String = "", isIncognito: Boolean = false): BrowserTab =
        tabManager.newTab(url, isIncognito)

    fun closeTab(tabId: String) = tabManager.closeTab(tabId)

    private val _pendingClose = MutableStateFlow<ClosedTab?>(null)
    val pendingClose: StateFlow<ClosedTab?> = _pendingClose.asStateFlow()
    private var pendingCloseJob: Job? = null

    fun closeTabWithUndo(tabId: String) {
        val closed = tabManager.removeTabForClose(tabId) ?: return
        finalizePendingClose()
        _pendingClose.value = closed
        pendingCloseJob = viewModelScope.launch {
            delay(TAB_CLOSE_UNDO_WINDOW_MS)
            finalizePendingClose()
        }
    }

    fun undoTabClose() {
        val closed = _pendingClose.value ?: return
        pendingCloseJob?.cancel()
        pendingCloseJob = null
        _pendingClose.value = null
        tabManager.restoreClosedTab(closed)
    }

    private fun finalizePendingClose() {
        val closed = _pendingClose.value ?: return
        pendingCloseJob?.cancel()
        pendingCloseJob = null
        _pendingClose.value = null
        tabManager.finalizeRemovedTab(closed)
    }

    fun switchTab(index: Int) = tabManager.switchTab(index)

    fun attachWebView(tabId: String, webView: BrowserWebView) = tabManager.attachWebView(tabId, webView)

    fun persistTabs() {
        val currentTabs = tabManager.tabs.value
        val activeIdx = tabManager.activeIndex.value
        val entities = currentTabs.mapIndexedNotNull { index, tab ->
            if (tab.isIncognito) null
            else TabEntity(id = tab.id, url = tab.url, title = tab.title, position = index, isActive = index == activeIdx)
        }
        viewModelScope.launch { repository.saveTabs(entities) }
    }

    fun captureSnapshot(tabId: String, bitmap: Bitmap) = tabManager.captureSnapshot(tabId, bitmap)

    override fun onCleared() {
        finalizePendingClose()
        super.onCleared()
    }

    fun updateTabInfo(tabId: String, url: String? = null, title: String? = null, favicon: Bitmap? = null) {
        tabManager.updateTabInfo(tabId, url, title, favicon)

        if (title != null) {
            val tab = tabManager.tabs.value.firstOrNull { it.id == tabId }
            if (tab != null && !tab.isIncognito && tab.url.isNotBlank() && tab.url != "about:blank" && title.isNotBlank()) {
                viewModelScope.launch {
                    repository.insertHistory(HistoryEntry(url = tab.url, title = title))
                }
            }
        }
    }
}
