package com.prime.nobuffer.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "browser_settings")

enum class DarkModeOption { SYSTEM, LIGHT, DARK }

data class BrowserSettings(
    val searchEngine: String = "Google",
    val homepageUrl: String = "",
    val downloadsLocation: String = "Downloads",
    val javaScriptEnabled: Boolean = true,
    val adBlockerEnabled: Boolean = false,
    val doNotTrackEnabled: Boolean = false,
    val blockThirdPartyCookies: Boolean = false,
    val safeBrowsingEnabled: Boolean = true,
    val searchSuggestionsEnabled: Boolean = true,
    val textZoom: Int = 100,
    val desktopSiteEnabled: Boolean = false,
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val locationPermission: Boolean = false,
    val micPermission: Boolean = false,
    val cameraPermission: Boolean = false,
    val notificationsPermission: Boolean = false,
    val popupsBlocked: Boolean = true,
    val autofillEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val HOMEPAGE_URL = stringPreferencesKey("homepage_url")
        val DOWNLOADS_LOCATION = stringPreferencesKey("downloads_location")
        val JS_ENABLED = booleanPreferencesKey("js_enabled")
        val AD_BLOCKER = booleanPreferencesKey("ad_blocker")
        val DNT = booleanPreferencesKey("do_not_track")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val SAFE_BROWSING = booleanPreferencesKey("safe_browsing")
        val SEARCH_SUGGESTIONS = booleanPreferencesKey("search_suggestions")
        val TEXT_ZOOM = intPreferencesKey("text_zoom")
        val DESKTOP_SITE = booleanPreferencesKey("desktop_site")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val LOCATION_PERM = booleanPreferencesKey("location_permission")
        val MIC_PERM = booleanPreferencesKey("mic_permission")
        val CAMERA_PERM = booleanPreferencesKey("camera_permission")
        val NOTIFICATIONS_PERM = booleanPreferencesKey("notifications_permission")
        val POPUPS_BLOCKED = booleanPreferencesKey("popups_blocked")
        val AUTOFILL_ENABLED = booleanPreferencesKey("autofill_enabled")
    }

    val settings: Flow<BrowserSettings> = context.dataStore.data.map { prefs ->
        BrowserSettings(
            searchEngine = prefs[Keys.SEARCH_ENGINE] ?: "Google",
            homepageUrl = prefs[Keys.HOMEPAGE_URL] ?: "",
            downloadsLocation = prefs[Keys.DOWNLOADS_LOCATION] ?: "Downloads",
            javaScriptEnabled = prefs[Keys.JS_ENABLED] ?: true,
            adBlockerEnabled = prefs[Keys.AD_BLOCKER] ?: false,
            doNotTrackEnabled = prefs[Keys.DNT] ?: false,
            blockThirdPartyCookies = prefs[Keys.BLOCK_THIRD_PARTY_COOKIES] ?: false,
            safeBrowsingEnabled = prefs[Keys.SAFE_BROWSING] ?: true,
            searchSuggestionsEnabled = prefs[Keys.SEARCH_SUGGESTIONS] ?: true,
            textZoom = prefs[Keys.TEXT_ZOOM] ?: 100,
            desktopSiteEnabled = prefs[Keys.DESKTOP_SITE] ?: false,
            darkMode = prefs[Keys.DARK_MODE]?.let { runCatching { DarkModeOption.valueOf(it) }.getOrNull() } ?: DarkModeOption.SYSTEM,
            locationPermission = prefs[Keys.LOCATION_PERM] ?: false,
            micPermission = prefs[Keys.MIC_PERM] ?: false,
            cameraPermission = prefs[Keys.CAMERA_PERM] ?: false,
            notificationsPermission = prefs[Keys.NOTIFICATIONS_PERM] ?: false,
            popupsBlocked = prefs[Keys.POPUPS_BLOCKED] ?: true,
            autofillEnabled = prefs[Keys.AUTOFILL_ENABLED] ?: true
        )
    }

    suspend fun setSearchEngine(value: String) = context.dataStore.edit { it[Keys.SEARCH_ENGINE] = value }
    suspend fun setHomepageUrl(value: String) = context.dataStore.edit { it[Keys.HOMEPAGE_URL] = value }
    suspend fun setDownloadsLocation(value: String) = context.dataStore.edit { it[Keys.DOWNLOADS_LOCATION] = value }
    suspend fun setJavaScriptEnabled(value: Boolean) = context.dataStore.edit { it[Keys.JS_ENABLED] = value }
    suspend fun setAdBlockerEnabled(value: Boolean) = context.dataStore.edit { it[Keys.AD_BLOCKER] = value }
    suspend fun setDoNotTrackEnabled(value: Boolean) = context.dataStore.edit { it[Keys.DNT] = value }
    suspend fun setBlockThirdPartyCookies(value: Boolean) = context.dataStore.edit { it[Keys.BLOCK_THIRD_PARTY_COOKIES] = value }
    suspend fun setSafeBrowsingEnabled(value: Boolean) = context.dataStore.edit { it[Keys.SAFE_BROWSING] = value }
    suspend fun setSearchSuggestionsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.SEARCH_SUGGESTIONS] = value }
    suspend fun setTextZoom(value: Int) = context.dataStore.edit { it[Keys.TEXT_ZOOM] = value }
    suspend fun setDesktopSiteEnabled(value: Boolean) = context.dataStore.edit { it[Keys.DESKTOP_SITE] = value }
    suspend fun setDarkMode(value: DarkModeOption) = context.dataStore.edit { it[Keys.DARK_MODE] = value.name }
    suspend fun setLocationPermission(value: Boolean) = context.dataStore.edit { it[Keys.LOCATION_PERM] = value }
    suspend fun setMicPermission(value: Boolean) = context.dataStore.edit { it[Keys.MIC_PERM] = value }
    suspend fun setCameraPermission(value: Boolean) = context.dataStore.edit { it[Keys.CAMERA_PERM] = value }
    suspend fun setNotificationsPermission(value: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS_PERM] = value }
    suspend fun setPopupsBlocked(value: Boolean) = context.dataStore.edit { it[Keys.POPUPS_BLOCKED] = value }
    suspend fun setAutofillEnabled(value: Boolean) = context.dataStore.edit { it[Keys.AUTOFILL_ENABLED] = value }
}
