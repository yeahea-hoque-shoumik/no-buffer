# NoBuffer

A custom Android browser built with Kotlin and Jetpack Compose — a Chrome UI/UX clone with one deliberate difference: **it never plays video**.

## Why

NoBuffer strips video out of every page it loads, at three independent layers, so no single bypass (blocked domain change, DOM re-injection, fullscreen API) can slip video through:

1. **Network layer** — `WebViewClient.shouldInterceptRequest` blocks requests by file extension, MIME type, and known video CDN patterns (YouTube, Twitter/X, Vimeo, JW Player, HLS/DASH manifests, etc.).
2. **DOM layer** — JS injected on `onPageFinished` removes any `<video>`/`<source>` elements and watches for new ones via `MutationObserver`.
3. **Fullscreen layer** — `WebChromeClient.onShowCustomView` (the hook sites use for fullscreen video players) is suppressed immediately.

Everything else about browsing — tabs, history, bookmarks, downloads, settings, incognito mode — works like a normal Chrome-style mobile browser.

## Features

- **Tabs** — grid switcher, incognito tabs, tab count badge, and persistence: open tabs survive an app restart (Room-backed), and each restored tab's page only loads when you actually switch to it.
- **Omnibox** — combined search/address bar with recent, bookmark, and search suggestions.
- **History & Bookmarks** — searchable, date-grouped history with swipe-to-delete/undo; foldered bookmarks with drag-to-reorder.
- **Downloads** — backed by Android's `DownloadManager`, with live progress; video downloads are blocked the same as video playback.
- **Settings** — search engine, homepage, JavaScript/desktop-site/dark-mode toggles, per-site permissions (camera/mic/location/notifications), Do Not Track, ad blocker flag, Safe Browsing, text zoom, and third-party cookie blocking.
- **Passwords** — no in-app password vault. Instead, NoBuffer integrates with Android's system Autofill Framework, so whatever you already use (Bitwarden, Google Password Manager, 1Password, etc.) autofills and saves credentials in the browser like it does everywhere else on your device.
- **Incognito mode** — no history writes, no-cache WebView, cookies cleared when the last private tab closes.
- **Security** — Safe Browsing, SSL error interstitial (never silently proceeds), strict mixed-content blocking, per-permission gating before any Android runtime prompt fires.

## Design

UI follows a from-scratch design system called **Orion** — a dark "space void" theme by default (light "soft violet" alternative), frosted-glass pill navigation bar in place of a traditional toolbar, `DM Sans` typography. Full token values and per-screen specs live in [`PHASES.md`](PHASES.md).

## Tech stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (single-activity, `NavHost`-driven) |
| Persistence | Room (history, bookmarks, tabs) + DataStore (settings) |
| Web engine | Android `WebView` (Chromium) |
| Min SDK | 33 |
| Target SDK | 36 |

State is managed per-screen with `ViewModel` + `StateFlow` — there's no global shared mutable state.

## Building

```bash
./gradlew assembleDebug          # debug APK
./gradlew installDebug           # install to a connected device/emulator
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device required)
./gradlew :app:lint
```

## Project docs

- [`CLAUDE.md`](CLAUDE.md) — architecture, directory layout, key design constraints.
- [`PHASES.md`](PHASES.md) — phased implementation checklist and design-token reference.
- [`PLAN.md`](PLAN.md) — original full design spec (legacy Views/XML wording; the app itself is Compose-only).
