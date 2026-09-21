package com.prime.nobuffer.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prime.nobuffer.omnibox.SearchEngineSpec
import com.prime.nobuffer.omnibox.SearchEngines
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<BrowserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserSettings())

    fun setSearchEngine(value: String) = launch { repository.setSearchEngine(value) }
    fun setHomepageUrl(value: String) = launch { repository.setHomepageUrl(value) }
    fun setDownloadsLocation(value: String) = launch { repository.setDownloadsLocation(value) }
    fun setJavaScriptEnabled(value: Boolean) = launch { repository.setJavaScriptEnabled(value) }
    fun setAdBlockerEnabled(value: Boolean) = launch { repository.setAdBlockerEnabled(value) }
    fun setDoNotTrackEnabled(value: Boolean) = launch { repository.setDoNotTrackEnabled(value) }
    fun setBlockThirdPartyCookies(value: Boolean) = launch { repository.setBlockThirdPartyCookies(value) }
    fun setSafeBrowsingEnabled(value: Boolean) = launch { repository.setSafeBrowsingEnabled(value) }
    fun setSearchSuggestionsEnabled(value: Boolean) = launch { repository.setSearchSuggestionsEnabled(value) }
    fun setTextZoom(value: Int) = launch { repository.setTextZoom(value) }
    fun setDesktopSiteEnabled(value: Boolean) = launch { repository.setDesktopSiteEnabled(value) }
    fun setDarkMode(value: DarkModeOption) = launch { repository.setDarkMode(value) }
    fun setLocationPermission(value: Boolean) = launch { repository.setLocationPermission(value) }
    fun setMicPermission(value: Boolean) = launch { repository.setMicPermission(value) }
    fun setCameraPermission(value: Boolean) = launch { repository.setCameraPermission(value) }
    fun setNotificationsPermission(value: Boolean) = launch { repository.setNotificationsPermission(value) }
    fun setPopupsBlocked(value: Boolean) = launch { repository.setPopupsBlocked(value) }
    fun setAutofillEnabled(value: Boolean) = launch { repository.setAutofillEnabled(value) }
    fun setAntiFingerprintingEnabled(value: Boolean) = launch { repository.setAntiFingerprintingEnabled(value) }
    fun setShieldsMode(value: ShieldsMode) = launch { repository.setShieldsMode(value) }
    fun setPermissionGrantTtlHours(value: Int) = launch { repository.setPermissionGrantTtlHours(value) }
    fun setForceDarkPages(value: Boolean) = launch { repository.setForceDarkPages(value) }
    fun setCustomSearchEnginesJson(value: String) = launch { repository.setCustomSearchEnginesJson(value) }

    fun addCustomSearchEngine(name: String, searchUrl: String, suggestUrl: String? = null) {
        val trimmedName = name.trim()
        val trimmedUrl = searchUrl.trim()
        if (trimmedName.isEmpty() || !trimmedUrl.contains("%s")) return
        val trimmedSuggest = suggestUrl?.trim()?.takeIf { it.contains("%s") }
        launch {
            val current = repository.settings.first()
            val custom = SearchEngines.parseCustom(current.customSearchEnginesJson)
                .filterNot { it.name.equals(trimmedName, ignoreCase = true) }
            val next = custom + SearchEngineSpec(trimmedName, trimmedUrl, trimmedSuggest)
            repository.setCustomSearchEnginesJson(SearchEngines.encodeCustom(next))
            repository.setSearchEngine(trimmedName)
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
