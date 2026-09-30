package com.prime.nobuffer.shields.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Owns the EasyList (ads) and EasyPrivacy (trackers) [FilterEngine]s. Lists ship in assets and are
 * refreshed into `filesDir/blocklists/` at most once per [REFRESH_INTERVAL_MS]. Until the engines
 * finish loading, [shouldBlock] returns false — the host blocklist still covers that window.
 */
class FilterEngineManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    @Volatile private var ads: FilterEngine? = null
    @Volatile private var trackers: FilterEngine? = null

    fun loadAsync() {
        scope.launch(Dispatchers.Default) { rebuild() }
    }

    /** Fetches fresh lists if [lastUpdatedMs] is stale, rebuilds engines, and returns the new timestamp (or null if nothing changed). */
    suspend fun refreshIfStale(lastUpdatedMs: Long, nowMs: Long = System.currentTimeMillis()): Long? {
        if (nowMs - lastUpdatedMs < REFRESH_INTERVAL_MS) return null
        var updated = false
        for (list in LISTS) {
            updated = runCatching { download(list) }.getOrDefault(false) || updated
        }
        if (!updated) return null
        withContext(Dispatchers.Default) { rebuild() }
        return nowMs
    }

    fun shouldBlock(
        url: String,
        headers: Map<String, String>,
        isMainFrame: Boolean,
        firstPartyHost: String?,
        adsEnabled: Boolean,
        trackersEnabled: Boolean
    ): Boolean {
        val adEngine = if (adsEnabled) ads else null
        val trackerEngine = if (trackersEnabled) trackers else null
        if (adEngine == null && trackerEngine == null) return false
        val type = RequestType.resolve(url.lowercase(), headers, isMainFrame)
        return adEngine?.shouldBlock(url, firstPartyHost, type) == true ||
            trackerEngine?.shouldBlock(url, firstPartyHost, type) == true
    }

    /** CSS to inject at document start for [host]; [extraSelectors] are appended (built-in + user rules). */
    fun cosmeticCss(host: String?, extraSelectors: List<String> = emptyList()): String {
        val selectors = (ads?.cosmetic?.selectorsFor(host).orEmpty() + extraSelectors).distinct()
        return CosmeticIndex.buildCss(selectors)
    }

    private fun rebuild() {
        ads = buildEngine(LISTS[0])
        trackers = buildEngine(LISTS[1])
    }

    private fun buildEngine(list: RemoteList): FilterEngine? = runCatching {
        val downloaded = File(context.filesDir, "$DIR/${list.fileName}")
        val stream = if (downloaded.exists()) downloaded.inputStream()
        else context.assets.open("$DIR/${list.fileName}")
        stream.bufferedReader().use { reader ->
            FilterEngine.Builder().addLines(reader.lineSequence()).build()
        }
    }.getOrNull()

    private fun download(list: RemoteList): Boolean {
        val target = File(context.filesDir, "$DIR/${list.fileName}")
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${list.fileName}.tmp")
        val connection = URL(list.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        } finally {
            connection.disconnect()
        }
        // Reject truncated/HTML error pages so a bad fetch never replaces a working list.
        val head = tmp.bufferedReader().use { it.readLine().orEmpty() }
        if (tmp.length() < MIN_LIST_BYTES || !head.startsWith("[Adblock", ignoreCase = true)) {
            tmp.delete()
            return false
        }
        return tmp.renameTo(target) || (target.delete() && tmp.renameTo(target))
    }

    private class RemoteList(val fileName: String, val url: String)

    companion object {
        private const val DIR = "blocklists"
        private const val MIN_LIST_BYTES = 50_000L
        const val REFRESH_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
        private val LISTS = listOf(
            RemoteList("easylist.txt", "https://easylist.to/easylist/easylist.txt"),
            RemoteList("easyprivacy.txt", "https://easylist.to/easylist/easyprivacy.txt")
        )
    }
}
