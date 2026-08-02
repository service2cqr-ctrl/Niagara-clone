# Niagara Clone

A from-scratch Android launcher inspired by Niagara Launcher, built with Kotlin + Jetpack Compose.

## What's implemented

- **Home screen** — big clock + a compact favorites list (max 6 apps), one-handed reach
- **App drawer** (swipe up) — alphabetical list with a right-edge letter index to jump instantly, live search, long-press menu (favorite / hide / app info / uninstall)
- **Widgets page** (swipe left) — placeholder screen ready for `AppWidgetHost` wiring
- **Notification shade** (swipe down from home) — opened via reflection into `StatusBarManager` (best-effort; some OEMs restrict this)
- **Gesture system** — a single `GestureHost` detects up/down/left/right swipes and double-taps anywhere on screen and maps them to actions
- **Theming** — light/dark/system + 7 accent color presets, persisted with DataStore
- **Icon packs** — detects installed Nova/ADW-style icon pack apps and parses their `appfilter.xml` to swap in themed icons
- **Backup/restore** — exports all settings (theme, favorites, hidden apps, icon pack, gesture bindings) to a JSON file via the system file picker, and restores from it

## Project layout

```
app/src/main/java/com/niagaraclone/launcher/
  data/            AppRepository, IconPackRepository, SettingsRepository (DataStore)
  backup/          BackupManager (JSON export/import)
  gestures/         GestureHost (swipe/tap detection)
  ui/
    theme/          Compose theme + accent colors
    screens/        HomeScreen, AppDrawerScreen, WidgetsScreen, SettingsScreen
    LauncherViewModel.kt
  MainActivity.kt
  LauncherApp.kt
```

## Build via GitHub Actions (no Android Studio needed)

This repo includes `.github/workflows/build.yml`, which builds a debug APK on every push to `main` (and can be run manually).

1. Push this repo to GitHub (see below).
2. Go to the **Actions** tab → the "Build APK" workflow will run automatically. You can also trigger it manually via **Run workflow**.
3. When it finishes, open the run → under **Artifacts**, download `niagara-clone-debug-apk`. It's a zip containing `app-debug.apk`.
4. Transfer `app-debug.apk` to your Android phone (email it to yourself, use a cloud drive, or `adb install app-debug.apk` from a computer).
5. On the phone, open the APK file. You'll need to allow "install unknown apps" for whichever app you used to open it (Settings prompts you the first time).
6. Once installed, press the Home button — Android will ask you to pick a launcher; choose "Niagara Clone". You can change it back anytime in **Settings → Apps → Default apps → Home app**.

If you'd rather build locally without Android Studio, the same `gradle assembleDebug` command works from any machine with a JDK 17 and the Android SDK command-line tools installed — Android Studio isn't required, just its underlying toolchain.

## Still to build out (natural next steps)

- Real `AppWidgetHost`/`AppWidgetManager` wiring for the Widgets page (needs a bound `AppWidgetHostService` + `BIND_APPWIDGET` permission grant flow)
- Drag-to-reorder for the favorites list
- Notification dots/badges on app rows (needs a `NotificationListenerService` + user-granted access)
- Gesture-to-action binding UI (the data layer already models arbitrary bindings in `SettingsRepository`/`GestureBinding`, just needs a settings screen)
- Per-app icon size/label tweaks, folder support

## Push to GitHub

From this project root:

```bash
git init
git add .
git commit -m "Initial Niagara-style launcher scaffold"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

(Create the empty repo on GitHub first, without a README/.gitignore, so there's no merge conflict on first push.)
