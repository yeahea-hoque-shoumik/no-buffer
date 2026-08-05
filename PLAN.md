# Custom Android Browser — Full Build Plan (Chrome UI/UX, No Video)

# Overview

A step-by-step plan to build a custom Android browser in Kotlin + Android WebView. The UI/UX **exactly mirrors Google Chrome for Android** — every screen, layout, interaction, and animation follows Chrome's design language. All video playback is blocked at three independent layers while all other browser features are preserved.

---

# Part A — Chrome UI/UX Design Reference

This section is the ground truth for every screen. All implementation in later phases must match this spec.

## Design Tokens

**Color palette**

| Token | Light Mode | Dark Mode |
| --- | --- | --- |
| Toolbar / Surface | `#FFFFFF` | `#202124` |
| Omnibox background | `#F1F3F4` | `#35363A` |
| Omnibox focused | `#FFFFFF` | `#2D2E31` |
| Primary text | `#202124` | `#E8EAED` |
| Secondary / hint text | `#5F6368` | `#9AA0A6` |
| Accent / links | `#1A73E8` | `#8AB4F8` |
| Progress bar | `#1A73E8` | `#8AB4F8` |
| Incognito toolbar | `#1A1A1A` | `#1A1A1A` |
| Divider | `#E8EAED` | `#3C4043` |

**Typography**

| Element | Typeface | Size | Weight |
| --- | --- | --- | --- |
| Omnibox URL | Google Sans / Roboto | 14sp | Regular |
| Omnibox hint | Roboto | 14sp | Regular |
| Tab card title | Roboto | 12sp | Medium |
| Menu item | Roboto | 14sp | Regular |
| Section header | Roboto | 12sp | Medium, ALL CAPS |
| Page/screen titles | Google Sans | 18sp | Medium |
| History row title | Roboto | 14sp | Regular |
| History row URL | Roboto | 12sp | Regular, grey |

**Dimensions**

| Element | Size |
| --- | --- |
| Toolbar height | 56dp |
| Omnibox pill height | 40dp, radius 20dp |
| Progress bar height | 3dp |
| Tab strip (switcher) | 40dp |
| Bottom sheet drag handle | 4dp × 32dp, radius 2dp |
| Menu row height | 48dp |
| Quick-action icon | 24dp |
| Min touch target | 48dp × 48dp |
| Tab card corner radius | 8dp |
| Tab card thumbnail height | 128dp |
| Most Visited tile | 72dp × 72dp, 8dp radius |
| Tab count badge | 26dp × 26dp, 4dp radius |

---

## Screen A1 — Main Browser

```
┌──────────────────────────────────────────┐
│  STATUS BAR                              │
├──────────────────────────────────────────┤
│  TOOLBAR  56dp  [elevation 2dp]          │
│  [←] [→]  [🔒 google.com      ] [□3] [⋮]│
├──────────────────────────────────────────┤
│  PROGRESS BAR  3dp  blue  (gone on done) │
├──────────────────────────────────────────┤
│                                          │
│           WEBVIEW CONTENT                │
│   (toolbar hides on scroll-down,         │
│    reappears on scroll-up)               │
│                                          │
└──────────────────────────────────────────┘
```

**Omnibox states:**

- Default: `[🔒] google.com` — host bold, path greyed
- Loading: spinner replaces favicon
- Insecure HTTP: `[ⓘ]` warning icon
- Focused: full-screen search overlay opens (Screen A2)

**Toolbar scroll behavior:** `AppBarLayout` with `scroll|enterAlways|snap` flags. Back button greyed out when can't go back. Forward button hidden (not just greyed) when unavailable.

**Find in Page bar** — slides up from bottom when activated:

```
[ Search in page... ]   2 / 8   [↑] [↓] [✕]
```

Height 52dp, white bg, 8dp elevation.

---

## Screen A2 — Omnibox Search Overlay

Full-screen overlay, slides up from toolbar position:

```
┌──────────────────────────────────────────┐
│ [✕]  [ https://current-url.com     ] [→] │
├──────────────────────────────────────────┤
│  🔍  kotlin android webview              │  search
│  🕒  stackoverflow.com/questions/...     │  history
│  ★   My Saved Bookmark                   │  bookmark
│  🌐  github.com/google/...               │  url
│  🔍  kotlin coroutines tutorial          │  search
└──────────────────────────────────────────┘
```

- `DialogFragment` with `windowIsFloating = false`, windowSoftInputMode adjustPan
- Pre-fills current URL, selects all on open
- Suggestions update on each keystroke (debounced 150ms)
- `→` icon auto-completes without navigating
- Row height 56dp: icon (24dp) + primary text + secondary URL/query text

---

## Screen A3 — New Tab Page

```
┌──────────────────────────────────────────┐
│  TOOLBAR                                 │
├──────────────────────────────────────────┤
│                                          │
│    [Google logo  — centered, 92dp tall]  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  🔍  Search or type URL           │  │
│  └────────────────────────────────────┘  │
│                                          │
│   MOST VISITED  (2 rows × 4 tiles)       │
│   [YT]  [GH]  [SO]  [TW]                │
│   [RD]  [MD]  [ +]                       │
│                                          │
└──────────────────────────────────────────┘
```

- Tile: 72×72dp `MaterialCardView`, 8dp radius
- Letter avatar fallback if no favicon (colored bg + initial)
- Label below tile: 11sp, truncated to 1 line
- Long-press tile → popup: Open / Open in new tab / Remove / Edit
- `+ Add shortcut` tile at end of grid

---

## Screen A4 — Tab Switcher

Slide-up activity with `overridePendingTransition(slideUp, fadeOut)`:

```
┌──────────────────────────────────────────┐
│  [✕]       3 Tabs          [+ New tab]   │
├──────────────────────────────────────────┤
│  ┌──────────────┐   ┌──────────────┐    │
│  │ [thumbnail]  │   │ [thumbnail]  │    │
│  │──────────────│   │──────────────│    │
│  │ 🌐 Title 1 ✕│   │ 🌐 Title 2 ✕│    │
│  └──────────────┘   └──────────────┘    │
│  ┌──────────────┐                        │
│  │ [thumbnail]  │                        │
│  │──────────────│                        │
│  │ 🌐 Title 3 ✕│                        │
│  └──────────────┘                        │
├──────────────────────────────────────────┤
│   [+ New tab]        [🕵 Incognito]      │
└──────────────────────────────────────────┘
```

- `GridLayoutManager` span 2, `MaterialCardView` cards
- Active tab: 2dp blue border stroke
- Swipe card left/right → close with fade-out animation
- Long press → Close / Close others / Add to group
- Tab count badge: white number in rounded-square border (drawn custom via `onDraw`)

---

## Screen A5 — Overflow Menu

Chrome-style `BottomSheetDialogFragment`, not a dropdown:

```
┌──────────────────────────────────────────┐
│              ▬  drag handle              │
├──────────────────────────────────────────┤
│  [←]  [→]  [⟳]  [☆ / ★]  [⬆ Share]   │  quick-action row
├──────────────────────────────────────────┤
│  ➕  New tab                              │
│  🕵  New incognito tab                   │
│  ☆   Bookmarks                           │
│  🕒  History                             │
│  ⬇   Downloads                          │
│  🔍  Find in page                        │
│  🖥   Desktop site              [toggle] │
│  🖨   Print                              │
│  ➕  Add to Home Screen                  │
│  ℹ    Site settings                      │
│  ─────────────────────────────────────── │
│  ⚙    Settings                          │
│  ❓   Help & Feedback                   │
└──────────────────────────────────────────┘
```

- Quick-action row: 5 icons, 56dp row height, centered, each 48dp touch target
- Bookmark icon fills (★) when page is bookmarked
- Reload shows ✕ while page is loading
- `RecyclerView` for menu items, each 48dp row
- Peek height = full content; draggable

---

## Screen A6 — Bookmarks

```
┌──────────────────────────────────────────┐
│  [←]  Bookmarks          [🔍]  [⋮]      │
├──────────────────────────────────────────┤
│  📁  Mobile Bookmarks           >        │
│  ├── 🌐 Google                           │
│  ├── 🌐 Stack Overflow                   │
│  └── 📁 Work                   >         │
│          └── 🌐 Jira                     │
│  📁  Other Bookmarks            >        │
├──────────────────────────────────────────┤
│  [+ Add bookmark for current page]       │
└──────────────────────────────────────────┘
```

- `RecyclerView` flat list, folders expand inline (indentation 16dp per level)
- Row: folder icon OR favicon (24dp) + title + URL (secondary)
- Long-press: popup with Open / Open in new tab / Edit / Delete / Move
- Drag-to-reorder via `ItemTouchHelper`
- Search bar filters titles and URLs in real-time

---

## Screen A7 — History

```
┌──────────────────────────────────────────┐
│  [←]  History         [🔍]  [Clear all]  │
├──────────────────────────────────────────┤
│  TODAY                                   │  sticky header
│  ├── 🌐 Google.com              3:42 PM  │
│  ├── 🌐 Stack Overflow          2:11 PM  │
│  └── 🌐 Github.com              1:05 PM  │
│  YESTERDAY                               │  sticky header
│  ├── 🌐 Reddit.com              9:30 PM  │
│  └── 🌐 Medium.com              7:15 PM  │
├──────────────────────────────────────────┤
│  [🗑 Clear browsing data]                │  sticky footer
└──────────────────────────────────────────┘
```

- `ConcatAdapter`: `StickyHeaderAdapter` + `HistoryItemAdapter` per date group
- Row: favicon 24dp + title (14sp bold) + URL (12sp grey) + time (12sp grey)
- Row height: 64dp
- Swipe right → delete with Snackbar + Undo
- Long press → multi-select mode (checkbox appears, action bar shows "Delete X")
- Search: `SearchView` in toolbar, filters all entries, matches highlighted
- Clear browsing data dialog: checkboxes for History / Cookies / Cache + time range picker (Last hour / Last 24h / Last 7 days / All time)

---

## Screen A8 — Downloads

```
┌──────────────────────────────────────────┐
│  [←]  Downloads               [⋮]        │
├──────────────────────────────────────────┤
│  [All ✓]  [Images]  [Video 🚫]          │  filter chips
├──────────────────────────────────────────┤
│  IN PROGRESS                             │
│  ┌──────────────────────────────────┐   │
│  │ 📥 bigfile.zip  ████░░  67%      │   │
│  │ 4.2 MB / 6.3 MB  [Pause]  [✕]   │   │
│  └──────────────────────────────────┘   │
│  COMPLETED                               │
│  ┌──────────────────────────────────┐   │
│  │ 📄 report.pdf   2.3 MB · Today   │   │
│  │ [Open]          [Share]  [🗑]    │   │
│  └──────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

- Video filter chip present but disabled (tap shows toast: "Video downloads are blocked")
- Backed by `DownloadManager` queries, polled every 1 second via `lifecycleScope`
- In-progress: `LinearProgressIndicator` + Pause / Cancel
- Completed: file type icon + name + size + date + Open / Share / Delete
- Long press → multi-select + delete selected

---

## Screen A9 — Settings

```
┌──────────────────────────────────────────┐
│  [←]  Settings                           │
├──────────────────────────────────────────┤
│  BASICS                                  │
│  🔍 Search engine           Google  >    │
│  🏠 Homepage                Off     >    │
│  ADVANCED                                │
│  🔒 Privacy & Security              >    │
│  🛡 Safety Check                    >    │
│  🌐 Site Settings                   >    │
│  📥 Downloads                       >    │
│  🌙 Dark mode               System  >    │
│  🔠 Text size              100%     >    │
│  🖥 Desktop site              [off]      │
│  ABOUT                                   │
│  ℹ  About this browser              >    │
│  📜 Privacy policy                  >    │
└──────────────────────────────────────────┘
```

**Privacy & Security sub-screen:**

```
🍪 Cookies                    Allow all >
🚫 Block third-party cookies    [off]
🧹 Clear browsing data               >
🔏 Do Not Track                  [off]
🛡 Safe Browsing                 [on]
🔍 Search suggestions            [on]
```

**Site Settings sub-screen:**

```
📍 Location                    Block >
🎤 Microphone                  Block >
📷 Camera                      Block >
🔔 Notifications                 Ask >
📌 Pop-ups & redirects          Block
🎬 JavaScript                  Allow >
🎥 Media (Video)         Blocked 🔒 >   ← greyed, always off
```

---

## Screen A10 — Incognito NTP

When an incognito tab is active, the entire app shifts themes:

```
┌──────────────────────────────────────────┐
│  (dark toolbar #1A1A1A)                  │
├──────────────────────────────────────────┤
│                                          │
│    [🕵 hat-and-glasses icon — 72dp]      │
│                                          │
│  You've gone incognito                   │
│  Now you can browse privately...         │
│                                          │
│  What Incognito does / doesn't do:       │
│  ✓ Hides activity from this device       │
│  ✗ Doesn't hide from your employer       │
│                                          │
└──────────────────────────────────────────┘
```

- History not saved, cookies cleared on tab close
- Tab switcher shows incognito section with dark cards
- Toolbar, status bar, nav bar all `#1A1A1A`

---

# Part B — Implementation Phases

## Phase 1 — Project Setup

### Step 1: Create Project

- Android Studio → Empty Views Activity
- Kotlin, Min SDK API 26, `buildFeatures { viewBinding = true }`
- Package: `com.yourname.browser`

### Step 2: Gradle Dependencies

```kotlin
dependencies {
    // Material + Layout
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    // Preferences
    implementation 'androidx.preference:preference-ktx:1.2.1'
    // Glide (favicons)
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

### Step 3: Theme (res/values/themes.xml)

```xml
<style name="Theme.Browser" parent="Theme.Material3.Light.NoActionBar">
    <item name="colorPrimary">#1A73E8</item>
    <item name="colorSurface">#FFFFFF</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:windowLightStatusBar">true</item>
</style>

<style name="Theme.Browser.Incognito" parent="Theme.Material3.Dark.NoActionBar">
    <item name="colorSurface">#1A1A1A</item>
    <item name="android:statusBarColor">#1A1A1A</item>
</style>
```

---

## Phase 2 — Main Layout (Screen A1)

### Step 4: activity_main.xml

```xml
<CoordinatorLayout>
    <AppBarLayout android:elevation="2dp">

        <!-- Toolbar -->
        <LinearLayout height="56dp" app:layout_scrollFlags="scroll|enterAlways|snap">
            <ImageButton id="btnBack" src="ic_arrow_back_24"/>
            <ImageButton id="btnForward" src="ic_arrow_forward_24" visibility="gone"/>

            <!-- Omnibox pill -->
            <MaterialCardView id="omnibox" height="40dp" cornerRadius="20dp"
                cardBackgroundColor="#F1F3F4" layout_weight="1" marginHorizontal="8dp">
                <LinearLayout gravity="center_vertical" paddingHorizontal="12dp">
                    <ImageView id="omniboxSecurityIcon" size="16dp"/>
                    <TextView id="omniboxUrl" textSize="14sp" hint="Search or type URL"
                        paddingStart="6dp" singleLine="true" layout_weight="1"/>
                </LinearLayout>
            </MaterialCardView>

            <!-- Tab count badge -->
            <FrameLayout id="tabCountBtn" width="40dp" height="48dp">
                <TabCountDrawableView id="tabBadge" size="26dp" layout_gravity="center"/>
            </FrameLayout>

            <ImageButton id="btnMenu" src="ic_more_vert_24"/>
        </LinearLayout>

        <!-- 3dp progress bar -->
        <LinearProgressIndicator id="progressBar" height="3dp" visibility="gone"
            indicatorColor="#1A73E8"/>
    </AppBarLayout>

    <!-- WebView container -->
    <FrameLayout id="webViewContainer"
        app:layout_behavior="@string/appbar_scrolling_view_behavior"/>

    <!-- Find in Page bar (bottom overlay) -->
    <LinearLayout id="findInPageBar" height="52dp" layout_gravity="bottom"
        elevation="8dp" visibility="gone">
        <EditText id="findQuery" hint="Find in page" layout_weight="1"/>
        <TextView id="findCount" textSize="12sp"/>
        <ImageButton id="findPrev" src="ic_keyboard_arrow_up"/>
        <ImageButton id="findNext" src="ic_keyboard_arrow_down"/>
        <ImageButton id="findClose" src="ic_close_24"/>
    </LinearLayout>
</CoordinatorLayout>
```

---

## Phase 3 — WebView Engine + 3-Layer Video Blocking

### Step 5: BrowserWebView.kt

```kotlin
class BrowserWebView(context: Context) : WebView(context) {
    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = true  // layer 1: prevent autoplay
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
        isNestedScrollingEnabled = true  // for AppBarLayout coordination
    }
}
```

### Step 6: BrowserWebViewClient.kt — Layer 1 & 2 Video Blocking

```kotlin
class BrowserWebViewClient(...) : WebViewClient() {

    private val videoExtensions = listOf(
        ".mp4", ".webm", ".ogg", ".ogv", ".avi", ".mov",
        ".mkv", ".flv", ".m4v", ".m3u8", ".ts", ".3gp"
    )
    private val videoMimeKeywords = listOf(
        "video/", "application/x-mpegurl",
        "application/vnd.apple.mpegurl", "application/dash+xml"
    )

    // LAYER 1: Block video at the network request level
    override fun shouldInterceptRequest(view: WebView, req: WebResourceRequest): WebResourceResponse? {
        val url = req.url.toString().lowercase()
        val accept = req.requestHeaders["Accept"] ?: ""
        return if (
            videoExtensions.any { url.contains(it) } ||
            videoMimeKeywords.any { accept.contains(it) } ||
            url.contains("/videoplayback") || url.contains("googlevideo.com") ||
            url.contains(".mpd") || url.contains("manifest.m3u8")
        ) WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(),
            ByteArrayInputStream(ByteArray(0)))
        else super.shouldInterceptRequest(view, req)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        injectVideoBlock(view)  // LAYER 2: DOM injection
    }

    // LAYER 2: Remove video elements from DOM + watch for new ones
    private fun injectVideoBlock(view: WebView) {
        view.evaluateJavascript("""
            (function(){
                document.querySelectorAll('video,source,embed[type*="video"]').forEach(e=>e.remove());
                new MutationObserver(m=>m.forEach(r=>r.addedNodes.forEach(n=>{
                    if(['VIDEO','SOURCE'].includes(n.nodeName)) n.remove();
                    n.querySelectorAll?.('video,source').forEach(v=>v.remove());
                }))).observe(document.documentElement,{childList:true,subtree:true});
                Object.defineProperty(window,'HTMLVideoElement',{get:()=>null});
            })();
        """.trimIndent(), null)
    }

    // SSL error → always show dialog, never auto-proceed
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        MaterialAlertDialogBuilder(view.context)
            .setTitle("Connection not private")
            .setMessage("This site's certificate can't be verified.")
            .setPositiveButton("Back to safety") { _, _ -> handler.cancel() }
            .setNegativeButton("Proceed (unsafe)") { _, _ -> handler.proceed() }
            .show()
    }
}
```

### Step 7: BrowserWebChromeClient.kt — Layer 3 Video Blocking

```kotlin
class BrowserWebChromeClient(...) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, p: Int) = onProgress(p)
    override fun onReceivedTitle(view: WebView, t: String) = onTitle(t)
    override fun onReceivedIcon(view: WebView, icon: Bitmap) = onFavicon(icon)

    // LAYER 3: Block fullscreen video requests entirely
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        callback?.onCustomViewHidden()
    }
    override fun onShowCustomView(view: View?, orientation: Int, callback: CustomViewCallback?) {
        callback?.onCustomViewHidden()
    }
    override fun getVideoLoadingProgressView(): View? = null
}
```

---

## Phase 4 — Omnibox Search Overlay (Screen A2)

### Step 8: OmniboxOverlayFragment.kt

```kotlin
class OmniboxOverlayFragment : DialogFragment() {

    override fun onCreateView(...) = layoutInflater.inflate(R.layout.fragment_omnibox, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editText.setText(args.currentUrl)
        editText.selectAll()
        editText.addTextChangedListener { text -> loadSuggestions(text.toString()) }
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_ACTION_GO) { navigateTo(editText.text.toString()); true } else false
        }
    }

    private fun loadSuggestions(query: String) {
        lifecycleScope.launch {
            val history = historyDao.search("%$query%").take(3)
            val bookmarks = bookmarkDao.search("%$query%").take(2)
            val list = buildList {
                add(OmniboxSuggestion.Search(query))
                addAll(history.map { OmniboxSuggestion.History(it) })
                addAll(bookmarks.map { OmniboxSuggestion.Bookmark(it) })
            }
            adapter.submitList(list)
        }
    }
}

sealed class OmniboxSuggestion {
    data class Search(val query: String) : OmniboxSuggestion()
    data class History(val entry: HistoryEntry) : OmniboxSuggestion()
    data class Bookmark(val bm: Bookmark) : OmniboxSuggestion()
}
```

---

## Phase 5 — New Tab Page (Screen A3)

### Step 9: NewTabPageFragment.kt

Shown in the WebView container when URL is `about:blank`:

```kotlin
class NewTabPageFragment : Fragment(R.layout.fragment_new_tab) {
    override fun onViewCreated(view: View, ...) {
        searchBar.setOnClickListener { openOmnibox() }
        viewModel.topVisited.observe(viewLifecycleOwner) { sites ->
            mostVisitedAdapter.submitList(sites.take(8))
        }
    }
}
```

Layout: `ConstraintLayout` with Google logo centered ~40% from top, search pill below, `GridRecyclerView` span 4 below that.

---

## Phase 6 — Tab Switcher (Screen A4)

### Step 10: TabSwitcherActivity.kt

```kotlin
class TabSwitcherActivity : AppCompatActivity() {

    override fun onCreate(...) {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = TabGridAdapter(
            onTabClick = { tab -> setResultAndFinish(tab.id) },
            onTabClose = { tab ->
                tabManager.closeTab(tab.id)
                if (tabManager.tabs.isEmpty()) finish()
            }
        )
        ItemTouchHelper(SwipeToDismissCallback { tab ->
            tabManager.closeTab(tab.id)
        }).attachToRecyclerView(recyclerView)

        btnNewTab.setOnClickListener { tabManager.newTab(); finish() }
        btnIncognito.setOnClickListener { tabManager.newTab(incognito = true); finish() }
    }
}
```

### Step 11: TabCountDrawableView.kt

```kotlin
class TabCountDrawableView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : View(context, attrs) {

    var count = 1
        set(v) { field = v; invalidate() }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f.dp; color = Color.WHITE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 11f.sp
    }

    override fun onDraw(canvas: Canvas) {
        val r = 4f.dp
        canvas.drawRoundRect(2f.dp, 2f.dp, width - 2f.dp, height - 2f.dp, r, r, strokePaint)
        val cy = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(count.toString(), width / 2f, cy, textPaint)
    }
}
```

---

## Phase 7 — Overflow Menu (Screen A5)

### Step 12: BrowserMenuBottomSheet.kt

```kotlin
class BrowserMenuBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(...) =
        layoutInflater.inflate(R.layout.bottom_sheet_browser_menu, container, false)

    override fun onViewCreated(view: View, ...) {
        // Quick-action row
        quickBack.isEnabled = args.canGoBack
        quickForward.isEnabled = args.canGoForward
        quickBookmark.setImageResource(
            if (args.isBookmarked) R.drawable.ic_star_filled else R.drawable.ic_star_border
        )
        quickShare.setOnClickListener { shareUrl(args.url) }

        // Menu list
        menuList.adapter = MenuItemAdapter(getMenuItems()) { item ->
            handleMenuItem(item); dismiss()
        }
    }

    private fun getMenuItems() = listOf(
        MenuItem.NewTab, MenuItem.Incognito, MenuItem.Bookmarks,
        MenuItem.History, MenuItem.Downloads, MenuItem.FindInPage,
        MenuItem.DesktopSite(args.isDesktopMode), MenuItem.Print,
        MenuItem.AddToHomeScreen, MenuItem.SiteSettings,
        MenuItem.Divider,
        MenuItem.Settings, MenuItem.HelpFeedback
    )
}
```

---

## Phase 8 — History Screen (Screen A7)

### Step 13: HistoryActivity.kt

```kotlin
class HistoryActivity : AppCompatActivity() {

    override fun onCreate(...) {
        viewModel.groupedHistory.observe(this) { groups ->
            val adapters = groups.flatMap { (dateLabel, entries) ->
                listOf(StickyHeaderAdapter(dateLabel), HistoryRowAdapter(entries))
            }
            recyclerView.adapter = ConcatAdapter(adapters)
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, RIGHT) {
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val entry = (vh as HistoryRowAdapter.ViewHolder).entry
                viewModel.delete(entry)
                Snackbar.make(root, "Removed from history", LENGTH_SHORT)
                    .setAction("Undo") { viewModel.restore(entry) }.show()
            }
        }).attachToRecyclerView(recyclerView)

        clearAllBtn.setOnClickListener { showClearDataDialog() }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(q: String): Boolean {
                viewModel.filter(q); return true
            }
        })
    }

    private fun showClearDataDialog() {
        // Dialog with: time range spinner + checkboxes (History / Cookies / Cache)
    }
}
```

---

## Phase 9 — Downloads Screen (Screen A8)

### Step 14: DownloadsActivity.kt

```kotlin
class DownloadsActivity : AppCompatActivity() {

    override fun onCreate(...) {
        setupFilterChips()  // All, Images, Video (disabled)
        lifecycleScope.launch {
            while (true) {
                adapter.submitList(queryDownloads())
                delay(1000)
            }
        }
    }

    private fun queryDownloads(): List<DownloadItem> {
        val cursor = downloadManager.query(DownloadManager.Query())
        return buildList {
            while (cursor.moveToNext()) {
                add(DownloadItem.from(cursor))
            }
        }.sortedByDescending { it.lastModified }
    }

    private fun openFile(item: DownloadItem) {
        val uri = downloadManager.getUriForDownloadedFile(item.id) ?: return
        startActivity(Intent(ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType)
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        })
    }
}
```

**Block video downloads in DownloadListener:**

```kotlin
webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
    if (mimeType.startsWith("video/")) {
        Toast.makeText(ctx, "Video downloads are disabled", LENGTH_SHORT).show()
        return@setDownloadListener
    }
    val req = DownloadManager.Request(Uri.parse(url)).apply {
        setMimeType(mimeType)
        addRequestHeader("User-Agent", userAgent)
        setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
        setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS,
            URLUtil.guessFileName(url, contentDisposition, mimeType))
    }
    (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
}
```

---

## Phase 10 — Settings Screen (Screen A9)

### Step 15: SettingsActivity + Preferences XML

```kotlin
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(...) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("search_engine")?.setOnPreferenceChangeListener { _, v ->
            BrowserApp.searchEngine = v as String; true
        }
        findPreference<SwitchPreferenceCompat>("javascript_enabled")?.setOnPreferenceChangeListener { _, v ->
            // Apply to all active WebViews
            tabManager.tabs.forEach { it.webView.settings.javaScriptEnabled = v as Boolean }; true
        }
        findPreference<SeekBarPreference>("text_zoom")?.setOnPreferenceChangeListener { _, v ->
            tabManager.tabs.forEach { it.webView.settings.textZoom = v as Int }; true
        }
        findPreference<Preference>("clear_history")?.setOnPreferenceClickListener {
            showClearBrowsingDataDialog(); true
        }
    }
}
```

---

## Phase 11 — Bookmarks Screen (Screen A6)

### Step 16: BookmarksActivity.kt

```kotlin
class BookmarksActivity : AppCompatActivity() {

    private var currentFolder = BookmarkFolder.ROOT

    override fun onCreate(...) {
        recyclerView.adapter = BookmarkAdapter(
            onItemClick = { item ->
                when (item) {
                    is Bookmark -> { setResult(item.url); finish() }
                    is BookmarkFolder -> navigateInto(item)
                }
            },
            onLongClick = { item -> showContextMenu(item) }
        )
        ItemTouchHelper(DragReorderCallback()).attachToRecyclerView(recyclerView)
        fabAdd.setOnClickListener { showAddBookmarkDialog() }
    }

    private fun showContextMenu(item: BookmarkItem) {
        PopupMenu(this, anchorView).apply {
            menu.add("Open")
            menu.add("Open in new tab")
            menu.add("Edit")
            menu.add("Delete")
            menu.add("Move")
            show()
        }
    }
}
```

---

## Phase 12 — Data Layer (Room)

### Step 17: Entities & DAOs

```kotlin
@Entity("history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String, val title: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity("bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String, val title: String, val folderId: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun observeAll(): Flow<List<HistoryEntry>>
    @Query("SELECT * FROM history WHERE url LIKE :q OR title LIKE :q ORDER BY visitedAt DESC LIMIT 5")
    suspend fun search(q: String): List<HistoryEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: HistoryEntry)
    @Delete suspend fun delete(e: HistoryEntry)
    @Query("DELETE FROM history") suspend fun clearAll()
}

@Dao interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Bookmark>>
    @Query("SELECT * FROM bookmarks WHERE url LIKE :q OR title LIKE :q")
    suspend fun search(q: String): List<Bookmark>
    @Insert suspend fun insert(b: Bookmark)
    @Delete suspend fun delete(b: Bookmark)
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url=:url)")
    suspend fun isBookmarked(url: String): Boolean
}
```

---

## Phase 13 — Tab Management

### Step 18: TabManager.kt

```kotlin
class TabManager(private val context: Context) {
    private val _tabs = mutableListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() = _tabs
    var activeIndex = 0
    val activeTab get() = _tabs[activeIndex]

    fun newTab(url: String = "about:blank", incognito: Boolean = false): BrowserTab {
        val tab = BrowserTab(webView = BrowserWebView(context), url = url, incognito = incognito)
        _tabs.add(tab)
        activeIndex = _tabs.lastIndex
        if (url != "about:blank") tab.webView.loadUrl(url)
        return tab
    }

    fun closeTab(id: String) {
        val idx = _tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        _tabs[idx].webView.destroy()
        _tabs.removeAt(idx)
        if (_tabs.isEmpty()) newTab()
        else activeIndex = (idx - 1).coerceAtLeast(0)
    }

    fun captureSnapshot(tab: BrowserTab): Bitmap? {
        val bmp = Bitmap.createBitmap(tab.webView.width, 200.dp, Bitmap.Config.ARGB_8888)
        tab.webView.draw(Canvas(bmp))
        return bmp
    }

    fun destroyAll() = _tabs.forEach { it.webView.destroy() }
}
```

---

## Phase 14 — Incognito Mode (Screen A10)

### Step 19: Incognito Setup

```kotlin
fun openIncognitoTab() {
    val tab = tabManager.newTab(incognito = true)
    tab.webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
    CookieManager.getInstance().setAcceptCookie(false)
    applyIncognitoTheme()
}

fun applyIncognitoTheme() {
    val color = Color.parseColor("#1A1A1A")
    toolbar.setBackgroundColor(color)
    window.statusBarColor = color
    window.navigationBarColor = color
    WindowCompat.getInsetsController(window, window.decorView)
        .isAppearanceLightStatusBars = false
}

fun onIncognitoTabClosed() {
    // Clear all incognito data
    CookieManager.getInstance().removeSessionCookies(null)
    WebStorage.getInstance().deleteAllData()
    tab.webView.clearCache(true)
    tab.webView.clearHistory()
    if (!tabManager.tabs.any { it.incognito }) {
        CookieManager.getInstance().setAcceptCookie(true)
        applyNormalTheme()
    }
}
```

---

## Phase 15 — Security & Polish

### Step 20: Security Hardening

```kotlin
WebView.startSafeBrowsing(this) {}  // enable SafeBrowsing

webView.settings.apply {
    allowFileAccess = false
    allowContentAccess = false
}

// Force dark mode (follow system)
if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_AUTO)
}
```

### Step 21: Lifecycle

```kotlin
override fun onPause() { super.onPause(); tabManager.tabs.forEach { it.webView.onPause() } }
override fun onResume() { super.onResume(); tabManager.activeTab.webView.onResume() }
override fun onDestroy() { tabManager.destroyAll(); super.onDestroy() }

override fun onBackPressed() {
    when {
        findInPageBar.isVisible -> closeFindInPage()
        webView.canGoBack() -> webView.goBack()
        tabManager.tabs.size > 1 -> { tabManager.closeTab(tabManager.activeTab.id); renderActiveTab() }
        else -> super.onBackPressed()
    }
}
```

---

## Phase 16 — Full QA Checklist

### Step 22: Test Matrix

**Video blocking**

- [ ]  YouTube — video element removed, layout intact, no fullscreen attempted
- [ ]  Twitter/X — inline videos hidden, text/images load fine
- [ ]  Vimeo — player missing, page usable
- [ ]  Direct `.mp4` URL → 403 blocked silently
- [ ]  Video download attempt → toast, no file saved

**Navigation & Omnibox**

- [ ]  Back/forward buttons enable/disable correctly
- [ ]  Progress bar appears during load, hides on completion
- [ ]  Toolbar hides on scroll-down, reappears on scroll-up
- [ ]  Omnibox shows host only, 🔒 for HTTPS, ⚠ for HTTP
- [ ]  Typing in omnibox shows history + bookmark suggestions
- [ ]  Search query routes to configured search engine

**Tabs**

- [ ]  Tab switcher shows 2-column grid with thumbnails
- [ ]  Swipe to close tab with animation
- [ ]  Tab count badge updates in real time
- [ ]  Incognito tab: dark toolbar, no history written
- [ ]  Closing last incognito tab clears cookies/cache

**Bookmarks**

- [ ]  Add bookmark from menu → star fills immediately
- [ ]  Bookmarks page shows folder hierarchy
- [ ]  Long-press → edit / delete / move popup
- [ ]  Search filters in real time

**History**

- [ ]  Entries grouped by date with sticky headers
- [ ]  Swipe-right to delete + Undo snackbar
- [ ]  Multi-select mode on long press
- [ ]  Clear browsing data dialog with time range

**Downloads**

- [ ]  File downloads via DownloadManager with notification
- [ ]  In-progress shows progress bar + Pause/Cancel
- [ ]  Video filter chip shows disabled state
- [ ]  Open / Share / Delete actions work

**Settings**

- [ ]  Search engine change takes effect immediately
- [ ]  JavaScript toggle applies to active WebViews
- [ ]  Text zoom slider updates instantly
- [ ]  Clear browsing data clears history + cookies + cache

**Security**

- [ ]  SSL error shows dialog (not silently bypassed)
- [ ]  SafeBrowsing flags malicious URLs
- [ ]  Incognito disables cookies + caching

---

# Recommended Build Order

1. ✅ Phase 1 — Project setup, theme, Gradle
2. ✅ Phase 2 — Main layout (AppBarLayout + Omnibox + WebView)
3. ✅ Phase 3 — WebView engine + 3-layer video blocking
4. ✅ Phase 12 — Room DB (history + bookmarks DAOs)
5. ✅ Phase 13 — Tab management + TabCountBadge
6. ✅ Phase 4 — Omnibox overlay with suggestions
7. ✅ Phase 5 — New Tab Page (NTP)
8. ✅ Phase 6 — Tab Switcher screen
9. ✅ Phase 7 — Overflow bottom sheet menu
10. ✅ Phase 8 — History screen (sticky headers + swipe-delete)
11. ✅ Phase 9 — Downloads screen
12. ✅ Phase 10 — Settings screen
13. ✅ Phase 11 — Bookmarks screen
14. ✅ Phase 14 — Incognito mode + theme switching
15. ✅ Phase 15 — Security hardening + lifecycle polish
16. ✅ Phase 16 — Full QA

---

# Key Libraries

| Library | Purpose |
| --- | --- |
| `WebView`  • `WebViewClient` | Rendering, request interception, video URL blocking |
| `WebChromeClient` | Progress, favicons, fullscreen video blocking |
| `AppBarLayout`  • `CoordinatorLayout` | Toolbar hide-on-scroll behavior |
| `BottomSheetDialogFragment` | Chrome-style overflow menu |
| `Room` | History + bookmarks local storage |
| `DownloadManager` | System-level file downloads |
| `PreferenceFragmentCompat` | Settings UI |
| `ItemTouchHelper` | Swipe-to-delete (history), swipe-to-close (tabs) |
| `ConcatAdapter` | Sticky date headers in history |
| `SafeBrowsing API` | Malicious URL protection |
| `Glide` | Favicon loading in omnibox, history, tab cards |
| `MutationObserver` (JS) | DOM-level video element removal |

---

> **Note on rendering engine:** This plan uses Android WebView (Chromium-based, auto-updated via Play Store). For deeper media policy control, custom extension support, or protocol handling, consider **GeckoView** (Firefox engine) as a future upgrade path.
>