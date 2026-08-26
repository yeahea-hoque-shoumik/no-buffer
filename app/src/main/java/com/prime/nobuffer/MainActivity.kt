package com.prime.nobuffer

import android.Manifest
import android.app.Activity
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.prime.nobuffer.navigation.Screen
import com.prime.nobuffer.newtab.QuickAccessViewModel
import com.prime.nobuffer.settings.DarkModeOption
import com.prime.nobuffer.settings.SettingsViewModel
import com.prime.nobuffer.tabs.TabsViewModel
import com.prime.nobuffer.ui.screens.BookmarksScreen
import com.prime.nobuffer.ui.screens.BrowserMenuBottomSheet
import com.prime.nobuffer.ui.screens.BrowserScreen
import com.prime.nobuffer.ui.screens.DownloadsScreen
import com.prime.nobuffer.ui.screens.HistoryScreen
import com.prime.nobuffer.ui.screens.OmniboxScreen
import com.prime.nobuffer.ui.screens.SettingsPrivacyScreen
import com.prime.nobuffer.ui.screens.SettingsScreen
import com.prime.nobuffer.ui.screens.SettingsSiteScreen
import com.prime.nobuffer.ui.screens.TabSwitcherScreen
import com.prime.nobuffer.ui.theme.BrowserTheme
import com.prime.nobuffer.ui.theme.IncognitoTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null

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
            if (grants.values.all { it }) request.grant(request.resources) else request.deny()
        }
        pendingPermissionRequest = null

        pendingGeoCallback?.let { callback ->
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            callback.invoke(pendingGeoOrigin, granted, false)
        }
        pendingGeoCallback = null
        pendingGeoOrigin = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                BrowserNavHost(
                    onShowFileChooser = { callback, params -> showFileChooser(callback, params) },
                    onPermissionRequested = { request ->
                        handlePermissionRequest(request, settings.micPermission, settings.cameraPermission)
                    },
                    onGeolocationPermissionRequested = { origin, callback ->
                        handleGeolocationPermission(origin, callback, settings.locationPermission)
                    },
                    onInstallShortcut = { url, title -> installShortcut(url, title) },
                    onPrint = { webView -> printPage(webView) }
                )
            }
        }
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

    private fun handlePermissionRequest(request: PermissionRequest, micAllowed: Boolean, cameraAllowed: Boolean) {
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

        val androidPermissions = grantedResources.mapNotNull {
            when (it) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
                else -> null
            }
        }.toTypedArray()

        val allGranted = androidPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            request.grant(grantedResources.toTypedArray())
        } else {
            pendingPermissionRequest = request
            permissionLauncher.launch(androidPermissions)
        }
    }

    private fun handleGeolocationPermission(
        origin: String,
        callback: GeolocationPermissions.Callback,
        locationAllowed: Boolean
    ) {
        if (!locationAllowed) {
            callback.invoke(origin, false, false)
            return
        }
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fineGranted) {
            callback.invoke(origin, true, false)
        } else {
            pendingGeoCallback = callback
            pendingGeoOrigin = origin
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
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
    var menuVisible by remember { mutableStateOf(false) }
    var findInPageSignal by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(settings, tabs) {
        tabs.forEach { tab ->
            val webView = tab.webView ?: return@forEach
            webView.settings.javaScriptEnabled = settings.javaScriptEnabled
            webView.settings.textZoom = settings.textZoom
            webView.settings.userAgentString =
                if (settings.desktopSiteEnabled) BrowserWebView.DESKTOP_UA else BrowserWebView.CHROME_UA
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, settings.safeBrowsingEnabled)
            }
            webView.importantForAutofill =
                if (settings.autofillEnabled) View.IMPORTANT_FOR_AUTOFILL_YES else View.IMPORTANT_FOR_AUTOFILL_NO
        }
    }

    if (menuVisible) {
        BrowserMenuBottomSheet(
            url = activeTab?.url.orEmpty(),
            onDismiss = { menuVisible = false },
            onReload = {
                activeTab?.webView?.reload()
                menuVisible = false
            },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, activeTab?.url.orEmpty())
                }
                context.startActivity(Intent.createChooser(shareIntent, null))
                menuVisible = false
            },
            onPrint = {
                activeTab?.webView?.let(onPrint)
                menuVisible = false
            },
            onInstall = {
                activeTab?.let { onInstallShortcut(it.url, it.title) }
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
                activeTab?.let { quickAccessViewModel.addSite(it.url, it.title) }
                Toast.makeText(context, "Added to home page", Toast.LENGTH_SHORT).show()
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
            onSettings = {
                menuVisible = false
                navController.navigate(Screen.Settings.route)
            }
        )
    }

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
                            onUrlChanged = { url -> tabsViewModel.updateTabInfo(tab.id, url = url) },
                            onTitleChanged = { title -> tabsViewModel.updateTabInfo(tab.id, title = title) },
                            onOpenOmnibox = openOmnibox,
                            onOpenTabSwitcher = openTabSwitcher,
                            onOpenMenu = { menuVisible = true },
                            onExhausted = onCloseOrFinish,
                            onWebViewReady = { wv -> tabsViewModel.attachWebView(tab.id, wv) },
                            onShowFileChooser = onShowFileChooser,
                            onPermissionRequested = onPermissionRequested,
                            onGeolocationPermissionRequested = onGeolocationPermissionRequested
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
                onCloseTab = { tab -> tabsViewModel.closeTabWithUndo(tab.id) },
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
                onOpenSite = { navController.navigate(Screen.SettingsSite.route) }
            )
        }
        composable(Screen.SettingsPrivacy.route) {
            SettingsPrivacyScreen()
        }
        composable(Screen.SettingsSite.route) {
            SettingsSiteScreen()
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
