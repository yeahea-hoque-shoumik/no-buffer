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

/** Global shield strength (Phase 20) — per-site [com.prime.nobuffer.data.entity.SiteShieldOverride] wins when set. */
enum class ShieldsMode { STANDARD, AGGRESSIVE, DISABLED }

data class BrowserSettings(
    val searchEngine: String = "DuckDuckGo",
    val homepageUrl: String = "",
    val downloadsLocation: String = "Downloads",
    val javaScriptEnabled: Boolean = true,
    val adBlockerEnabled: Boolean = true,
    val doNotTrackEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val safeBrowsingEnabled: Boolean = false,
    val searchSuggestionsEnabled: Boolean = false,
    val textZoom: Int = 100,
    val desktopSiteEnabled: Boolean = false,
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val locationPermission: Boolean = false,
    val micPermission: Boolean = false,
    val cameraPermission: Boolean = false,
    val notificationsPermission: Boolean = false,
    val popupsBlocked: Boolean = true,
    val autofillEnabled: Boolean = true,
    val antiFingerprintingEnabled: Boolean = true,
    val shieldsMode: ShieldsMode = ShieldsMode.STANDARD,
    /** -1 means "ask every time" (no persisted grant is written). */
    val permissionGrantTtlHours: Int = 24,
    val forceDarkPages: Boolean = true,
    /** JSON array of `{name, searchUrl, suggestUrl?}`. Selected engine name is [searchEngine]. */
    val customSearchEnginesJson: String = ""
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
        val ANTI_FINGERPRINTING = booleanPreferencesKey("anti_fingerprinting")
        val SHIELDS_MODE = stringPreferencesKey("shields_mode")
        val PERMISSION_GRANT_TTL_HOURS = intPreferencesKey("permission_grant_ttl_hours")
        val FORCE_DARK_PAGES = booleanPreferencesKey("force_dark_pages")
        val CUSTOM_SEARCH_ENGINES_JSON = stringPreferencesKey("custom_search_engines_json")
        val RECENTLY_CLOSED_JSON = stringPreferencesKey("recently_closed_json")
    }

    val settings: Flow<BrowserSettings> = context.dataStore.data.map { prefs ->
        BrowserSettings(
            searchEngine = prefs[Keys.SEARCH_ENGINE] ?: "DuckDuckGo",
            homepageUrl = prefs[Keys.HOMEPAGE_URL] ?: "",
            downloadsLocation = prefs[Keys.DOWNLOADS_LOCATION] ?: "Downloads",
            javaScriptEnabled = prefs[Keys.JS_ENABLED] ?: true,
            adBlockerEnabled = prefs[Keys.AD_BLOCKER] ?: true,
            doNotTrackEnabled = prefs[Keys.DNT] ?: true,
            blockThirdPartyCookies = prefs[Keys.BLOCK_THIRD_PARTY_COOKIES] ?: true,
            safeBrowsingEnabled = prefs[Keys.SAFE_BROWSING] ?: false,
            searchSuggestionsEnabled = false,
            textZoom = prefs[Keys.TEXT_ZOOM] ?: 100,
            desktopSiteEnabled = prefs[Keys.DESKTOP_SITE] ?: false,
            darkMode = prefs[Keys.DARK_MODE]?.let { runCatching { DarkModeOption.valueOf(it) }.getOrNull() } ?: DarkModeOption.SYSTEM,
            locationPermission = prefs[Keys.LOCATION_PERM] ?: false,
            micPermission = prefs[Keys.MIC_PERM] ?: false,
            cameraPermission = prefs[Keys.CAMERA_PERM] ?: false,
            notificationsPermission = prefs[Keys.NOTIFICATIONS_PERM] ?: false,
            popupsBlocked = prefs[Keys.POPUPS_BLOCKED] ?: true,
            autofillEnabled = prefs[Keys.AUTOFILL_ENABLED] ?: true,
            antiFingerprintingEnabled = prefs[Keys.ANTI_FINGERPRINTING] ?: true,
            shieldsMode = prefs[Keys.SHIELDS_MODE]?.let { runCatching { ShieldsMode.valueOf(it) }.getOrNull() } ?: ShieldsMode.STANDARD,
            permissionGrantTtlHours = prefs[Keys.PERMISSION_GRANT_TTL_HOURS] ?: 24,
            forceDarkPages = prefs[Keys.FORCE_DARK_PAGES] ?: true,
            customSearchEnginesJson = prefs[Keys.CUSTOM_SEARCH_ENGINES_JSON] ?: ""
        )
    }

    val recentlyClosedJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENTLY_CLOSED_JSON] ?: "[]"
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
    suspend fun setAntiFingerprintingEnabled(value: Boolean) = context.dataStore.edit { it[Keys.ANTI_FINGERPRINTING] = value }
    suspend fun setShieldsMode(value: ShieldsMode) = context.dataStore.edit { it[Keys.SHIELDS_MODE] = value.name }
    suspend fun setPermissionGrantTtlHours(value: Int) = context.dataStore.edit { it[Keys.PERMISSION_GRANT_TTL_HOURS] = value }
    suspend fun setForceDarkPages(value: Boolean) = context.dataStore.edit { it[Keys.FORCE_DARK_PAGES] = value }
    suspend fun setCustomSearchEnginesJson(value: String) = context.dataStore.edit { it[Keys.CUSTOM_SEARCH_ENGINES_JSON] = value }
    suspend fun setRecentlyClosedJson(value: String) = context.dataStore.edit { it[Keys.RECENTLY_CLOSED_JSON] = value }
}
