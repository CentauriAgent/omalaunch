# Jade (working name) — an Omarchy launcher for Android

Plan v0.1 — 2026-10-10. Status: PLANNING (nothing built yet).

## 1. Vision

An Android home-screen replacement that feels like your Omarchy machine
pocket-sized: same top panel, same searchable drawer, same themes, same
taste. Themes and preferences sync from your desktop via Peridot, end to
end encrypted, no account, offered-not-forced — exactly Peridot's model.

One-line pitch: "Your Omarchy desktop, in your pocket. Change your theme
at your desk, your phone offers to match."

## 2. Principles (inherited, not invented)

- No account, no server of ours. Transport rides Nostr via Opal/Peridot.
- E2EE before anything leaves the device. Phone is a first-class Peridot
  peer, not a cloud client.
- Offered, not forced: desktop changes arrive as suggestions with
  Apply / Ignore. Nothing mutates silently.
- FOSS, F-Droid friendly, Pixel-first (Derek's daily: Pixel 10 Pro XL).

## 3. Desktop to phone mapping

| Omarchy desktop | Jade on Android |
|---|---|
| Waybar top panel | Persistent panel strip at top of home: page dots (workspaces), current/last app title, clock, battery, net |
| Wofi app launcher | Swipe-up full-screen drawer, search-first, fuzzy match, usage-ranked |
| Hyprland workspaces | Horizontal home pages, dots in panel, long-press to reorder |
| Theme (wallpaper + GTK + accent + font) | ThemePack: wallpaper, panel colors/alpha, accent, JetBrainsMono Nerd Font, drawer style |
| Super+D / keybindings | Gestures: swipe up = drawer, pinch = page overview, double-tap = lock |
| Panel modules (waybar) | Built-in panel modules: clock, battery, CPU, RAM, net, date |
| Plugin marketplace (omacom) | Declarative plugin config (TOML) mirroring the omarchy-plugin-marketplace manifest; prefs sync via Peridot |

Note: the real Android status bar stays above our panel (system-owned).
We draw the Omarchy look directly under it. System gesture nav takes the
bottom edge; our gestures live inside the canvas above it.

## 4. Architecture

Native Android, Kotlin + Jetpack Compose. No Flutter/RN: a launcher
needs PackageManager, AppWidgetHost, wallpaper APIs, and always-on
performance.

Modules:
- `:app` — launcher activity (HOME intent), home canvas, pages, gestures
- `:drawer` — wofi-style drawer: instant search, fuzzy, usage ranking
- `:panel` — waybar-style Compose strip + module registry
- `:widgets` — AppWidgetHost (real Android widgets on canvas) + first-party modules
- `:theme` — ThemePack engine, applies wallpaper + Compose MaterialTheme
- `:sync` — UniFFI bindings over `peridot-core` (see §5)
- `:plugins` — module registry + TOML config surface

Min SDK 29 (Android 10), target latest. Server build note: USER.md lists
Android SDK v13.0 — needs a current cmdline-tools/SDK install under
`~/Android/Sdk` (user-space, no sudo needed) or GitHub Actions CI builds.

## 5. Peridot sync (the differentiator)

Findings from cloning derekross/peridot (2026-10-10):

- `crates/peridot-sync` = engine: crypto.rs (aes-gcm, hkdf, hmac),
  envelope.rs, identity.rs, pairing.rs, recovery.rs, rotation.rs,
  store.rs (rusqlite), sync.rs, gallery.rs — all portable logic.
- BUT scan.rs / apply.rs / manifest.rs touch std::fs, and Cargo pulls
  `notify` (inotify) + `rustix(fs)` — the desktop file-watching half.

Plan:
1. **Extract `peridot-core`** in the peridot repo: envelope, crypto,
   identity, pairing, recovery, rotation, store, sync protocol, gallery.
   `peridot-sync` keeps scan/apply/watch and depends on peridot-core.
   Clean workspace refactor, benefits every future client.
   Fallback if we want zero refactor first: bind peridot-sync as-is via
   UniFFI (Android is a Linux kernel; notify/rustix plausibly compile for
   the Android target) and simply never call the watch/scan paths.
2. **One UniFFI surface** `jade-sync`: expose join/pair, list offered
   settings, fetch/decrypt, publish own changes. Internally uses
   nostr-sdk (already a peridot dep) — Android never binds nostr directly.
3. **Pairing UX**: desktop Peridot shows a QR; Jade scans; phone joins
   the device group using the existing pairing/recovery machinery.
4. **What syncs**: ThemePack (colors, font id, wallpaper asset id or
   downscaled blob ≤2 MB), panel module list + order, drawer prefs,
   hidden/double-tap app prefs, plugin TOML. What never syncs: secrets,
   keys, anything executable (Peridot's rules already).
5. **v1 phone role: read-only mirror** (offers in, nothing published).
   Publishing phone-side changes is v2 — needs desktop-side apply UX.

## 6. Theme engine

- ThemePack JSON + assets; built-ins ported from the omarchy repo themes.
- "Synced" pseudo-theme tracks the desktop's current choice via Peridot.
- Applies: WallpaperManager (with our own crop/downscale), Compose color
  scheme, panel translucency, drawer corner radius/blur, font (bundle
  JetBrainsMono Nerd Font, OFL).
- Verify: wallpaper/font licensing in the omarchy repo before shipping
  built-ins; custom wallpapers sync as downscaled blobs.

## 7. Panel widgets and plugins

- Panel modules v1: clock, battery, date, CPU/RAM (proc), net state,
  page dots, active app. Adaptive polling (15 s idle, 1 s while charging
  and screen on) to protect battery.
- Canvas widgets: AppWidgetHost so standard Android widgets (clock,
  calendar) work on pages. First-party extras: terminal-look clock,
  system monitor card.
- Plugins: v1 is declarative — a TOML file (mirroring the
  omarchy-plugin-marketplace manifest format) selects modules and
  configures them; the TOML itself syncs via Peridot so desktop and
  phone stay in lockstep. Dynamic code loading is off the table
  (Android + Play policy); future: plugin "provider apps" exposing a
  bound-service module API.

## 8. Phases

- **P0 "Wofi" (~2 wks)**: HOME activity, canvas + pages, drawer with
  instant search, minimal panel (clock/battery/dots). Acceptance: Derek
  daily-drives it; cold start < 400 ms; zero jank paging.
- **P1 "Waybar" (~2 wks)**: full module set, gestures, ThemePack engine
  with 5+ built-in Omarchy themes. Acceptance: side-by-side screenshot
  with the desktop looks like family.
- **P2 "Peridot" (~3 wks)**: peridot-core extraction + UniFFI, QR
  pairing, theme/prefs sync with offer-apply UX. Acceptance: change
  theme on the desktop, phone offers the match in < 60 s. This is the
  demo GIF.
- **P3 "Widgets and plugins" (~2 wks)**: AppWidgetHost, first-party
  widgets, plugin TOML + sync.
- **P4 "Ship" (~1 wk)**: F-Droid metadata (reproducible Rust+Gradle
  build), Play listing, landing page, the GIF.

## 9. Risks

- peridot-core extraction drift — mitigate with a shared test-vectors
  crate so desktop and phone agree byte-for-byte.
- nostr-sdk on Android via our UniFFI wrapper: cross-compile toolchain
  (cargo-ndk) + build size (~5-8 MB native lib) — acceptable, verify
  early in P2 week 1.
- Battery/memory: launcher is always resident; budget strict (no bitmap
  churn, Compose-only rendering, release profiling on Pixel).
- OEM quirks (Samsung/Xiaomi widget hosts) — Pixel-first, community
  reports drive fixes.
- Name/brand: "Onyx" collides with Derek's own project; "Emerald
  Launcher" exists on F-Droid. Working name Jade — clearance + naming
  decision open (§10). Using "Omarchy" in the strapline needs the
  omacom/DHH nod.

## 10. Open questions for Derek

1. Name: Jade? Another gem? Or literal "Omarchy Launcher" if blessed?
2. Ask DHH/omacom about the Omarchy name for a third-party companion
   launcher (and possibly upstream peridot-core extraction)?
3. F-Droid first (community cred) vs Play first (reach)? Recommend both,
   F-Droid first.
4. v1 sync scope: themes only, or themes + panel/drawer prefs too?
   Recommend themes + prefs (it's the same machinery).
5. Read-only phone v1 (recommend) vs full peer immediately?

## 11. Verify-first checklist (P2 gate, do before writing sync code)

- [ ] Read peridot pairing.rs + dm.rs flow; document the exact join path
- [ ] Read envelope.rs + gallery.rs; document offer/apply semantics
- [ ] Check opal-core/opal-kit: what the transport expects (relays,
      event kinds) and whether a phone can speak it unmodified
- [ ] Confirm nostr-sdk cross-compiles via cargo-ndk in CI
- [ ] Inventory omarchy repo themes + licenses (wallpapers, fonts)
- [ ] Android SDK refresh on the server (current cmdline-tools) or move
      builds to GitHub Actions from day one
