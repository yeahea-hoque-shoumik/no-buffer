package com.prime.nobuffer.shields

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Persists extra EasyList-style host rules under `filesDir/blocklists/extra_hosts.txt`
 * and merges them into [AdTrackerBlocklist]. Does not fetch on init — call
 * [updateFromUrl] explicitly if a remote refresh is wanted.
 */
class BlocklistUpdater(
    private val context: Context,
    private val blocklist: AdTrackerBlocklist
) {

    init {
        loadPersistedExtras()
    }

    fun loadPersistedExtras() {
        val file = extraFile()
        if (!file.exists()) return
        val hosts = file.useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        }
        blocklist.mergeHosts(hosts)
    }

    suspend fun applyText(text: String) {
        val hosts = EasyListParser.parseHosts(text)
        withContext(Dispatchers.IO) {
            val file = extraFile()
            file.parentFile?.mkdirs()
            file.writeText(hosts.joinToString("\n"))
        }
        blocklist.mergeHosts(hosts)
    }

    /** Unused by default. Pass [client] to avoid the built-in HttpURLConnection fetch. */
    suspend fun updateFromUrl(url: String, client: (suspend (String) -> String)? = null) {
        val text = withContext(Dispatchers.IO) {
            if (client != null) client(url) else fetch(url)
        }
        applyText(text)
    }

    private fun extraFile(): File = File(context.filesDir, AdTrackerBlocklist.EXTRA_HOSTS_PATH)

    private fun fetch(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code !in 200..299) error("Blocklist fetch failed: HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
