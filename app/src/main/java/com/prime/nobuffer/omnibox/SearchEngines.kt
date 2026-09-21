package com.prime.nobuffer.omnibox

import com.prime.nobuffer.json.JsonLite
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SearchEngineSpec(
    val name: String,
    val searchUrl: String,
    val suggestUrl: String?
)

object SearchEngines {

    val BUILTIN: List<SearchEngineSpec> = listOf(
        SearchEngineSpec(
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=%s",
            suggestUrl = null
        ),
        SearchEngineSpec(
            name = "Google",
            searchUrl = "https://www.google.com/search?q=%s",
            suggestUrl = null
        ),
        SearchEngineSpec(
            name = "Bing",
            searchUrl = "https://www.bing.com/search?q=%s",
            suggestUrl = null
        )
    )

    fun parseCustom(customJson: String): List<SearchEngineSpec> {
        if (customJson.isBlank()) return emptyList()
        val root = runCatching { JsonLite.parse(customJson) }.getOrNull() as? List<*> ?: return emptyList()
        return root.mapNotNull { item ->
            val obj = item as? Map<*, *> ?: return@mapNotNull null
            val name = (obj["name"] as? String)?.trim().orEmpty()
            val searchUrl = (obj["searchUrl"] as? String)?.trim().orEmpty()
            if (name.isEmpty() || !searchUrl.contains("%s")) return@mapNotNull null
            val suggestUrl = (obj["suggestUrl"] as? String)?.trim()
                ?.takeIf { it.contains("%s") }
            SearchEngineSpec(name, searchUrl, suggestUrl)
        }
    }

    fun encodeCustom(engines: List<SearchEngineSpec>): String {
        val payload = engines.map { spec ->
            buildMap<String, Any?> {
                put("name", spec.name)
                put("searchUrl", spec.searchUrl)
                if (!spec.suggestUrl.isNullOrBlank()) put("suggestUrl", spec.suggestUrl)
            }
        }
        return JsonLite.stringify(payload)
    }

    fun all(customJson: String): List<SearchEngineSpec> = BUILTIN + parseCustom(customJson)

    fun selected(name: String, customJson: String): SearchEngineSpec {
        val engines = all(customJson)
        return engines.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: engines.firstOrNull { it.name.equals("DuckDuckGo", ignoreCase = true) }
            ?: BUILTIN.first()
    }

    fun searchUrl(spec: SearchEngineSpec, query: String): String =
        spec.searchUrl.replace("%s", encodeQuery(query))

    fun suggestEndpoint(spec: SearchEngineSpec, query: String): String? {
        val template = spec.suggestUrl ?: return null
        return template.replace("%s", encodeQuery(query))
    }

    fun looksLikeUrl(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        return trimmed.contains('.') && !trimmed.contains(' ')
    }

    fun resolveInput(input: String, spec: SearchEngineSpec): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            looksLikeUrl(trimmed) -> "https://$trimmed"
            else -> searchUrl(spec, trimmed)
        }
    }

    private fun encodeQuery(query: String): String =
        URLEncoder.encode(query, StandardCharsets.UTF_8.name())
}
