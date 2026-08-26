package com.prime.nobuffer.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
