package com.prime.browser.history

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.browser.BrowserApplication
import com.prime.browser.data.entity.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TimeRange(val label: String, val millis: Long?) {
    LAST_HOUR("Last hour", TimeUnit.HOURS.toMillis(1)),
    LAST_DAY("Last 24 hours", TimeUnit.DAYS.toMillis(1)),
    LAST_WEEK("Last 7 days", TimeUnit.DAYS.toMillis(7)),
    ALL_TIME("All time", null)
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BrowserApplication).repository

    private val query = MutableStateFlow("")

    private val allHistory: StateFlow<List<HistoryEntry>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedHistory: StateFlow<Map<String, List<HistoryEntry>>> = combine(allHistory, query) { list, q ->
        val filtered = if (q.isBlank()) {
            list
        } else {
            list.filter { it.title.contains(q, ignoreCase = true) || it.url.contains(q, ignoreCase = true) }
        }
        filtered.groupBy { dateLabel(it.visitedAt) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun filter(q: String) {
        query.value = q
    }

    fun delete(entry: HistoryEntry) {
        viewModelScope.launch { repository.deleteHistory(entry.id) }
    }

    fun undoDelete(entry: HistoryEntry) {
        viewModelScope.launch { repository.insertHistory(entry.copy(id = 0)) }
    }

    fun deleteAll(entries: List<HistoryEntry>) {
        viewModelScope.launch { entries.forEach { repository.deleteHistory(it.id) } }
    }

    fun clearBrowsingData(clearHistory: Boolean, clearCookies: Boolean, clearCache: Boolean, range: TimeRange) {
        viewModelScope.launch {
            if (clearHistory) {
                if (range == TimeRange.ALL_TIME) {
                    repository.clearAllHistory()
                } else {
                    val cutoff = System.currentTimeMillis() - (range.millis ?: 0)
                    allHistory.value.filter { it.visitedAt >= cutoff }.forEach { repository.deleteHistory(it.id) }
                }
            }
            if (clearCookies) {
                CookieManager.getInstance().removeAllCookies(null)
            }
            if (clearCache) {
                WebStorage.getInstance().deleteAllData()
            }
        }
    }
}

private fun dateLabel(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        cal.isSameDay(today) -> "Today"
        cal.isSameDay(yesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun Calendar.isSameDay(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
