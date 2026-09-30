# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Custom Android browser app, **NoBuffer** (`com.prime.nobuffer`) — Chrome UI/UX clone with 3-layer video blocking. Built in Kotlin + Jetpack Compose. See `PLAN.md` for the full design spec (colors, dimensions, interactions per screen) and `PHASES.md` for the phased implementation checklist.

> **Important:** `PLAN.md` was written for a Views/XML architecture. The project uses **Jetpack Compose** exclusively — `LazyColumn` not `RecyclerView`, `NavHost` not Activities per screen, `ModalBottomSheet` not `BottomSheetDialogFragment`, `AndroidView` wrapping `WebView`.

## Conventions

**State management:** `ViewModel` + `StateFlow` per screen; no global shared mutable state.

## Key Design Constraints

- **3-layer video blocking** (preserve on every refactor):
  1. `WebViewClient.shouldInterceptRequest` — network-level URL blocking
  2. `onPageFinished` JS injection — DOM removal + `MutationObserver`
  3. `WebChromeClient.onShowCustomView` — fullscreen video suppression
- **Min SDK 33**, Target SDK 36
- **KSP only** — Room annotation processing via `ksp(libs.androidx.room.compiler)`, no KAPT; `android.disallowKotlinSourceSets=false` required in `gradle.properties` for AGP 9.x + KSP 2.x compatibility
- **No dynamic Material You color** — `BrowserTheme` always uses Chrome's fixed palette; `IncognitoTheme` uses `#1A1A1A` surface
- **`allowFileAccess`/`allowContentAccess` are `true`** (`BrowserWebView.kt`) — local `file://`/`content://` loading enabled; `allowFileAccessFromFileURLs`/`allowUniversalAccessFromFileURLs` must stay `false` (default) — that pairing is the actual sandbox-escape risk, not the two flags above
- **No in-app password vault** — credential storage/autofill is delegated entirely to Android's system Autofill Framework (`AutofillManager`, `Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE`); the app only toggles `View.importantForAutofill` per tab, it never stores credentials itself

## Dependencies

When adding new libraries, declare versions in `libs.versions.toml` first, reference via `libs.*` aliases.

## Orion Design System (primary reference for Phases 5–13)

Design files live in `design/project/`. The primary reference is `design/project/Orion Browser Hi-Fi.html`. Chat context: `design/chats/chat1.md`.

**Visual identity:** App name is **Orion Browser**. Icon is a geometric orbit ring (SVG: outer circle 40% opacity + tilted ellipse rx=17 ry=6.5 rotate=-32° + filled core dot + white highlight). Font: `DM Sans`.

**Dark theme (space void — default):** bg `#08080F`, surface `#101018`, elevated `#17172280`, border `#252538`, accent `#7B6EF5` (purple), teal `#36C9B0` (security), text `#F0EFF8`, textMid `#9994B8`, textDim `#504E68`, pill `#14142080`.

**Light theme (soft violet):** bg `#F4F3F8`, surface `#FFFFFF`, elevated `#ECE9F8`, border `#E0DCF3`, accent `#6B5EE4`, teal `#1DB89D`, text `#14122A`, textMid `#6B6585`, textDim `#BAAFCC`, pill `#FFFFFFD0`.

**PillBar** replaces Chrome's TopAppBar — frosted glass pill at top of every screen (blur 24dp, `borderRadius 28dp`). Search mode shows accent search icon; URL mode shows teal lock icon + host.

All token values and per-screen pixel specs are in `PHASES.md` Design Reference section and in the Hi-Fi HTML source.
