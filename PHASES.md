# PHASES.md — NoBuffer App Development Checklist

Chrome UI/UX Android browser (**NoBuffer**, `com.prime.nobuffer`) built in Kotlin + Jetpack Compose.
Cross-reference `PLAN.md` for full design specs per screen.

> All UI is Compose. Translate any Views/XML references in PLAN.md to Compose equivalents.

---

## Phase 1 — Project Foundation ✅

- [x] Add all required dependencies to `gradle/libs.versions.toml`:
  - [x] `androidx.navigation:navigation-compose` (`2.9.0`)
  - [x] `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (via KSP)
  - [x] `com.google.devtools.ksp` plugin (`2.2.10-1.0.31`)
  - [x] `androidx.lifecycle:lifecycle-viewmodel-compose`
  - [x] `io.coil-kt:coil-compose` (`2.7.0`)
  - [x] `androidx.datastore:datastore-preferences` (`1.1.3`)
- [x] Apply KSP plugin in `app/build.gradle.kts`; remove any KAPT references
- [x] Populate `ui/theme/Color.kt` with Chrome design tokens (light + dark + incognito palettes from `PLAN.md`)
- [x] Populate `ui/theme/Theme.kt` with `BrowserTheme` + `IncognitoTheme` MaterialTheme wrappers
- [x] Populate `ui/theme/Type.kt` with Chrome typography scale (Roboto / Google Sans)
- [x] Add `INTERNET` and `DOWNLOAD_WITHOUT_NOTIFICATION` permissions to `AndroidManifest.xml`
- [x] Set up `NavHost` in `MainActivity` with a sealed `Screen` route class covering all screens
- [x] App rebranded to **NoBuffer** (`com.prime.nobuffer`) — package, display name, and adaptive launcher icon (`drawable/nobuffer_icon.png` as `ic_launcher_foreground`, wrapped in an 18dp `<inset>` so the mask doesn't crop/zoom the artwork; plain white `ic_launcher_background`)

---

## Phase 2 — WebView Engine + Video Blocking ✅

- [x] Create `BrowserWebView.kt` — `WebView` subclass with baseline settings (JS, DOM storage, zoom, `mediaPlaybackRequiresUserGesture = true`, Chrome UA)
- [x] Create `BrowserWebViewClient.kt`:
  - [x] Layer 1: `shouldInterceptRequest` — block video URLs by extension, MIME, and known CDN patterns (`googlevideo.com`, `/videoplayback`, `.m3u8`, `.mpd`)
  - [x] Layer 2: `onPageFinished` — inject JS that removes `<video>`/`<source>` elements and attaches `MutationObserver`
  - [x] `onReceivedSslError` — show Material dialog; never auto-proceed
- [x] Create `BrowserWebChromeClient.kt`:
  - [x] Layer 3: `onShowCustomView` — call `callback.onCustomViewHidden()` immediately
  - [x] Wire `onProgressChanged`, `onReceivedTitle`, `onReceivedIcon` callbacks
- [x] Wrap `BrowserWebView` in an `AndroidView` composable (`BrowserWebViewComposable`)
- [x] Apply security hardening: `allowFileAccess = true`, `allowContentAccess = true` (local `file://`/`content://` resource loading — safe since Layer 1 `shouldInterceptRequest` and the Layer 2 JS strip are scheme-agnostic and still block video regardless of origin; `allowFileAccessFromFileURLs`/`allowUniversalAccessFromFileURLs` left at their default `false`), `WebView.startSafeBrowsing()`
- [x] Apply `WebSettingsCompat.FORCE_DARK_AUTO` for system dark-mode support

---

## Phase 3 — Data Layer (Room) ✅

- [x] Create `HistoryEntry` entity and `HistoryDao` (insert, delete, clearAll, search, `Flow<List>` observe)
- [x] Create `Bookmark` entity and `BookmarkDao` (insert, delete, search, isBookmarked, `Flow<List>` observe)
- [x] Create `BrowserDatabase` (`RoomDatabase`) with both DAOs
- [x] Create `BrowserRepository` wrapping both DAOs with coroutine-friendly suspend functions
- [x] Provide database singleton via `Application` subclass or dependency injection

---

## Phase 4 — Tab Management ✅

- [x] Create `BrowserTab` data class (`id`, `webView`, `url`, `title`, `favicon`, `isIncognito`, `snapshotBitmap`)
- [x] Create `TabManager` — manages list of `BrowserTab`, `activeIndex`, `newTab()`, `closeTab()`, `captureSnapshot()`
- [x] Create `TabCountBadge` composable — custom Canvas drawing of rounded-square with count number (mirrors Chrome badge)
- [x] Expose tab list and active tab as `StateFlow` from a `TabsViewModel`
- [x] Tab persistence across app restart: `TabEntity`/`TabDao` (Room `tabs` table, `BrowserDatabase` v2) store `id`/`url`/`title`/`position`/`isActive` for non-incognito tabs; `TabsViewModel.init` restores them via `TabManager.restoreTabs()` on launch (falling back to a blank tab if nothing was saved) and `persistTabs()` writes them back on `ON_PAUSE`; `BrowserTab.webView` is nullable so a restored tab is metadata-only until switched to — `BrowserScreen`'s `onWebViewReady` → `TabsViewModel.attachWebView()` only creates/loads the real `BrowserWebView` when that tab is actually rendered

---

## Design Reference — Orion Browser Hi-Fi

> All phases 5–12 implement the **Orion Browser Hi-Fi** design (`design/project/Orion Browser Hi-Fi.html`).
> Design chat context: `design/chats/chat1.md`.

### Design System (from Hi-Fi)

**Font:** `DM Sans` (300/400/500/600/700) — add via `@font-face` / bundled asset or Google Fonts fallback.

**Dark theme tokens (primary):**
| Token | Value |
|-------|-------|
| `bg` | `#08080F` |
| `surface` | `#101018` |
| `elevated` | `#17172280` (frosted glass) |
| `border` | `#252538` |
| `borderHi` | `#35355a` |
| `accent` | `#7B6EF5` (purple/violet) |
| `accentGlow` | `#7B6EF540` |
| `teal` | `#36C9B0` (security/success) |
| `text` | `#F0EFF8` |
| `textMid` | `#9994B8` |
| `textDim` | `#504E68` |
| `pill` | `#14142080` |

**Light theme tokens:**
| Token | Value |
|-------|-------|
| `bg` | `#F4F3F8` |
| `surface` | `#FFFFFF` |
| `elevated` | `#ECE9F8` |
| `border` | `#E0DCF3` |
| `borderHi` | `#C8C2EE` |
| `accent` | `#6B5EE4` |
| `teal` | `#1DB89D` |
| `text` | `#14122A` |
| `textMid` | `#6B6585` |
| `textDim` | `#BAAFCC` |
| `pill` | `#FFFFFFD0` |

**Orion Icon (SVG):** geometric orbit ring — outer circle (r=17, 40% opacity) + tilted ellipse (rx=17 ry=6.5 rotate=-32°) + filled core dot (r=4.5 accent) + white highlight (r=2, 70% opacity). Phone frame: 390×844dp, border-radius 54dp.

---

## Phase 5 — Main Browser Screen (Browsing + PillBar) ✅

> Design ref: `BrowsingScreen` + `PillBar` in Orion Hi-Fi.

- [x] Build `OrionTokens` object in `ui/theme/OrionTokens.kt` with all dark/light token values above
- [x] Build `OrionIcon` composable (Canvas or `Image` from SVG asset) — orbit-ring icon, size param, glow drop-shadow option
- [x] Build `PillBar` composable (top navigation bar):
  - [x] Outer pill: `background pill token`, `border borderHi`, `borderRadius 28dp`, padding `8dp 12dp` (note: true backdrop blur skipped — Compose has no backdrop-filter API; approximated with translucent pill token)
  - [x] Optional back chevron button (color `textMid`, 20×20dp icon) — shown only when `showBack = true`
  - [x] Inner URL/search field (flex-1): `borderRadius 20dp`
    - URL mode: `background elevated`, teal 🔒 lock icon (13×13dp) + host text (14sp `textMid`, overflow ellipsis)
    - Search mode: `background accent×18`, accent search icon + placeholder text (14sp `textDim`)
  - [x] Right side: 4-square grid icon button (20×20dp, `textMid`) + 3-dot overflow button
  - [x] Outer container: `padding 8dp 16dp 0` (top of content area below StatusBar)
- [x] Build `StatusBar` composable (44dp height): time (15sp bold) + signal bars + WiFi + battery icons
- [x] Build `HomeIndicator` composable: 134×5dp rounded pill, 25% opacity, centered, 34dp height container
- [x] Build `BrowserScreen` composable:
  - [x] `StatusBar` → `PillBar(url=currentUrl, showBack=canGoBack)` → `BrowserWebViewComposable` (fills remaining space) → `HomeIndicator`
  - [x] No separate TopAppBar — PillBar replaces it entirely
  - [x] Thin `LinearProgressIndicator` (3dp, accent color) between PillBar and WebView — visible during load
  - [x] Page background: dark bg token, light `#FFFFFF`
- [x] Implement back/forward from `WebView.canGoBack()` / `canGoForward()` — back shown in PillBar when available
- [x] Teal lock icon for HTTPS; warning icon (amber) for HTTP in PillBar inner field
- [x] Build `FindInPageBar` composable (52dp bottom bar): query input, match count `X/Y`, prev/next/close; `background surface`, `border border`

---

## Phase 6 — Address Bar / Omnibox Overlay (Screen A2) ✅

> Design ref: `AddressScreen` in Orion Hi-Fi.

- [x] Build `OmniboxScreen` (full-screen `NavHost` destination):
  - [x] `StatusBar` at top
  - [x] Active search bar row (below status bar): search field (`background surface`, `border 1.5dp accent`, `borderRadius 26dp`) + accent search glyph + text field (15sp `text`) + "Cancel" button (15sp, 600w, accent color)
  - [x] Suggestion list (`LazyColumn`): each row 36×36dp icon tile (`surface`, `border`, `borderRadius 10dp`) + suggestion text (15sp `text`) + tag label (11sp, 600w) — tag colors: recent=`accent`, bookmark=`teal`, search=`textDim`
  - [x] On-screen keyboard: system IME via focus request on `BasicTextField` (note: no slide-up nav transition / custom keyboard row shadows — out of scope for Compose default transitions)
- [x] Pre-fill current URL on open, select all text
- [x] Debounced input (150ms) triggers DB search (bookmarks + history) + prepend search suggestion row
- [x] `IME_ACTION_GO` navigates; row tap navigates
- [x] `OmniboxViewModel` — `suggestions: StateFlow<List<OmniboxSuggestion>>` (type: recent/bookmark/search)
- [x] Long-URL editing fixed: migrated the URL field from the classic `BasicTextField(value: TextFieldValue, ...)` overload to the `TextFieldState`-based overload (Compose Foundation 1.11.3) with `lineLimits = TextFieldLineLimits.SingleLine` — its built-in `ScrollState` handles horizontal scroll-to-cursor internally, so the whole URL (and the cursor) is reachable however long the string is; an earlier fix wrapping the field in an external `Modifier.horizontalScroll` container was reverted because its drag gesture conflicted with the field's own selection-drag gesture and broke multi-character cursor movement

---

## Phase 7 — New Tab / Home Page (Screen A3) ✅

> Design ref: `HomeScreen` in Orion Hi-Fi.

- [x] Build `NewTabPage` composable:
  - [x] Full background `bg` token
  - [x] Radial ambient glow: `260×260dp` circle, `background radial-gradient(accentGlow → transparent)`, centered ~60dp from top, decorative
  - [x] `StatusBar` → `PillBar(url=null)` (search mode, no back button) → content
  - [x] Orion logo row: `OrionIcon(28dp, glow=true)` + "orion" wordmark (15sp, 700w) + date string (13sp `textDim`)
  - [x] **Quick Access section**: section label (11sp, 600w, `textDim`), `LazyVerticalGrid(4 columns)` of site tiles: 52×52dp `borderRadius 16dp` `background color×18` `border color×30`; letter avatar centered; label below (11sp `textMid`)
  - [x] Horizontal divider (`1dp border token`)
  - [x] **Recent section**: rows `borderRadius 14dp` `background elevated` `border border`; 34×34dp color-dot icon tile + title (14sp 500w) + relative time string (11sp `textDim`), backed by `NewTabViewModel` (recent history)
  - [x] `HomeIndicator` at bottom
- [x] Show `NewTabPage` when active tab URL is `about:blank` or `about:newtab`
- [x] Tapping PillBar opens `OmniboxScreen`
- [x] Long-press Quick Access tile → `DropdownMenu` (Open / Open in New Tab / Remove / Edit)

---

## Phase 8 — Tab Switcher (Screen A4) ✅

> Design ref: `TabsScreen` in Orion Hi-Fi.

- [x] Build `TabSwitcherScreen` composable (full-screen):
  - [x] `StatusBar` at top
  - [x] Header row: tab count label "N Tabs" (22sp, 700w) + "+ New" button (`background elevated`, `border border`, `borderRadius 20dp`, accent text, 14sp 600w)
  - [x] Tab grid: `LazyVerticalGrid(2 columns)`
  - [x] `TabCard` (168dp height, `borderRadius 18dp`, `background surface`):
    - Active: `border 2dp accent`; Inactive: `border 1dp border`
    - Card header (`background elevated`): favicon tile (color dot) + host text (11sp `textMid`, ellipsis) + ✕ close button
    - Preview area: skeleton lines + color block
  - [x] "+ New Tab" slot: dashed border (Canvas `dashPathEffect`), centered 36dp circle with + icon
  - [x] Bottom pill bar: `background pill`, `border border`, `borderRadius 28dp`; "Done" (accent) / grid glyph / "Private" (`textMid`)
  - [x] `HomeIndicator` at bottom
- [x] Swipe card left/right → close tab with fade-out (drag gesture + alpha/translation, note: no backdrop blur — see Phase 5 note)
- [x] Tab count badge in PillBar (grid icon) updates reactively from `TabsViewModel` — full tab persistence wired: `MainActivity` now owns a single `TabsViewModel`, `BrowserScreen` renders the active tab's real `BrowserWebView` (`existingWebView`), switching tabs preserves each WebView instance

---

## Phase 9 — Overflow Menu (Screen A5) ✅

> Design ref: `MenuScreen` in Orion Hi-Fi.

- [x] Build `BrowserMenuBottomSheet` composable (`ModalBottomSheet`):
  - [x] Sheet: `background surface`, `borderRadius 24dp 24dp 0 0` (note: no backdrop blur — see Phase 5 note; default `ModalBottomSheet` scrim used)
  - [x] Drag handle: 40×4dp, `borderRadius 2dp`, `background textDim`
  - [x] **URL row**: `background elevated`, `borderRadius 14dp`, `border border`; teal lock icon + URL text (14sp, 500w) + external-link glyph
  - [x] **Quick actions row**: 4 items (Reload / Save / Share / Install); 52×52dp tile (`background elevated`, `border border`, `borderRadius 16dp`) + 11sp 500w `textMid` label below
  - [x] **Menu list**: label (16sp `text`) + chevron right (`textDim`)
    - Items: New Tab / New Private Tab / History / Downloads / Zoom: 100% / Find on Page / Settings
- [x] Wire each item to the appropriate navigation action or callback (Reload, Share via `Intent.ACTION_SEND`, New Tab, New Private Tab, History, Downloads, Settings all wired in `MainActivity`; Save/Install/Zoom are stubs pending later phases)

---

## Phase 10 — History Screen ✅

> Design ref: `HistoryScreen` in Orion Hi-Fi.

- [x] Build `HistoryScreen` composable:
  - [x] `StatusBar` at top
  - [x] Header row: "History" title (26sp, 700w) + "Clear All" button (14sp, 600w, accent)
  - [x] Search bar: `background surface`, `border border`, `borderRadius 14dp`; search glyph + placeholder
  - [x] History list (`LazyColumn`):
    - Date group label: 11sp, 700w, `textDim`, all-caps
    - Group card: `background surface`, `border border`, `borderRadius 16dp`
    - History row: 36×36dp favicon tile (color dot) + title (14sp 500w, ellipsis) + URL (12sp `textDim`, ellipsis) + time string (11sp `textDim`)
  - [x] Swipe-to-dismiss (right, `SwipeToDismissBox`) → delete + `Snackbar` Undo
  - [x] Long-press → multi-select mode + delete-selected bar
  - [x] `HomeIndicator` at bottom
- [x] `ClearBrowsingDataDialog`: checkboxes (History / Cookies / Cache) + time-range selector (Last hour / 24h / 7 days / All time); cookies/cache cleared via `CookieManager`/`WebStorage`
- [x] `HistoryViewModel` with grouped (by date) history `StateFlow` and `filter(query)` function
- [x] Bonus: real history writes wired — `TabsViewModel.updateTabInfo` now inserts a `HistoryEntry` on title change for non-incognito tabs, so this screen has live data

---

## Phase 11 — Bookmarks Screen ✅

> Bookmarks screen is not in the Orion Hi-Fi (not yet designed). Use Orion design tokens and card/list patterns consistent with History screen.

- [x] Build `BookmarksScreen` composable:
  - [x] `StatusBar` + header row ("Bookmarks" 26sp 700w)
  - [x] Search bar (same style as History search bar)
  - [x] `LazyColumn`: folder rows (`surface` card, `border`, `borderRadius 16dp`) with folder icon + title; bookmark rows same but with favicon tile + URL secondary text (12sp `textDim`)
  - [x] Folders expand inline with 16dp indentation per level
  - [x] Long-press row → `DropdownMenu` (Open / Open in New Tab / Edit / Delete / Move — Move opens a folder-picker dialog)
  - [x] Drag-to-reorder: simplified handle-based gesture (drag "⠿" handle up/down swaps `sortOrder` with adjacent sibling) rather than full continuous DnD — documented tradeoff, avoids pulling in a reorder library
  - [x] FAB: accent background, `borderRadius 16dp`, "+ Bookmark" label — bottom-right, opens add dialog
  - [x] `HomeIndicator` at bottom
- [x] `BookmarksViewModel` exposing tree-structured bookmark list as `StateFlow` (`displayList: StateFlow<List<BookmarkNode>>`, flattened with depth for indentation)

---

## Phase 12 — Downloads Screen ✅

> Design ref: `DownloadsScreen` in Orion Hi-Fi.

- [x] Build `DownloadsScreen` composable:
  - [x] `StatusBar` at top
  - [x] Header row: "Downloads" title (26sp, 700w) + "Manage" button (14sp, 600w, accent — toggles multi-select)
  - [x] **Storage bar card**: "Storage Used" label + "X / 2 GB" value; progress bar 6dp `borderRadius 3dp`, fill `linear-gradient(accent → teal)`
  - [x] **File list**: file card (`background surface`, `border border`, `borderRadius 14dp`):
    - 44×44dp ext badge tile (`color×18`, `border color×30`, `borderRadius 12dp`): ext label (color)
    - File name (14sp, 500w, ellipsis) + size / progress secondary text
    - In-progress: gradient progress bar + "X% · size" label
    - Done: 28dp teal checkmark circle; In-progress: 28dp accent square icon circle
  - [x] Long-press → multi-select + delete selected (`DownloadManager.remove`)
  - [x] `HomeIndicator` at bottom
- [x] Poll `DownloadManager` every 1 second in `DownloadsViewModel` (plain `viewModelScope` loop — `repeatOnLifecycle` needs a `LifecycleOwner`, not applicable inside a bare `AndroidViewModel`; the loop is cancelled automatically when the ViewModel clears, equivalent effect)
- [x] Wire `WebView.setDownloadListener` in `BrowserWebViewComposable` — block `video/*` MIME downloads with Toast; route others to `DownloadManager` (public Downloads dir, notification on completion)

---

## Phase 13 — Settings Screen ✅

> Design ref: `SettingsScreen` in Orion Hi-Fi.

- [x] Build `SettingsScreen` composable:
  - [x] `StatusBar` at top, "Settings" title (26sp, 700w)
  - [x] **Profile/sync card**: gradient background (accent/teal), avatar circle with letter initial, name + sync status + chevron (static — no real account/sync system in this app)
  - [x] **Settings sections**: section label (11sp, 700w, `textDim`, all-caps); section card (`background surface`, `border border`, `borderRadius 16dp`); row with emoji icon + label + value/chevron OR Material3 `Switch` (colored to match accent/border tokens — close to the 44×26dp spec, not hand-rolled pixel-for-pixel)
  - [x] Sections: General (Search Engine, Homepage) / Privacy & Security (Ad Blocker, DNT, Block 3rd-party Cookies toggles + links to sub-screens + Clear Browsing Data) / Appearance (Theme, Font Size, Page Zoom) / About (version)
- [x] Implement preference items — all backed by `SettingsViewModel` + `DataStore`:
  - [x] Search engine selector (Google / Bing / DuckDuckGo) dialog
  - [x] Homepage text entry dialog
  - [x] JavaScript enabled toggle → propagated to all active `BrowserTab.webView` instances via a `LaunchedEffect(settings, tabs)` in `MainActivity`
  - [x] Text zoom `Slider` (50–200%) → `webView.settings.textZoom`, propagated the same way
  - [x] Desktop site toggle → swaps `userAgentString` between `BrowserWebView.CHROME_UA` / `DESKTOP_UA` (propagated per-tab)
  - [x] Dark mode selector (System / Light / Dark) → drives `BrowserTheme(darkTheme=...)` in `MainActivity`
  - [x] Clear browsing data entry → reuses Phase 10's `ClearBrowsingDataDialog`
  - [x] Do Not Track toggle — persisted; **not wired to actual request headers** (would need per-request header injection in `BrowserWebViewClient.shouldInterceptRequest`, out of scope here)
  - [x] Safe Browsing toggle → `WebSettingsCompat.setSafeBrowsingEnabled` propagated per-tab
  - [x] Block 3rd-party cookies toggle → `CookieManager.setAcceptThirdPartyCookies` propagated per-tab (genuinely wired, not just persisted)
- [x] **Privacy & Security sub-screen**: Cookies (always-on, informational), third-party cookie block, DNT, Safe Browsing, search suggestions (search suggestions flag persisted but not yet consumed by Omnibox — future wiring)
- [x] **Site Settings sub-screen**: Location / Mic / Camera / Notifications / Pop-ups / JavaScript toggles (persisted; real permission-prompt enforcement lands in Phase 15's `onPermissionRequest` wiring) / Media (Video) — greyed, always blocked, matches the 3-layer video block
- [x] Persist all settings via `DataStore<Preferences>` (`SettingsRepository`)
- [x] **Passwords section** — no in-app password vault; integrates with Android's system Autofill Framework instead: "Autofill Passwords" toggle (`autofillEnabled`, default on) drives `webView.importantForAutofill` (`IMPORTANT_FOR_AUTOFILL_YES`/`NO`) per tab in `MainActivity`; "Password Manager" row shows the active system autofill service (`AutofillManager`, resolved to its app label) and launches `Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE` so the user can pick/switch provider (Bitwarden, Google Password Manager, etc.) — label refreshes on `ON_RESUME` via a `LifecycleEventObserver`

---

## Phase 14 — Incognito Mode (Screen A10) ✅

- [x] `IncognitoNewTabPage` composable: dark toolbar (via `IncognitoTheme`'s `OrionTokens.Incognito`), 🥸 icon, "You've gone incognito" copy, what-it-does/doesn't-do bullet list
- [x] `applyIncognitoTheme()`: already existed as `IncognitoTheme` composable (Phase 1) — status bar/nav bar → `#1A1A1A`, `isAppearanceLightStatusBars = false`; now actually applied by wrapping the active screen content in `MainActivity` whenever `tab.isIncognito`
- [x] Incognito tab: `cacheMode = LOAD_NO_CACHE` set per-webview in `TabManager.newTab`; `CookieManager.setAcceptCookie(false)` (global — Android's `CookieManager` is a process-wide singleton with no true per-`WebView` cookie-accept override, so this disables cookies for **all** tabs while any incognito tab is open, matching the spec's literal global-API instruction; documented limitation) — no history writes already wired in Phase 10 (`TabsViewModel.updateTabInfo` skips `insertHistory` when `tab.isIncognito`)
- [x] On last incognito tab closed (`TabManager.closeTab`): `removeSessionCookies` + `flush` + `setAcceptCookie(true)`, `WebStorage.deleteAllData()`; theme restoration is automatic since `IncognitoTheme` wrapping is conditional on `tab.isIncognito` per active tab
- [x] Tab switcher shows incognito section: regular tabs grid, then a "PRIVATE" full-span label + incognito `TabCard`s forced to `OrionTokens.Incognito` dark colors regardless of app theme; bottom pill's "Private" label now creates+switches to a new incognito tab

---

## Phase 15 — Security & Lifecycle Polish ✅

- [x] `onBackPressed` / `BackHandler`: find-in-page → close it; webView can go back → go back; no more back history → `onExhausted` (closes current tab if others remain, else `Activity.finish()`) — wired in `BrowserScreen`/`NewTabPage`/`IncognitoNewTabPage` via `androidx.activity.compose.BackHandler`
- [x] Lifecycle: Activity-level `DisposableEffect` in `BrowserNavHost` — `webView.onPause()` for **all** tabs on `ON_PAUSE`; `webView.onResume()` for the active tab only on `ON_RESUME`; `webView.destroy()` for all tabs on `ON_DESTROY` (complements the existing per-composable resume/pause handling in `BrowserWebViewComposable`, which only touched the currently-composed WebView)
- [x] Add-to-Home-Screen: `ShortcutManager.requestPinShortcut` wired to the overflow menu's "Install" quick action
- [x] Print: `PrintManager.print()` + `WebView.createPrintDocumentAdapter()` wired to the overflow menu's "Print" quick action (was "Save")
- [x] Share URL: `Intent.ACTION_SEND` with `text/plain` (done in Phase 9)
- [x] Handle `WebView` file chooser (`onShowFileChooser`) for `<input type="file">` — `MainActivity` registers an `ActivityResultLauncher`, threaded down through `BrowserScreen` → `BrowserWebViewComposable` → `BrowserWebChromeClient`
- [x] Request runtime permissions for Camera, Mic, Location when sites request them via `WebChromeClient.onPermissionRequest` / `onGeolocationPermissionsShowPrompt` — gated first by the Site Settings toggles (Phase 13), then by Android's `RequestMultiplePermissions` runtime prompt

---

## Phase 16 — Full QA

- [ ] Video blocking: YouTube, Twitter/X, Vimeo, direct `.mp4` URL, video download attempt
- [ ] Navigation: back/forward state, progress bar, toolbar hide/show on scroll
- [ ] Omnibox: HTTPS lock, HTTP warning, suggestions on keystroke, search engine routing
- [ ] Tabs: switcher grid, swipe-close animation, badge count, tab persistence across rotation
- [ ] Incognito: dark theme, no history written, cookie + cache cleared on last tab close
- [ ] Bookmarks: add/edit/delete/move, folder navigation, drag reorder, real-time search
- [ ] History: date grouping, swipe-delete + undo, multi-select, clear data dialog
- [ ] Downloads: progress polling, pause/cancel, video chip disabled, open/share/delete
- [ ] Settings: search engine change immediate, JS toggle propagates, text zoom live preview
- [ ] Security: SSL error dialog, SafeBrowsing flag, incognito disables cookies + cache

---

## Brave Shields Parity — Feasibility Notes

Brave Shields is implemented at the Chromium-engine level (C++ hooks into the network stack, storage
partitioning, process model). This app runs on **Android system WebView**, a black-box engine with a
narrower API surface (`shouldInterceptRequest`, JS injection, `WebSettings`, cookie/storage managers).
Everything below is scoped to what's actually reachable through that surface — phases are tagged:

- **DOABLE** — implementable with existing WebView APIs, comparable in effect to Brave's version
- **PARTIAL** — implementable but weaker than Brave's engine-level version (JS-shim based, or narrow pattern-list based instead of general heuristic)
- **SKIP** — requires engine/OS-level hooks system WebView does not expose; not implementable without shipping a custom browser engine

---

## Phase 17 — Content Blocking Engine (Ads, Trackers, Social Embeds) ✅

> Extends the existing Layer 1 (`shouldInterceptRequest`)/Layer 2 (JS injection) pattern from Phase 2's video blocking to general ad/tracker blocking.

- [x] **DOABLE** — Static blocklist bundled as a raw asset (`assets/blocklists/ad_tracker_hosts.txt`, ~150 curated ad/tracker/social-SDK domains), loaded into an in-memory `HashSet` by `shields/AdTrackerBlocklist.kt` at startup (domain + all subdomains match)
- [x] **DOABLE** — Network-level ad/tracker blocking: `BrowserWebViewClient.shouldInterceptRequest` checks non-main-frame request hosts against the blocklist (+ imported custom domain rules) via an `isHostBlocked` lambda, returns an empty `WebResourceResponse` on match, gated on `ShieldsResolver`'s effective ad/tracker-block flags for the current page's host
- [x] **DOABLE** — Cosmetic filtering: `shields/CosmeticSelectors.kt` holds a curated `display:none` selector list (generic ad-slot conventions + social embed placeholders) injected in `onPageFinished` with a `MutationObserver` reattachment, mirroring the video-element removal pattern
- [x] **DOABLE** — Social media embed blocking: FB/Twitter/LinkedIn/Pinterest/TikTok/Instagram SDK + widget hosts added to the blocklist; their embed placeholders covered by the cosmetic selector list
- [x] **PARTIAL** — Custom filter list import (`shields/CustomFilterListParser.kt`): parses only plain domain-blocking lines (`||domain.com^`) and simple cosmetic rules (`domain.com##.selector`) from pasted text (Settings → Privacy & Security → Custom Filter List); `$third-party`/`$domain=`/regex options, scriptlets, `@@` exceptions are explicitly unsupported and silently ignored rather than mis-applied
- [x] **DOABLE** — Element blocker / tap-to-block picker: overflow menu → "Block Element" enters picker mode (`BrowserWebView.startElementPicker`, JS click-capture + selector computation via a `NoBufferElementPicker` JS interface); the selector is stored per-host in the new `site_cosmetic_rules` Room table and hidden immediately + on future loads of that site
- [x] Block counter: `BrowserTab.blockedCount` (reset on URL change, incremented via `TabsViewModel.incrementBlockedCount`), surfaced as a badge on the new shield icon in `PillBar`

---

## Phase 18 — Anti-Fingerprinting & Request Hardening ✅

> All items here are **JS-injection or header-level shims** — system WebView has no engine hook equivalent to Brave's per-session farbling, so these raise the bar but are more defeatable than Brave's implementation. This limitation is disclosed in the Settings toggle copy.

- [x] **PARTIAL** ("farbling-lite") — `shields/FingerprintProtectionJs.kt`, injected via `WebViewCompat.addDocumentStartJavaScript` (falls back to no-op when `DOCUMENT_START_SCRIPT` isn't supported) with a per-WebView-instance random seed; overrides `HTMLCanvasElement.toDataURL`/`CanvasRenderingContext2D.getImageData`, `AudioBuffer.getChannelData`, and `WebGLRenderingContext.getParameter` — JS-level, not hardened against `Function.prototype.toString` detection
- [x] **DOABLE** — `navigator.language`/`navigator.languages` overridden to `en-US` by the same document-start script; `Accept-Language: en-US` attached via `WebView.loadUrl(url, extraHeaders)` at the two navigation choke points (initial `BrowserWebViewComposable` load + `shouldOverrideUrlLoading`) rather than a global `WebSettings` API (no such per-app API exists on Android WebView — that's the real, working mechanism for this)
- [x] **SKIP** (documented) — Client hints reduction: confirmed no `WebSettingsCompat` user-agent-metadata API exists on webkit 1.13.0 to strip `Sec-CH-UA*`; those headers are attached by the engine before `shouldInterceptRequest`/`loadUrl(headers)` can touch them
- [x] **DOABLE** — Referrer policy hardening: `<meta name="referrer" content="strict-origin-when-cross-origin">` inserted by the document-start script (with a `MutationObserver` fallback if `<head>` isn't attached yet)
- [x] **DOABLE** — GPC: `navigator.globalPrivacyControl` defined via the document-start script; `Sec-GPC: 1` attached via the same `loadUrl(url, extraHeaders)` mechanism as Accept-Language (main-frame only — subresources can't carry it, a WebView limitation)
- [x] Settings UI: single "Anti-Fingerprinting" toggle (default on) in both Settings → Privacy & Security and the Privacy sub-screen; per-site override available from the `PillBar` shield icon (Phase 20)

---

## Phase 19 — Navigation & URL Hardening ✅

- [x] **DOABLE** — HTTPS upgrading (main-frame only): `BrowserWebViewClient.resolveNavigationUrl` rewrites `http://` → `https://` at both navigation choke points; on failure (`onReceivedError` for that main-frame request) falls back to the original `http://` URL, marks the host as abandoned for the rest of the session, and shows a "loaded over HTTP" Toast instead of a modal interstitial; subresource upgrading remains **SKIP** per the original reasoning
- [x] **DOABLE** — Query-parameter stripping (`shields/UrlSanitizer.stripTrackingParams`): strips `utm_*`, `fbclid`, `gclid`, `msclkid`, `mc_eid`, `igshid`, and a few more, applied at both navigation choke points; no separate "copy link" action exists in this app yet, so that half of the original bullet doesn't apply
- [x] **PARTIAL** — Tracking-redirect debouncing (`UrlSanitizer.unwrapRedirector`): list-based unwrap of Google `/url?q=` and Facebook `l.php?u=` wrappers only; `t.co` and other opaque shorteners need a real network hop to resolve and are out of scope, as documented originally
- [x] **DOABLE** — De-AMP: `UrlSanitizer.isAmpUrl` matches `cdn.ampproject.org`/`/amp/` path segments; on match, `onPageFinished` reads the loaded page's `<link rel="canonical">` via JS and redirects to it if present and non-AMP

---

## Phase 20 — Shields UI (Per-Site Controls + Global Defaults) ✅

- [x] New Room table `site_shield_overrides` (`SiteShieldOverride(host, adBlock, trackerBlock, scriptsEnabled, fingerprintProtection)` — nullable fields inherit the global default)
- [x] Global defaults: `ShieldsMode` enum (Standard / Aggressive / Disabled) in `SettingsRepository`/DataStore, selectable from Settings → Privacy & Security → Shields
- [x] Per-site override UI: shield icon added to `PillBar` next to the tabs button (badge = this page's blocked-request count), opening `ShieldsBottomSheet` with the site's effective ad-block/tracker-block/scripts/fingerprint-protection switches + a "Reset" action clearing the override
- [x] Site Settings' JavaScript toggle stays global-only (per-site JS control now lives in the Shields sheet instead, to avoid two competing per-site JS controls); the Shields sheet is the actual per-site entry point
- [x] `shields/ShieldsResolver.kt` is the single source of truth — layers a per-site override on top of the global `ShieldsMode`/settings snapshot, kept in-memory and updated via background Flow collectors so `BrowserWebViewClient` callbacks (called off the UI thread) can read it synchronously without hitting Room

---

## Phase 21 — Time-Limited Permission Grants ✅

> Extends Phase 15's per-request Camera/Mic/Location permission prompting.

- [x] New Room table `site_permission_grants` (`SitePermissionGrant(host, permissionType, grantedAt, expiresAt)`, composite PK on host+type)
- [x] `MainActivity.handlePermissionRequest`/`handleGeolocationPermission`: checks for a non-expired grant first (only among resource types whose Site Settings master toggle is still on, so a stored grant can never bypass a toggle the user has since turned off) before falling back to the Android runtime prompt; writes a new grant with the configured TTL on approval instead of only an in-memory decision
- [x] Background cleanup: `BrowserRepository.purgeExpiredPermissionGrants()` called from `BrowserApplication.onCreate`
- [x] Settings UI: grant TTL selector (1 hour / 24 hours / 7 days / Ask every time — the last skips persisting a grant entirely) under Site Settings

---

## Not Feasible on Android System WebView

These Brave Shields features require Chromium-engine-level or OS-network-level hooks that system
WebView does not expose to host apps. Listed here so they aren't silently dropped from the plan —
they're an explicit **won't-do** given this app's architecture, not an oversight:

- **DOM/network state partitioning** — storage isolation keyed by top-level site requires engine-level partitioning of the cookie jar/localStorage/cache; Android's `CookieManager`/`WebStorage` are process-wide singletons with no per-top-level-site partition API
- **Bounce-tracking protection (general/heuristic)** — Brave's version uses interaction + storage-access timing heuristics deep in the engine; only the narrow list-based redirector unwrapping in Phase 19 is achievable here
- **CNAME cloaking protection** — requires intercepting DNS resolution (including CNAME chain inspection) before the engine connects; WebView performs its own DNS resolution with no hook to inspect or veto it pre-connect
- **"Pool-party" side-channel mitigation** — a process-scheduler-level mitigation specific to Brave/Chromium's process model; not something an app hosting WebView can influence
- **Limited first-run/background telemetry calls** — N/A rather than skip: this app already makes no background telemetry calls of its own, so there's nothing to reduce

---

## Recommended Build Order

Follow this sequence to keep each phase runnable end-to-end:

1. ✅ Phase 1 — Project Foundation
2. ✅ Phase 2 — WebView Engine + Video Blocking
3. ✅ Phase 3 — Data Layer (Room)
4. ✅ Phase 4 — Tab Management
5. ✅ Phase 5 — Main Browser Screen
6. ✅ Phase 6 — Omnibox Search Overlay
7. ✅ Phase 7 — New Tab Page
8. ✅ Phase 8 — Tab Switcher
9. ✅ Phase 9 — Overflow Menu
10. ✅ Phase 10 — History Screen
11. ✅ Phase 11 — Bookmarks Screen
12. ✅ Phase 12 — Downloads Screen
13. ✅ Phase 13 — Settings Screen
14. ✅ Phase 14 — Incognito Mode
15. ✅ Phase 15 — Security & Lifecycle Polish
16. Phase 16 — Full QA (manual/device testing — not code, out of scope for automated build)
17. ✅ Phase 17 — Content Blocking Engine (Ads, Trackers, Social Embeds)
18. ✅ Phase 18 — Anti-Fingerprinting & Request Hardening
19. ✅ Phase 19 — Navigation & URL Hardening
20. ✅ Phase 20 — Shields UI (Per-Site Controls + Global Defaults)
21. ✅ Phase 21 — Time-Limited Permission Grants
