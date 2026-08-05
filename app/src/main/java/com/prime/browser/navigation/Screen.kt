package com.prime.browser.navigation

sealed class Screen(val route: String) {
    object Browser : Screen("browser")
    object TabSwitcher : Screen("tab_switcher")
    object Omnibox : Screen("omnibox/{currentUrl}") {
        fun withUrl(encodedUrl: String) = "omnibox/$encodedUrl"
    }
    object History : Screen("history")
    object Bookmarks : Screen("bookmarks")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object SettingsPrivacy : Screen("settings/privacy")
    object SettingsSite : Screen("settings/site")
}
