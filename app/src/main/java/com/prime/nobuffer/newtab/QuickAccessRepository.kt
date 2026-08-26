package com.prime.nobuffer.newtab

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class QuickAccessSite(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val url: String,
    val colorArgb: Int
)

private val Context.quickAccessDataStore by preferencesDataStore(name = "quick_access")

// Single-line text fields can't contain these, so they're safe as delimiters.
private const val FIELD_SEPARATOR = "\t"
private const val RECORD_SEPARATOR = "\n"

class QuickAccessRepository(private val context: Context) {

    private val sitesKey = stringPreferencesKey("sites")

    val sites: Flow<List<QuickAccessSite>> = context.quickAccessDataStore.data.map { prefs ->
        prefs[sitesKey]?.let { decode(it) } ?: defaultSites()
    }

    suspend fun save(sites: List<QuickAccessSite>) {
        context.quickAccessDataStore.edit { it[sitesKey] = encode(sites) }
    }

    fun defaultSites(): List<QuickAccessSite> = listOf(
        QuickAccessSite(label = "Google", url = "https://www.google.com", colorArgb = 0xFF7B6EF5.toInt()),
        QuickAccessSite(label = "YouTube", url = "https://www.youtube.com", colorArgb = 0xFFE84C4C.toInt()),
        QuickAccessSite(label = "Wikipedia", url = "https://www.wikipedia.org", colorArgb = 0xFF36C9B0.toInt()),
        QuickAccessSite(label = "GitHub", url = "https://www.github.com", colorArgb = 0xFF9994B8.toInt()),
        QuickAccessSite(label = "Reddit", url = "https://www.reddit.com", colorArgb = 0xFFE8A33D.toInt()),
        QuickAccessSite(label = "X", url = "https://www.x.com", colorArgb = 0xFF6B5EE4.toInt()),
        QuickAccessSite(label = "Amazon", url = "https://www.amazon.com", colorArgb = 0xFFE8A33D.toInt()),
        QuickAccessSite(label = "Netflix", url = "https://www.netflix.com", colorArgb = 0xFFE84C4C.toInt())
    )

    private fun encode(sites: List<QuickAccessSite>): String =
        sites.joinToString(RECORD_SEPARATOR) { site ->
            listOf(site.id, site.label, site.url, site.colorArgb.toString()).joinToString(FIELD_SEPARATOR)
        }

    private fun decode(raw: String): List<QuickAccessSite> {
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val parts = record.split(FIELD_SEPARATOR)
            if (parts.size == 4) {
                QuickAccessSite(
                    id = parts[0],
                    label = parts[1],
                    url = parts[2],
                    colorArgb = parts[3].toIntOrNull() ?: 0xFF7B6EF5.toInt()
                )
            } else {
                null
            }
        }
    }
}
