package com.prime.nobuffer.omnibox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.nobuffer.BrowserApplication
import com.prime.nobuffer.settings.BrowserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class OmniboxViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BrowserApplication
    private val repository = app.repository
    private val settingsRepository = app.settingsRepository

    private val query = MutableStateFlow("")

    private val browserSettings: StateFlow<BrowserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, BrowserSettings())

    val suggestions: StateFlow<List<OmniboxSuggestion>> = query
        .debounce(150)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList()) else flow { emit(buildSuggestions(q)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun resolveInput(input: String): String {
        val settings = browserSettings.value
        val spec = SearchEngines.selected(settings.searchEngine, settings.customSearchEnginesJson)
        return SearchEngines.resolveInput(input, spec)
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
