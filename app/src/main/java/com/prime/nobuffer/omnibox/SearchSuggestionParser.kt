package com.prime.nobuffer.omnibox

import com.prime.nobuffer.json.JsonLite

object SearchSuggestionParser {

    /**
     * Parses Google/Bing firefox-format JSON (`[query, [s1, s2, ...]]`) and
     * DuckDuckGo autocomplete JSON (`[{"phrase":"..."}, ...]`).
     */
    fun parse(body: String): List<String> {
        val trimmed = body.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return emptyList()
        val parsed = runCatching { JsonLite.parse(trimmed) }.getOrNull() ?: return emptyList()
        val list = parsed as? List<*> ?: return emptyList()
        val suggestions = when (val second = list.getOrNull(1)) {
            is List<*> -> second.mapNotNull { it as? String }
            else -> list.mapNotNull { item ->
                (item as? Map<*, *>)?.get("phrase") as? String
            }
        }
        return suggestions.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
