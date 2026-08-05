package com.prime.browser.tabs

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.browser.BrowserApplication
import com.prime.browser.data.entity.HistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TabsViewModel(application: Application) : AndroidViewModel(application) {

    private val tabManager = TabManager(application)
    private val repository = (application as BrowserApplication).repository

    val tabs: StateFlow<List<BrowserTab>> = tabManager.tabs
    val activeIndex: StateFlow<Int> = tabManager.activeIndex

    val activeTabFlow: StateFlow<BrowserTab?> = combine(tabs, activeIndex) { list, index ->
        list.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tabManager.activeTab)

    init {
        if (tabManager.tabs.value.isEmpty()) {
            tabManager.newTab("about:blank")
        }
    }

    val activeTab: BrowserTab?
        get() = tabManager.activeTab

    fun newTab(url: String = "", isIncognito: Boolean = false): BrowserTab =
        tabManager.newTab(url, isIncognito)

    fun closeTab(tabId: String) = tabManager.closeTab(tabId)

    fun switchTab(index: Int) = tabManager.switchTab(index)

    fun captureSnapshot(tabId: String, bitmap: Bitmap) = tabManager.captureSnapshot(tabId, bitmap)

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
