package com.prime.browser.omnibox

enum class SuggestionType { RECENT, BOOKMARK, SEARCH }

data class OmniboxSuggestion(
    val text: String,
    val subtitle: String,
    val type: SuggestionType
)
