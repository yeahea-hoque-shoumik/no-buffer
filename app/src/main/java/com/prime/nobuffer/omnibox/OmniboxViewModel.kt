package com.prime.nobuffer.omnibox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.nobuffer.BrowserApplication
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class)
class OmniboxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BrowserApplication).repository

    private val query = MutableStateFlow("")

    val suggestions: StateFlow<List<OmniboxSuggestion>> = query
        .debounce(150)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else flow { emit(buildSuggestions(q)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    private suspend fun buildSuggestions(q: String): List<OmniboxSuggestion> {
        val bookmarks = repository.searchBookmarks(q)
            .filter { !it.isFolder }
            .take(4)
            .map { OmniboxSuggestion(it.title.ifBlank { it.url }, it.url, SuggestionType.BOOKMARK) }

        val recent = repository.searchHistory(q)
            .take(4)
            .map { OmniboxSuggestion(it.title.ifBlank { it.url }, it.url, SuggestionType.RECENT) }

        val searchSuggestion = OmniboxSuggestion(q, "Search the web", SuggestionType.SEARCH)

        return listOf(searchSuggestion) + bookmarks + recent
    }
}
