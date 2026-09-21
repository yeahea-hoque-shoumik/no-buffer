package com.prime.nobuffer.tabs

import com.prime.nobuffer.json.JsonLite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RecentlyClosedTab(
    val url: String,
    val title: String,
    val isIncognito: Boolean,
    val closedAt: Long
)

class RecentlyClosedStore(initial: List<RecentlyClosedTab> = emptyList()) {

    private val _items = MutableStateFlow(sanitize(initial))
    val items: StateFlow<List<RecentlyClosedTab>> = _items.asStateFlow()

    fun replaceAll(list: List<RecentlyClosedTab>) {
        _items.value = sanitize(list)
    }

    fun push(tab: RecentlyClosedTab) {
        if (!isEligible(tab)) return
        _items.update { current ->
            (listOf(tab) + current).take(MAX_SIZE)
        }
    }

    fun pop(): RecentlyClosedTab? {
        val current = _items.value
        val first = current.firstOrNull() ?: return null
        _items.value = current.drop(1)
        return first
    }

    companion object {
        const val MAX_SIZE = 20

        fun isEligible(tab: RecentlyClosedTab): Boolean {
            if (tab.isIncognito) return false
            val url = tab.url.trim()
            return url.isNotEmpty() && !url.equals("about:blank", ignoreCase = true)
        }

        fun fromJson(json: String): List<RecentlyClosedTab> {
            if (json.isBlank()) return emptyList()
            val root = runCatching { JsonLite.parse(json) }.getOrNull() as? List<*> ?: return emptyList()
            return sanitize(
                root.mapNotNull { item ->
                    val obj = item as? Map<*, *> ?: return@mapNotNull null
                    val url = obj["url"] as? String ?: return@mapNotNull null
                    val title = obj["title"] as? String ?: ""
                    val isIncognito = obj["isIncognito"] as? Boolean ?: false
                    val closedAt = (obj["closedAt"] as? Number)?.toLong() ?: 0L
                    RecentlyClosedTab(url = url, title = title, isIncognito = isIncognito, closedAt = closedAt)
                }
            )
        }

        fun toJson(list: List<RecentlyClosedTab>): String {
            val payload = sanitize(list).map { tab ->
                mapOf(
                    "url" to tab.url,
                    "title" to tab.title,
                    "isIncognito" to tab.isIncognito,
                    "closedAt" to tab.closedAt
                )
            }
            return JsonLite.stringify(payload)
        }

        private fun sanitize(list: List<RecentlyClosedTab>): List<RecentlyClosedTab> =
            list.filter { isEligible(it) }.take(MAX_SIZE)
    }
}
