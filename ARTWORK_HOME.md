# Artwork home preview 2

## Target

This pass follows the user's returned design-2 mockup and their photo of the glass build. The target is a calm, artwork-led Home, with Favorites as the large first row and Recent as the smaller second row. It is not another glass treatment. The full 200dp sidebar stays, so the content cannot have exactly the same width as the mockup's rejected narrow rail.

## Implementation

- Saved Favorites move into their own horizontally scrolling Home shelf before Recent. Up to three generous cards fit across the measured content width, with fewer on narrow screens. More favorites scroll horizontally in their saved order, rather than disappearing or being cut to three. No favorite is automatically added or removed. With no eligible favorites, the hero shelf is hidden.
- Recent is below Favorites, with up to four smaller cards across the same measured content width. It keeps the existing recent ordering, hidden-app filtering and exclusion of apps placed in folders. The filled glass shelf container is removed.
- Folders and nonfavorite apps remain below those shelves under Apps & folders, in the existing saved folder/app order. The dedicated Favorites, Recent, Folders and kids views retain their existing routes and filtering.
- Banners fill the clipped 16:9 art stage using proportional center-crop. On unusually shaped banners this may crop edges, so check real installed artwork for clipped brand text. This is a deliberate change from letterboxed FIT_CENTER, not invented artwork.
- Without a banner, hero/Recent artwork uses a dark matte hue sampled from a 24x24 render of the installed app icon, a generous uncropped logo and a native-text app-name lockup. The name respects the user's renamed label. It is not a downloaded brand wordmark or proprietary font. Transparent, nearly neutral and very dark pixels do not wash out the sampled tint; an unavailable/neutral icon uses a neutral matte. Tints are cached only for the current in-memory app catalog.
- In the hero/Recent rows, the logo scales to its art stage rather than obeying a tiny library-icon cap. The existing 24/32dp and other icon/density choices remain effective in the app library and folders. This separation is intentional: the user's newer artwork target should not be shrunk back into five tiny hero cards by an older small-density preference.
- One art surface, restrained corner radius, quiet label underneath and a focus-only 1dp outline. No outer filled card slab. Two-line shelf labels grow when needed instead of reserving a large empty block for every short name. Font size still respects the saved label setting; Auto remains at least 17sp. The app can truncate names that exceed two lines.
- Home uses the selected theme's calm base canvas instead of the prior generated glass atmosphere. Existing custom wallpaper and its corrected scrims still work. The sidebar destinations, labels, styling, clock, order/hide settings and callbacks are unchanged in this pass.
- Focus return searches both the hero shelf and the app grid. Double-Home still reaches favorites. Organize/launch callbacks use the existing handlers and PIN paths. Favorite move mode uses the hero track count for vertical steps and retains saved-order cancellation.
- Settings > System > Visual build identifies Artwork home preview 2. Package/version are intentionally unchanged at com.leo.tvlauncher, 2.4/code 15, so the Android version number is not a reliable visual-build marker.

## Compatibility and scope

No new permission, storage key, migration, dependency or build system. HomePrefs, FolderStore, PinStore, BackupUtil, Sidebar and the manifest remain unchanged. The previous dialog-blur API gates remain. Native inspection led to an opaque dialog-surface fallback when blur is unavailable, avoiding background text showing through the menu. Supported blur-enabled dialogs keep the tinted surface. Earlier notification/PIN, backup validation, PIN retry and power-action risks are not fixed here.

Existing CI still signs each build with a fresh temporary debug key. A new CI artifact will not update an installation signed with a different key. Do not uninstall to bypass this without a backup and explicit permission for the data-erasing reinstall.

## Validation

Completed locally:

- Existing APK build/signature/alignment, XML parsing and diff checks.
- 699 host-side layout assertions and 8,199 icon-tint assertions.
- 18 access-source checks, 24 retained material/alpha guardrails and 18 artwork-composition source checks.
- Actual API-26 Android emulator captures at 1280x720 / 213dpi, inspected in dark and Light themes. Test apps are deliberately named Banner sample, Logo sample and Long fallback name sample. They are synthetic fixtures, not the user's APKs or extracted brand assets.
- Inspected zero-favorite Home, three-across hero composition, smaller Recent, long fallback names, focus outline, five favorites scrolling through the fifth item, and a Tiny library/folder tile.
- Exercised hero launch and return, directional hero/Recent focus, touch long-press organize, and favorite move/cancel. Read back the fixture preference file to confirm cancel restored the saved order. A local matching-key APK update retained the test setup.
- Inspected the settled API-26 organize dialog after the opaque fallback change. Android 12+ real blur remains untested.

These checks are limited. The emulator uses test data, does not reproduce the user's onn TV or real app artwork, and is not full remote/upgrade/performance sign-off. Synthetic fixture APKs are not included in the deliverable app. The user's returned mockup and TV photo remain the target/baseline, not evidence this candidate has passed on their device.

Required device checks:
- Inspect the actual artwork of SmartTube, Salatona, IPTV Smarters Pro, LeoLink TV, Yacine TV, Haystack News, Downloader, NETFLY, Play Store and Projectivy, including center-crop edge clipping.
- Exercise a truly bannerless app, a neutral/transparent icon, long English/Arabic labels and renamed labels. Check the large logo/name lockup does not overflow or appear too small.
- Inspect hero and Recent at 720p/1080p/4K density, Light/dark themes and larger text. The full sidebar deliberately uses more width than the reference rail.
- Try zero, one, three and many favorites; favorites inside folders/hidden apps; empty Recent; and kids mode. Confirm no app becomes unreachable.
- Navigate sidebar -> Favorites -> Recent -> library with the D-pad, including horizontal scrolling, favorite add/remove/hide, long-press organize, move/cancel and double-Home focus.
- Upgrade with a matching signing key and confirm saved app order, favorites, folders, wallpaper, labels, PIN/kids settings and sidebar customization survive.

Visual status must be reported from actual runtime evidence, separately from the user's target mockup or an HTML illustration. No claim of pixel-perfect matching or TV sign-off follows from a successful build.
