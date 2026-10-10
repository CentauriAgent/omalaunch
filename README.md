# Omalaunch

An [Omarchy](https://omarchy.org)-style home launcher for Android. Native Kotlin + Jetpack Compose.

P0 "Wofi": home canvas with pages, waybar-style top panel (page dots, clock,
battery), swipe-up wofi-style search drawer with fuzzy matching, and a dock.
Themes and Peridot sync land in P1/P2 — see PLAN.md.

## Build

    ./gradlew :app:assembleDebug

Requires JDK 17+ and an Android SDK with platform 35.
APK lands in `app/build/outputs/apk/debug/`.

## Install

Sideload the debug APK, then: Settings → Apps → Default apps → Home app → Omalaunch.

## Roadmap

- **P0 Wofi** (this): drawer, panel, dock, pages
- **P1 Waybar**: full panel modules + ThemePack engine with Omarchy themes
- **P2 Peridot**: encrypted settings sync via peridot-core (UniFFI), QR pairing
- **P3 Widgets/plugins**: AppWidgetHost, panel plugin TOML
- **P4 Ship**: F-Droid + Play

## Principles

No account. E2EE before anything leaves the device. Offered, not forced.
FOSS.
