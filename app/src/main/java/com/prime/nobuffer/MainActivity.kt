package com.prime.nobuffer

import android.Manifest
import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.CookieManager
import android.widget.Toast
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.prime.nobuffer.browser.BrowserWebView
import com.prime.nobuffer.browser.BrowserWebViewClient
import com.prime.nobuffer.browser.WebViewAppearance
import com.prime.nobuffer.data.entity.PermissionType
import com.prime.nobuffer.navigation.Screen
import com.prime.nobuffer.newtab.QuickAccessViewModel
import com.prime.nobuffer.settings.DarkModeOption
import com.prime.nobuffer.settings.SettingsViewModel
import com.prime.nobuffer.shields.NavigationHeaders
import com.prime.nobuffer.shields.WebShieldsContext
import com.prime.nobuffer.tabs.BrowserTab
import com.prime.nobuffer.tabs.TabsViewModel
import com.prime.nobuffer.ui.screens.AppLockGate
import com.prime.nobuffer.ui.screens.BookmarksScreen
import com.prime.nobuffer.ui.screens.BrowserMenuBottomSheet
import com.prime.nobuffer.ui.screens.BrowserScreen
import com.prime.nobuffer.ui.screens.CookieInspectorScreen
import com.prime.nobuffer.ui.screens.DownloadsScreen
import com.prime.nobuffer.ui.screens.HistoryScreen
import com.prime.nobuffer.ui.screens.OmniboxScreen
import com.prime.nobuffer.ui.screens.ReaderArticle
import com.prime.nobuffer.ui.screens.ReaderMode
import com.prime.nobuffer.ui.screens.ReaderModeScreen
import com.prime.nobuffer.ui.screens.SettingsBlockedSitesScreen
import com.prime.nobuffer.ui.screens.SettingsPrivacyScreen
import com.prime.nobuffer.ui.screens.SettingsScreen
import com.prime.nobuffer.ui.screens.SettingsSiteScreen
import com.prime.nobuffer.ui.screens.ShieldsBottomSheet
import com.prime.nobuffer.ui.screens.SiteInfoBottomSheet
import com.prime.nobuffer.ui.screens.TabSwitcherScreen
import com.prime.nobuffer.ui.theme.BrowserTheme
import com.prime.nobuffer.ui.theme.IncognitoTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private var pendingOpenUrl by mutableStateOf<String?>(null)
    private var pendingOmniboxQuery by mutableStateOf<String?>(null)
    private var appLocked by mutableStateOf(false)

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingPermissionHost: String? = null
    private var pendingPermissionTypes: List<PermissionType> = emptyList()
    private var pendingPermissionTtlHours: Int = 24
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoHost: String? = null
    private var pendingGeoTtlHours: Int = 24

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else {
            null
        }
        filePathCallback?.onReceiveValue(uris ?: arrayOf())
        filePathCallback = null
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        pendingPermissionRequest?.let { request ->
            if (grants.values.all { it }) {
                request.grant(request.resources)
                pendingPermissionHost?.let { host -> persistPermissionGrants(host, pendingPermissionTypes, pendingPermissionTtlHours) }
            } else {
                request.deny()
            }
        }
        pendingPermissionRequest = null
        pendingPermissionHost = null
        pendingPermissionTypes = emptyList()

        pendingGeoCallback?.let { callback ->
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            callback.invoke(pendingGeoOrigin, granted, false)
            if (granted) pendingGeoHost?.let { host -> persistPermissionGrants(host, listOf(PermissionType.LOCATION), pendingGeoTtlHours) }
        }
        pendingGeoCallback = null
        pendingGeoOrigin = null
        pendingGeoHost = null
    }

    /** Phase 21 — persists a time-limited grant so [PermissionType] requests from [host] skip re-prompting until it expires. TTL < 0 means "ask every time" — nothing is persisted. */
    private fun persistPermissionGrants(host: String, types: List<PermissionType>, ttlHours: Int) {
        if (ttlHours < 0 || types.isEmpty()) return
        val repository = (application as BrowserApplication).repository
        val ttlMillis = ttlHours * 3_600_000L
        lifecycleScope.launch { types.forEach { repository.grantSitePermission(host, it, ttlMillis) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIncomingIntent(intent)
        appLocked = (application as BrowserApplication).appLockController.shouldLock()
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.darkMode) {
                DarkModeOption.SYSTEM -> systemDark
                DarkModeOption.LIGHT -> false
                DarkModeOption.DARK -> true
            }
            BrowserTheme(darkTheme = darkTheme) {
                if (appLocked) {
                    AppLockGate(
                        controller = (application as BrowserApplication).appLockController,
                        onUnlocked = { appLocked = false }
                    )
                    return@BrowserTheme
                }
                BrowserNavHost(
                    pendingOpenUrl = pendingOpenUrl,
                    onConsumePendingOpenUrl = { pendingOpenUrl = null },
                    pendingOmniboxQuery = pendingOmniboxQuery,
                    onConsumePendingOmniboxQuery = { pendingOmniboxQuery = null },
                    darkTheme = darkTheme,
                    onShowFileChooser = { callback, params -> showFileChooser(callback, params) },
                    onPermissionRequested = { request ->
                        handlePermissionRequest(request, settings.micPermission, settings.cameraPermission, settings.permissionGrantTtlHours)
                    },
                    onGeolocationPermissionRequested = { origin, callback ->
                        handleGeolocationPermission(origin, callback, settings.locationPermission, settings.permissionGrantTtlHours)
                    },
                    onInstallShortcut = { url, title -> installShortcut(url, title) },
                    onPrint = { webView -> printPage(webView) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        val controller = (application as BrowserApplication).appLockController
        controller.onActivityStop()
        appLocked = controller.shouldLock()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.dataString?.trim()?.takeIf { it.isNotBlank() }?.let { pendingOpenUrl = it }
            }
            Intent.ACTION_WEB_SEARCH -> {
                intent.getStringExtra(SearchManager.QUERY)?.trim()?.takeIf { it.isNotBlank() }?.let {
                    pendingOmniboxQuery = it
                }
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    pendingOpenUrl = text
                } else if (text.isNotBlank()) {
                    pendingOmniboxQuery = text
                }
            }
            else -> {
                if (intent.hasExtra(EXTRA_QUERY)) {
                    pendingOmniboxQuery = intent.getStringExtra(EXTRA_QUERY).orEmpty()
                }
            }
        }
    }

    companion object {
        const val EXTRA_QUERY = "com.prime.nobuffer.EXTRA_QUERY"
    }

    private fun showFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        return try {
            fileChooserLauncher.launch(params.createIntent())
            true
        } catch (e: Exception) {
            filePathCallback = null
            false
        }
    }

    private fun toPermissionType(resource: String): PermissionType? = when (resource) {
        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> PermissionType.MICROPHONE
        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> PermissionType.CAMERA
        else -> null
    }

    // Phase 21: checks for a non-expired per-site grant before falling back to the Site Settings
    // gate + Android runtime prompt.
    private fun handlePermissionRequest(request: PermissionRequest, micAllowed: Boolean, cameraAllowed: Boolean, ttlHours: Int) {
        val repository = (application as BrowserApplication).repository
        val host = request.origin.host
        if (host == null) {
            request.deny()
            return
        }
        // Only resources whose master Site Settings toggle is on are eligible at all — a stored
        // grant must never bypass that toggle if the user has since turned it off.
        val grantedResources = request.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> micAllowed
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> cameraAllowed
                else -> false
            }
        }
        if (grantedResources.isEmpty()) {
            request.deny()
            return
        }
        val eligibleTypes = grantedResources.mapNotNull(::toPermissionType)

        lifecycleScope.launch {
            val hasActiveGrant = eligibleTypes.isNotEmpty() && eligibleTypes.all { repository.findActivePermissionGrant(host, it) != null }
            if (hasActiveGrant) {
                request.grant(grantedResources.toTypedArray())
                return@launch
            }

            val androidPermissions = grantedResources.mapNotNull {
                when (it) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
                    else -> null
                }
            }.toTypedArray()

            val allGranted = androidPermissions.all {
                ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                request.grant(grantedResources.toTypedArray())
                persistPermissionGrants(host, eligibleTypes, ttlHours)
            } else {
                pendingPermissionRequest = request
                pendingPermissionHost = host
                pendingPermissionTypes = eligibleTypes
                pendingPermissionTtlHours = ttlHours
                permissionLauncher.launch(androidPermissions)
            }
        }
    }

    private fun handleGeolocationPermission(
        origin: String,
        callback: GeolocationPermissions.Callback,
        locationAllowed: Boolean,
        ttlHours: Int
    ) {
        if (!locationAllowed) {
            callback.invoke(origin, false, false)
            return
        }
        val repository = (application as BrowserApplication).repository
        val host = runCatching { Uri.parse(origin).host }.getOrNull()
        if (host == null) {
            callback.invoke(origin, false, false)
            return
        }

        lifecycleScope.launch {
            if (repository.findActivePermissionGrant(host, PermissionType.LOCATION) != null) {
                callback.invoke(origin, true, false)
                return@launch
            }

            val fineGranted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (fineGranted) {
                callback.invoke(origin, true, false)
                persistPermissionGrants(host, listOf(PermissionType.LOCATION), ttlHours)
            } else {
                pendingGeoCallback = callback
                pendingGeoOrigin = origin
                pendingGeoHost = host
                pendingGeoTtlHours = ttlHours
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }
    }

    private fun installShortcut(url: String, title: String) {
        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        if (!shortcutManager.isRequestPinShortcutSupported) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val shortcut = ShortcutInfo.Builder(this, url)
            .setShortLabel(title.ifBlank { url })
            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
        shortcutManager.requestPinShortcut(shortcut, null)
    }

    private fun printPage(webView: android.webkit.WebView) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = webView.createPrintDocumentAdapter("Orion Document")
        printManager.print("Orion Document", adapter, PrintAttributes.Builder().build())
    }
}

@Composable
fun BrowserNavHost(
    navController: NavHostController = rememberNavController(),
    pendingOpenUrl: String? = null,
    onConsumePendingOpenUrl: () -> Unit = {},
    pendingOmniboxQuery: String? = null,
    onConsumePendingOmniboxQuery: () -> Unit = {},
    darkTheme: Boolean = false,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean = { _, _ -> false },
    onPermissionRequested: (PermissionRequest) -> Unit = { it.deny() },
    onGeolocationPermissionRequested: (String, GeolocationPermissions.Callback) -> Unit = { _, callback -> callback.invoke(null, false, false) },
    onInstallShortcut: (String, String) -> Unit = { _, _ -> },
    onPrint: (android.webkit.WebView) -> Unit = {}
) {
    val tabsViewModel: TabsViewModel = viewModel()
    val tabs by tabsViewModel.tabs.collectAsStateWithLifecycle()
    val activeTab by tabsViewModel.activeTabFlow.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = viewModel()
    val quickAccessViewModel: QuickAccessViewModel = viewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as BrowserApplication
    val coroutineScope = rememberCoroutineScope()
    var menuVisible by remember { mutableStateOf(false) }
    var findInPageSignal by remember { mutableIntStateOf(0) }
    var elementPickerActive by remember { mutableStateOf(false) }
    var shieldsSheetVisible by remember { mutableStateOf(false) }
    var siteInfoVisible by remember { mutableStateOf(false) }
    var cookieInspectorVisible by remember { mutableStateOf(false) }
    var readerArticle by remember { mutableStateOf<ReaderArticle?>(null) }

    fun loadInActiveTab(url: String) {
        val tab = tabsViewModel.activeTab ?: return
        tabsViewModel.updateTabInfo(tab.id, url = url)
        val webView = tab.webView ?: return
        val client = webView.webViewClient as? BrowserWebViewClient
        val resolved = client?.resolveNavigationUrl(url) ?: url
        webView.loadUrl(resolved, client?.currentNavigationHeaders() ?: emptyMap())
    }

    fun applyZoom(value: Int) {
        val next = value.coerceIn(50, 200)
        settingsViewModel.setTextZoom(next)
        tabsViewModel.activeTab?.webView?.settings?.textZoom = next
    }

    // Phase 17-19 — single lambda bundle threaded down to every BrowserWebViewClient instance.
    val shields = remember(settings) {
        WebShieldsContext(
            effectiveShields = { host -> app.shieldsResolver.effectiveShields(host) },
            isHostBlocked = { host -> app.adTrackerBlocklist.isBlocked(host) || app.cosmeticRuleStore.isDomainBlocked(host) },
            cosmeticSelectors = { host -> app.cosmeticRuleStore.selectorsFor(host) },
            navigationHeaders = {
                NavigationHeaders.build(
                    doNotTrack = settings.doNotTrackEnabled,
                    globalPrivacyControl = settings.antiFingerprintingEnabled
                )
            },
            httpsUpgradeEnabled = true,
            trackingParamStrippingEnabled = true,
            redirectorUnwrapEnabled = true,
            deAmpEnabled = true,
            isVideoAllowed = { host -> app.siteBlocker.isVideoAllowed(host.orEmpty()) }
        )
    }

    fun hostOfTab(tab: com.prime.nobuffer.tabs.BrowserTab?): String =
        runCatching { Uri.parse(tab?.url.orEmpty()).host }.getOrNull().orEmpty()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, tabsViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    tabsViewModel.tabs.value.forEach { it.webView?.onPause() }
                    tabsViewModel.persistTabs()
                }
                Lifecycle.Event.ON_RESUME -> tabsViewModel.activeTab?.webView?.onResume()
                Lifecycle.Event.ON_DESTROY -> tabsViewModel.tabs.value.forEach { it.webView?.destroy() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings, tabs, darkTheme) {
        tabs.forEach { tab ->
            val webView = tab.webView ?: return@forEach
            webView.settings.javaScriptEnabled = app.shieldsResolver.effectiveShields(hostOfTab(tab)).scriptsEnabled
            webView.settings.textZoom = settings.textZoom
            val desktop = tab.desktopSite ?: settings.desktopSiteEnabled
            webView.settings.userAgentString =
                if (desktop) BrowserWebView.DESKTOP_UA else BrowserWebView.CHROME_UA
            if (tab.isIncognito) {
                webView.enableIncognitoIsolation()
            } else {
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies)
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                    WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, settings.safeBrowsingEnabled)
                }
                webView.importantForAutofill =
                    if (settings.autofillEnabled) View.IMPORTANT_FOR_AUTOFILL_YES else View.IMPORTANT_FOR_AUTOFILL_NO
            }
            WebViewAppearance.applyForceDark(webView, darkTheme && settings.forceDarkPages)
        }
    }

    LaunchedEffect(pendingOpenUrl, tabs.size) {
        val url = pendingOpenUrl ?: return@LaunchedEffect
        if (tabs.isEmpty()) return@LaunchedEffect
        onConsumePendingOpenUrl()
        val active = tabsViewModel.activeTab
        if (tabs.size == 1 && active != null && (active.url.isBlank() || active.url == "about:blank")) {
            loadInActiveTab(url)
        } else {
            tabsViewModel.newTab(url)
        }
        if (navController.currentDestination?.route != Screen.Browser.route) {
            navController.popBackStack(Screen.Browser.route, inclusive = false)
        }
    }

    LaunchedEffect(activeTab?.id, activeTab?.url) {
        val url = activeTab?.url ?: return@LaunchedEffect
        if (url.isBlank() || url == "about:blank") return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(30_000)
            app.siteBlocker.recordUsage(url, 30_000L)
        }
    }

    LaunchedEffect(pendingOmniboxQuery) {
        val query = pendingOmniboxQuery ?: return@LaunchedEffect
        onConsumePendingOmniboxQuery()
        val encoded = URLEncoder.encode(query.ifBlank { "about:blank" }, "UTF-8")
        if (navController.currentDestination?.route != Screen.Browser.route) {
            navController.popBackStack(Screen.Browser.route, inclusive = false)
        }
        navController.navigate(Screen.Omnibox.withUrl(encoded))
    }

    if (menuVisible) {
        BrowserMenuBottomSheet(
            url = activeTab?.url.orEmpty(),
            textZoom = settings.textZoom,
            desktopSiteEnabled = activeTab?.desktopSite ?: settings.desktopSiteEnabled,
            onDismiss = { menuVisible = false },
            onReload = {
                activeTab?.webView?.reload()
                menuVisible = false
            },
            onShare = {
                val shareUrl = activeTab?.url.orEmpty()
                if (app.siteBlocker.isUrlBlocked(shareUrl)) {
                    Toast.makeText(context, "Blocked sites can't be shared", Toast.LENGTH_SHORT).show()
                } else {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
                menuVisible = false
            },
            onPrint = {
                activeTab?.webView?.let(onPrint)
                menuVisible = false
            },
            onInstall = {
                val tab = activeTab
                when {
                    tab == null -> Unit
                    tab.isIncognito ->
                        Toast.makeText(context, "Can't add private tabs to the home screen", Toast.LENGTH_SHORT).show()
                    app.siteBlocker.isUrlBlocked(tab.url) ->
                        Toast.makeText(context, "Blocked sites can't be added to the home screen", Toast.LENGTH_SHORT).show()
                    else -> onInstallShortcut(tab.url, tab.title)
                }
                menuVisible = false
            },
            onNewTab = {
                tabsViewModel.newTab("about:blank")
                menuVisible = false
            },
            onNewPrivateTab = {
                tabsViewModel.newTab("about:blank", isIncognito = true)
                menuVisible = false
            },
            onAddToHomePage = {
                val tab = activeTab
                if (tab == null || tab.isIncognito) {
                    Toast.makeText(context, "Can't pin private tabs to the home page", Toast.LENGTH_SHORT).show()
                } else {
                    quickAccessViewModel.addSite(tab.url, tab.title)
                    Toast.makeText(context, "Added to home page", Toast.LENGTH_SHORT).show()
                }
                menuVisible = false
            },
            onHistory = {
                menuVisible = false
                navController.navigate(Screen.History.route)
            },
            onDownloads = {
                menuVisible = false
                navController.navigate(Screen.Downloads.route)
            },
            onFindOnPage = {
                menuVisible = false
                findInPageSignal++
            },
            onBlockElement = {
                menuVisible = false
                elementPickerActive = true
            },
            onSettings = {
                menuVisible = false
                navController.navigate(Screen.Settings.route)
            },
            onCopyLink = {
                val copyUrl = activeTab?.url.orEmpty()
                when {
                    copyUrl.isBlank() || copyUrl == "about:blank" ->
                        Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                    app.siteBlocker.isUrlBlocked(copyUrl) ->
                        Toast.makeText(context, "Blocked sites can't be copied", Toast.LENGTH_SHORT).show()
                    else -> {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("link", copyUrl))
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    }
                }
                menuVisible = false
            },
            onZoomDelta = { delta -> applyZoom(settings.textZoom + delta) },
            onZoomSet = { value -> applyZoom(value) },
            onBookmarks = {
                menuVisible = false
                navController.navigate(Screen.Bookmarks.route)
            },
            onRestoreRecentlyClosed = {
                menuVisible = false
                val restored = tabsViewModel.restoreRecentlyClosed()
                if (restored == null) {
                    Toast.makeText(context, "No recently closed tabs", Toast.LENGTH_SHORT).show()
                }
            },
            onReaderMode = {
                menuVisible = false
                if (readerArticle != null) {
                    readerArticle = null
                } else {
                    val pageUrl = activeTab?.url.orEmpty()
                    val webView = activeTab?.webView
                    if (webView == null || pageUrl.isBlank() || pageUrl == "about:blank") {
                        Toast.makeText(context, "No page to read", Toast.LENGTH_SHORT).show()
                    } else {
                        webView.evaluateJavascript(ReaderMode.EXTRACT_ARTICLE_JS) { raw ->
                            val parsed = ReaderMode.parse(raw)
                            if (parsed == null || parsed.body.isBlank()) {
                                Toast.makeText(context, "Couldn't extract article", Toast.LENGTH_SHORT).show()
                            } else {
                                readerArticle = parsed
                            }
                        }
                    }
                }
            },
            onLockSite = {
                menuVisible = false
                val pageUrl = activeTab?.url.orEmpty()
                if (pageUrl.isBlank() || pageUrl == "about:blank") {
                    Toast.makeText(context, "Nothing to lock", Toast.LENGTH_SHORT).show()
                } else {
                    coroutineScope.launch {
                        val error = app.siteBlocker.addSite(pageUrl)
                        Toast.makeText(context, error ?: "Site locked", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSiteInfo = {
                menuVisible = false
                siteInfoVisible = true
            },
            onDesktopSite = {
                val tab = activeTab
                if (tab != null) {
                    val enabled = !(tab.desktopSite ?: settings.desktopSiteEnabled)
                    tabsViewModel.updateDesktopSite(tab.id, enabled)
                    tab.webView?.settings?.userAgentString =
                        if (enabled) BrowserWebView.DESKTOP_UA else BrowserWebView.CHROME_UA
                    tab.webView?.reload()
                }
                menuVisible = false
            }
        )
    }

    if (shieldsSheetVisible) {
        val host = hostOfTab(activeTab)
        val effective = app.shieldsResolver.effectiveShields(host)
        val override = app.shieldsResolver.currentOverride(host)
        ShieldsBottomSheet(
            host = host,
            effective = effective,
            hasOverride = override != null,
            onDismiss = { shieldsSheetVisible = false },
            onSetAdBlock = { value ->
                coroutineScope.launch {
                    app.shieldsResolver.setOverride(host, value, effective.trackerBlockEnabled, effective.scriptsEnabled, effective.fingerprintProtectionEnabled)
                }
            },
            onSetTrackerBlock = { value ->
                coroutineScope.launch {
                    app.shieldsResolver.setOverride(host, effective.adBlockEnabled, value, effective.scriptsEnabled, effective.fingerprintProtectionEnabled)
                }
            },
            onSetScriptsEnabled = { value ->
                coroutineScope.launch {
                    app.shieldsResolver.setOverride(host, effective.adBlockEnabled, effective.trackerBlockEnabled, value, effective.fingerprintProtectionEnabled)
                }
                activeTab?.webView?.settings?.javaScriptEnabled = value
            },
            onSetFingerprintProtection = { value ->
                coroutineScope.launch {
                    app.shieldsResolver.setOverride(host, effective.adBlockEnabled, effective.trackerBlockEnabled, effective.scriptsEnabled, value)
                }
                activeTab?.webView?.setFingerprintProtectionEnabled(value)
            },
            onResetToDefault = {
                coroutineScope.launch { app.shieldsResolver.clearOverride(host) }
            }
        )
    }

    if (siteInfoVisible) {
        SiteInfoBottomSheet(
            url = activeTab?.url.orEmpty(),
            javaScriptEnabled = activeTab?.webView?.settings?.javaScriptEnabled ?: settings.javaScriptEnabled,
            onDismiss = { siteInfoVisible = false },
            onViewCookies = {
                siteInfoVisible = false
                cookieInspectorVisible = true
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Browser.route
    ) {
        composable(Screen.Browser.route) {
            val tab = activeTab
            if (tab == null) {
                PlaceholderScreen("Loading")
            } else {
                key(tab.id) {
                    val openOmnibox: () -> Unit = {
                        val encoded = URLEncoder.encode(tab.url, "UTF-8")
                        navController.navigate(Screen.Omnibox.withUrl(encoded))
                    }
                    val openTabSwitcher: () -> Unit = { navController.navigate(Screen.TabSwitcher.route) }
                    val onCloseOrFinish: () -> Unit = {
                        if (tabs.size > 1) {
                            tabsViewModel.closeTab(tab.id)
                        } else {
                            (context as? Activity)?.finish()
                        }
                    }

                    val screenContent: @Composable () -> Unit = {
                        BrowserScreen(
                            startUrl = tab.url,
                            tabCount = tabs.size,
                            isIncognito = tab.isIncognito,
                            webView = tab.webView,
                            findInPageSignal = findInPageSignal,
                            blockedCount = tab.blockedCount,
                            shields = shields,
                            elementPickerActive = elementPickerActive,
                            onUrlChanged = { url -> tabsViewModel.updateTabInfo(tab.id, url = url) },
                            onTitleChanged = { title -> tabsViewModel.updateTabInfo(tab.id, title = title) },
                            onOpenOmnibox = openOmnibox,
                            onOpenTabSwitcher = openTabSwitcher,
                            onOpenMenu = { menuVisible = true },
                            onOpenShields = { shieldsSheetVisible = true },
                            onExhausted = onCloseOrFinish,
                            onWebViewReady = { wv -> tabsViewModel.attachWebView(tab.id, wv) },
                            onRequestBlocked = { tabsViewModel.incrementBlockedCount(tab.id) },
                            onElementPicked = { selector, webView ->
                                elementPickerActive = false
                                val host = hostOfTab(tab)
                                if (host.isNotBlank()) {
                                    coroutineScope.launch {
                                        app.cosmeticRuleStore.addSiteCosmeticRule(host, selector)
                                    }
                                    webView.evaluateJavascript(
                                        "(function(){document.querySelectorAll('${selector.replace("'", "\\'")}').forEach(function(el){el.style.setProperty('display','none','important');});})();",
                                        null
                                    )
                                }
                            },
                            onCancelElementPicker = { elementPickerActive = false },
                            onShowFileChooser = onShowFileChooser,
                            onPermissionRequested = onPermissionRequested,
                            onGeolocationPermissionRequested = onGeolocationPermissionRequested,
                            onOpenInNewTab = { url -> tabsViewModel.newTab(url, isIncognito = tab.isIncognito) }
                        )
                    }

                    if (tab.isIncognito) {
                        IncognitoTheme { screenContent() }
                    } else {
                        screenContent()
                    }
                }
            }
        }
        composable(Screen.TabSwitcher.route) {
            val pendingClose by tabsViewModel.pendingClose.collectAsStateWithLifecycle()
            TabSwitcherScreen(
                tabs = tabs,
                activeTabId = activeTab?.id,
                onSelectTab = { tab ->
                    val index = tabs.indexOfFirst { it.id == tab.id }
                    if (index >= 0) tabsViewModel.switchTab(index)
                    navController.popBackStack()
                },
                onCloseTab = { tab ->
                    tabsViewModel.closeTabWithUndo(tab.id)
                },
                onNewTab = {
                    tabsViewModel.newTab("about:blank")
                    navController.popBackStack()
                },
                onNewPrivateTab = {
                    tabsViewModel.newTab("about:blank", isIncognito = true)
                    navController.popBackStack()
                },
                onDone = { navController.popBackStack() },
                pendingClosedTab = pendingClose?.tab,
                onUndoClose = { tabsViewModel.undoTabClose() }
            )
        }
        composable(
            Screen.Omnibox.route,
            arguments = listOf(navArgument("currentUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("currentUrl").orEmpty()
            val currentUrl = URLDecoder.decode(encoded, "UTF-8")
            OmniboxScreen(
                initialUrl = currentUrl,
                onNavigate = { url ->
                    activeTab?.let { tabsViewModel.updateTabInfo(it.id, url = url) }
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigate = { url ->
                    activeTab?.let { tabsViewModel.updateTabInfo(it.id, url = url) }
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                onNavigate = { url ->
                    activeTab?.let { tabsViewModel.updateTabInfo(it.id, url = url) }
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Downloads.route) {
            DownloadsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onOpenPrivacy = { navController.navigate(Screen.SettingsPrivacy.route) },
                onOpenSite = { navController.navigate(Screen.SettingsSite.route) },
                onOpenBlockedSites = { navController.navigate(Screen.SettingsBlockedSites.route) },
                onOpenBookmarks = { navController.navigate(Screen.Bookmarks.route) }
            )
        }
        composable(Screen.SettingsPrivacy.route) {
            SettingsPrivacyScreen()
        }
        composable(Screen.SettingsSite.route) {
            SettingsSiteScreen()
        }
        composable(Screen.SettingsBlockedSites.route) {
            SettingsBlockedSitesScreen()
        }
    }

        readerArticle?.let { article ->
            ReaderModeScreen(
                article = article,
                onClose = { readerArticle = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (cookieInspectorVisible) {
            CookieInspectorScreen(
                url = activeTab?.url.orEmpty(),
                onClose = { cookieInspectorVisible = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
