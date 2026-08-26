# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Custom Android browser app, **NoBuffer** (`com.prime.nobuffer`) — Chrome UI/UX clone with 3-layer video blocking. Built in Kotlin + Jetpack Compose. See `PLAN.md` for the full design spec (colors, dimensions, interactions per screen) and `PHASES.md` for the phased implementation checklist.

> **Important:** `PLAN.md` was written for a Views/XML architecture. The project uses **Jetpack Compose** exclusively — `LazyColumn` not `RecyclerView`, `NavHost` not Activities per screen, `ModalBottomSheet` not `BottomSheetDialogFragment`, `AndroidView` wrapping `WebView`.

## Build & Run

```bash
./gradlew assembleDebug          # debug APK
./gradlew installDebug           # install to connected device/emulator
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device required)
./gradlew :app:lint
```

## Architecture

Single-activity Compose app. Entry point: `MainActivity` → `BrowserNavHost` (`NavHost`).

```
app/src/main/java/com/prime/nobuffer/
├── BrowserApplication.kt        # Application subclass; lazy database + repository singletons
├── MainActivity.kt              # NavHost host; BrowserNavHost composable
├── navigation/
│   └── Screen.kt                # sealed class with all route strings
├── browser/                     # Phase 2 — WebView engine + 3-layer video blocking
│   ├── BrowserWebView.kt        # WebView subclass: JS, dark mode, UA, safe browsing, security hardening
│   ├── BrowserWebViewClient.kt  # Layer 1 (shouldInterceptRequest) + Layer 2 (JS injection), SSL dialog
│   ├── BrowserWebChromeClient.kt# Layer 3 (onShowCustomView suppression), progress/title/icon callbacks
│   └── BrowserWebViewComposable.kt # AndroidView wrapper with lifecycle management
├── data/                        # Phase 3 — Room data layer
│   ├── entity/
│   │   ├── HistoryEntry.kt      # Room entity: url, title, faviconUrl, visitedAt
│   │   └── Bookmark.kt          # Room entity: url, title, faviconUrl, parentId, isFolder, sortOrder
│   ├── dao/
│   │   ├── HistoryDao.kt        # insert, delete, clearAll, search, findByUrl, observeAll (Flow)
│   │   └── BookmarkDao.kt       # insert, update, delete, search, isBookmarked, observeAll/ByParent, findByUrl
│   ├── BrowserDatabase.kt       # RoomDatabase with HistoryDao + BookmarkDao; version 1
│   └── BrowserRepository.kt     # Coroutine-friendly facade over both DAOs
└── ui/theme/
    ├── Color.kt                 # Chrome palette tokens (light / dark / incognito)
    ├── Theme.kt                 # BrowserTheme + IncognitoTheme
    └── Type.kt                  # Chrome typography scale
```

**Layers added by phase (see PHASES.md):**
- Phase 2 → `browser/` (WebView engine, 3-layer video blocking)
- Phase 3 ✅ → `data/` (Room entities, DAOs, Repository, BrowserDatabase); `BrowserApplication` singleton
- Phase 4 → `tabs/` (TabManager, TabsViewModel, TabCountBadge composable)
- Phase 5–9 → screen composables wired into NavHost
- Phase 13 → `settings/` (DataStore preferences)

**State management:** `ViewModel` + `StateFlow` per screen; no global shared mutable state.

## Key Design Constraints

- **3-layer video blocking** (preserve on every refactor):
  1. `WebViewClient.shouldInterceptRequest` — network-level URL blocking
  2. `onPageFinished` JS injection — DOM removal + `MutationObserver`
  3. `WebChromeClient.onShowCustomView` — fullscreen video suppression
- **Min SDK 33**, Target SDK 36
- **KSP only** — Room annotation processing via `ksp(libs.androidx.room.compiler)`, no KAPT; `android.disallowKotlinSourceSets=false` required in `gradle.properties` for AGP 9.x + KSP 2.x compatibility
- **No dynamic Material You color** — `BrowserTheme` always uses Chrome's fixed palette; `IncognitoTheme` uses `#1A1A1A` surface

## Dependency Versions

All declared in `gradle/libs.versions.toml`:

| Library | Version |
|---------|---------|
| AGP | 9.1.1 |
| Kotlin | 2.2.10 |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.9.0 |
| Room | 2.7.0 |
| Lifecycle / ViewModel | 2.10.0 |
| Coil | 2.7.0 |
| DataStore | 1.1.3 |
| WebKit | 1.13.0 |
| Lifecycle Compose | 2.10.0 |

When adding new libraries, declare versions in `libs.versions.toml` first, reference via `libs.*` aliases.

## Navigation Routes

All routes are in `navigation/Screen.kt`. Current screens:

| Screen | Route | Phase |
|--------|-------|-------|
| Browser (main) | `browser` | 5 |
| Tab Switcher | `tab_switcher` | 8 |
| Omnibox | `omnibox/{currentUrl}` | 6 |
| History | `history` | 10 |
| Bookmarks | `bookmarks` | 11 |
| Downloads | `downloads` | 12 |
| Settings | `settings` | 13 |
| Settings → Privacy | `settings/privacy` | 13 |
| Settings → Site | `settings/site` | 13 |

## Orion Design System (primary reference for Phases 5–13)

Design files live in `design/project/`. The primary reference is `design/project/Orion Browser Hi-Fi.html`. Chat context: `design/chats/chat1.md`.

**Visual identity:** App name is **Orion Browser**. Icon is a geometric orbit ring (SVG: outer circle 40% opacity + tilted ellipse rx=17 ry=6.5 rotate=-32° + filled core dot + white highlight). Font: `DM Sans`.

**Dark theme (space void — default):** bg `#08080F`, surface `#101018`, elevated `#17172280`, border `#252538`, accent `#7B6EF5` (purple), teal `#36C9B0` (security), text `#F0EFF8`, textMid `#9994B8`, textDim `#504E68`, pill `#14142080`.

**Light theme (soft violet):** bg `#F4F3F8`, surface `#FFFFFF`, elevated `#ECE9F8`, border `#E0DCF3`, accent `#6B5EE4`, teal `#1DB89D`, text `#14122A`, textMid `#6B6585`, textDim `#BAAFCC`, pill `#FFFFFFD0`.

**PillBar** replaces Chrome's TopAppBar — frosted glass pill at top of every screen (blur 24dp, `borderRadius 28dp`). Search mode shows accent search icon; URL mode shows teal lock icon + host.

All token values and per-screen pixel specs are in `PHASES.md` Design Reference section and in the Hi-Fi HTML source.

## Chrome Design Tokens (legacy — superseded by Orion tokens above for Phases 5+)

Light / dark colors, typography, and dimension specs from `PLAN.md` Part A are used only for Phase 1–3 infrastructure. Phases 5+ follow the Orion Hi-Fi design.
