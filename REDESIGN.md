# Leo TV visual refresh: designs 2-8

## Scope

This branch implements the selected visual direction. Design 1, the collapsing rail, is not included. The permanent 200dp sidebar, its destinations, clock, ordering, hiding and customization remain in place.

This is a review candidate, not a device-verified release. Do not merge or distribute it as ready until the manual checks below pass. The HTML concepts are design references, not screenshots of this Android implementation.

## Changes by design

| Design | Android implementation |
| --- | --- |
| 2. TV artwork | `Apps` reads the activity/application TV banner. Shared `AppTiles` uses that banner with preserved proportions, otherwise the existing shaped app icon. Folders show a four-app preview. Lock and favorite badges are separate from labels. |
| 3. Grid | `TileMetrics` derives columns from the measured grid content area after sidebar and padding. Every row uses the same explicit track width. dp margins and two-line, scaled-text label space replace the fixed-height recent card. Existing icon sizes also select coherent card density. |
| 4. Focus | Shared focus, pressed, selected and dashed moving states. Tiles lift slightly over 160ms unless reduced motion is enabled, including the device animation-scale setting. Stable package/folder tags help return focus. |
| 5. Settings | Appearance, Layout & apps, Family controls, System and Backup tabs. One summary row per setting opens its chooser. Choices are staged until Apply; the theme chooser includes a palette preview. Wallpaper image selection still uses Android's picker. |
| 6. Identity | Shared native sans-serif typography, readable automatic labels, warm amber default, canvas-drawn line icons and palette-aware dialogs. Quick-setting and notification descriptions stack below titles. Existing explicit label sizes and theme choices are retained. |
| 7. Wallpaper | A theme-aware vertical readability overlay sits between the existing custom wallpaper and home content. No live blur or video. The fixed sidebar remains opaque. |
| 8. PIN | Translucent themed PIN panel, consistent line lock icon, readable Light-theme guidance and a compact touch-arrow row. The four-arrow input sequence, Back behavior, PIN modes and verification callbacks are unchanged. |

## Compatibility

- Same package: `com.leo.tvlauncher`. No version/release bump in this review branch.
- Existing preference file and keys are retained. `reduce_motion` is the only new preference, defaulting to false and also respecting the system animation setting.
- `HomePrefs`, `FolderStore`, `PinStore`, `BackupUtil` and `Sidebar` are unchanged from the branch base.
- No rename, clear or migration of favorites, app order, folder identities, hidden apps, renamed labels, kids allowlist, lock hashes, relock timers, wallpaper or sidebar configuration.
- Existing organize, folder, kids-mode, lock and backup callbacks are retained. Retaining their code is not proof that every route has passed a runtime regression test.
- Existing native `aapt2`, `javac`, `d8`, `zipalign` and `apksigner` flow. No new app runtime dependency or framework.
- Same debug-signing arrangement. Updating an existing install needs the same signing key. Do not uninstall an existing setup merely to bypass a signing mismatch, since uninstalling can erase its saved data.

## Checks completed

- APK built with `./build-apk.sh` against Android 34.
- APK signature and zip alignment verified by the existing script.
- `./tests/run-tests.sh`: 483 assertions for measured-content layout arithmetic across multiple widths, densities and all existing icon-size choices.
- Resource XML parsed and `git diff --check` clean.
- Source comparison of the unchanged storage and sidebar helpers.

These are build and host-side checks, not Android instrumented tests. The new arithmetic test is not wired into the existing build-only CI. Run it explicitly with a JDK, `JAVA_HOME`, or the existing sibling `../jdk` setup.

## Still required on a device

The available emulator did not reliably finish installing and launching Leo. No valid screenshot of this Android redesign has been obtained, so layout and D-pad approval remain blocked.

- [ ] Install as an update with a matching key over a configured Leo 2.4 setup. Confirm all saved data survives. First keep an independent backup.
- [ ] Capture actual Home, Favorites, Recent, Folders, Settings, notifications, quick settings and every PIN mode. Inspect all pixels, not just the view tree.
- [ ] Check 720p, 1080p and 4K density, long names, larger system text, Light and dark themes, overscan/safe areas and all five density settings.
- [ ] Check banners, icon-only apps, missing icons, empty folders and four-app folder previews. No cropped labels, scaled focus outlines or last-row stretching.
- [ ] Complete remote-only navigation, fast key-repeat, sidebar-to-content transitions, category changes, chooser Apply/Cancel, PIN entry/delete and return-to-app focus.
- [ ] Exercise favorite, hide, rename, reorder, move cancellation, create/rename/delete folder, folder lock, app lock, relock timer, kids allowed apps and exit.
- [ ] Verify backup export/restore and custom wallpaper selection/clear, including returning from Android's file picker.
- [ ] Check notifications, clear-all, system shortcuts and return paths without claiming the existing safety issues below are fixed.
- [ ] Measure startup, memory use and scroll/focus responsiveness with a large app library on a low-end TV. This branch still eagerly builds a GridLayout; it is not a recycled library or background catalog rewrite.
- [ ] Check accessibility announcements and RTL navigation/text. Existing English strings have not been fully localized.

## Access and permissions audit

- Home: retain the HOME/DEFAULT activity filter and Android 10+ Home-role request. Add HOME package-query visibility for accurate default-Home checks. System settings now always show the current state. A cancelled role request stays cancelled. Older TVs use Home/default-app settings rather than claiming that a generic chooser can replace the default launcher.
- App discovery: retain LAUNCHER and LEANBACK_LAUNCHER queries. No QUERY_ALL_PACKAGES grant. Add Leanback launch fallback for TV-only apps in normal, post-PIN, notification and boot launch paths.
- Notifications: retain the service's system-only BIND_NOTIFICATION_LISTENER_SERVICE requirement and exported=false, matching the current Android reference. Remove the redundant top-level uses-permission, which cannot grant listener access. Show consent guidance before Android settings, prefer the component-specific page on Android 11+, and fall back to the listener list. Detect the exact listener grant, not a package-name substring. Load existing active notifications only after onListenerConnected; drop cached notes on disconnection.
- Wallpaper: use ACTION_OPEN_DOCUMENT for a user-selected image, then copy it into Leo's private files. Remove broad READ_MEDIA_IMAGES and READ_EXTERNAL_STORAGE permissions and their blocking runtime prompt. Backup/restore already use document-picker URI grants and need no broad storage access.
- Boot: retain RECEIVE_BOOT_COMPLETED, the real-boot action check and default-Home guard. This is not a guarantee that a TV vendor will allow a background app launch. No overlay grant is added to evade that restriction.
- Uninstall: retain REQUEST_DELETE_PACKAGES and the system's confirmation. Leo cannot silently uninstall system apps.
- Not needed: overlay, usage stats, an accessibility service, device administration, notification-posting or all-files access. Recents track launches through Leo, not device-wide usage. The existing Accessibility row opens device settings only and does not grant Leo an accessibility service.
- Add an in-app Access & device limits explanation under System. It makes clear that PIN/kids mode only controls routes through Leo, not Android settings, other launchers or a device-owner kiosk. Sleep/restart remain vendor/system-controlled and retain the existing power-action caveat.

Validation: `python3 tests/check-access.py` passes 18 source checks. These are assertions about source structure, not proof of Android/OEM behavior. Device checks still needed: Home role allow/cancel/change-back on API 24/28/29/34+, notification allow/revoke/reconnect with existing notifications, unavailable settings pages, wallpaper without photo permission, boot with and without Leo as default, and a TV-only Leanback package. Check long access text and dialogs visually on a real TV. No runtime visual evidence is claimed.

Android sources used:

- https://developer.android.com/reference/android/app/role/RoleManager
- https://developer.android.com/training/package-visibility/declaring
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/guide/components/activities/secure-bal

## Existing issues outside this visual branch

The source review identified a notification app-launch route that bypasses the common app-PIN helper, insufficient backup schema/size validation, weak PIN retry protection and a Sleep TV label wired to a shutdown request. This visual branch does not fix those policy or behavior issues. They remain release blockers or safety follow-ups as described in the review. A nicer PIN panel is not stronger device security.

## Deliberate differences from the HTML study

- The permanent sidebar stays, so there is less app content width than in the collapsed-rail mockups.
- Artwork comes from installed apps, not embedded demo service logos.
- Home keeps its existing favorites/folders/app ordering and recent shelf instead of adding a new content feed.
- Appearance preview is a staged theme sample; density and text choices use staged choosers, and wallpaper uses the existing Android picker. There is no full interactive home-screen preview for every setting.
- The font is Android's native sans-serif family, not a bundled new font asset.
