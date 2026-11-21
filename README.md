# LeoTVLauncher

A simple Android TV launcher with a Roku-style left sidebar. Plain Java, no Gradle — builds with `aapt2` + `javac` + `d8` (see `build-apk.sh`).

**Package:** `com.leo.tvlauncher`

## Features

- **Sidebar navigation** — Home, Favorites, Recent, Folders, 🔔 Notifications, ⚡ Quick settings, App Lock, Kids mode, Settings. Stays visible on every screen, scrolls, and shows a live clock/date.
- **Notifications tab** — the TV's notifications, newest first; tap one to open its app. Needs notification access (Settings → General → Notification access).
- **Quick settings** — one-press shortcuts to Wi-Fi, Display, Sound, Storage, Date & time, About, plus Sleep TV and Restart TV.
- **Auto-create folders** — sorts apps into 🎬 Video / 🎵 Music / 🎮 Games / 🛠 Tools, with a preview first.
- **Arrow PIN** — 4-press D-pad PIN to lock apps, folders, and Settings itself.
- **Kids mode** — only the apps you allow, PIN to exit.
- **Folders** — create, rename, move apps live with the D-pad.
- **Favorites, recents, hide apps, rename apps** — long-press any tile to organize.
- **Themes, icon shapes/sizes, custom wallpaper**, backup & restore to JSON, open an app on TV start, sidebar customization.

## Build

```sh
./build-apk.sh
```

Requires the Android SDK build-tools 34.0.0 and platform `android-34`, plus a JDK, in the sibling directories the script expects (`../sdk`, `../jdk`), and a debug keystore at `../debug.keystore` (the standard Android debug keystore). Output: `build/out/LeoTVLauncher.apk`, signed with that debug key so updates install over previous versions.

## Status

Current version: **v2.4** (versionCode 15).
