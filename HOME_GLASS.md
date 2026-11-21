# Glass home preview 1

This is the second, unpublished review pass after the user's onn-TV photo. It also incorporates the subsequent request for the latest macOS style. It is a native Android Views interpretation, not macOS, an Apple asset pack or an implementation of Apple's Liquid Glass renderer.

## Visible changes

- Single clipped 16:9 artwork surface with its label on the canvas. No outer filled card slab. Banner proportions remain intact. Icon-only apps use centered, shaped icons on a tinted material surface. Favorite and lock indicators are smaller and neutral, not bright accent decorations.
- A 1dp focus outline and 1.025x focus scale. Unfocused app artwork has no accent outline. Existing reduced-motion behavior remains. Native Android elevation supplies shadows; there is no ray-traced lighting, lens refraction or dynamic optical distortion.
- Permanent 200dp sidebar and all existing destinations, ordering, hidden items, clock and customization are retained. The sidebar's styling changes: rounded tinted panel, fine line symbols instead of emoji in live navigation, a quieter brand and light-weight clock. Customization labels keep their existing definitions. No sidebar storage migration.
- Dark presets use neutral surfaces and restrained preset-specific accents. The existing theme IDs remain, but their dark palette values intentionally change. Ocean therefore does not keep the old saturated blue slabs. Light stays light. Midnight's amber is replaced with a muted cool accent to follow the user's later macOS-style request.
- A built-in neutral gradient atmosphere is visible without selecting a wallpaper. Custom images still use the existing picker and saved file. Home panels use layered translucent gradients, NOT live background blur.
- Recent remains in the same place, with the same entries and behavior. Smaller 136dp tiles sit in a rounded, tinted dock-like shelf. It is not a relocated macOS bottom Dock or a new favorites workflow.
- Folder covers use one matte material and up to four app icons in a 2x2 mosaic, in the saved folder order. No tiny banner collage. The title sits below with a folder line symbol. Empty folders use a single folder symbol.
- Default Comfortable fits four tracks at 704dp of post-sidebar content. A library of 1-8 tiles is capped at four tracks when space permits, except for explicit Tiny. Narrow screens use fewer tracks, rather than squeeze labels. Larger libraries keep width-based density. Gutters stay 16dp between tracks.
- Layout density now includes Tiny 24dp and Very compact 32dp, alongside all five old numeric values. The existing icon_size preference is reused without resetting it. Tiny can fit five 124dp cards in a 704dp content area. These are maximum icon sizes; banners fill their card art area and shrink when density produces smaller cards. Auto app labels remain at least 17sp; existing explicit text-size choices are preserved. Tiny is an optional compact setting, not a claim that every icon is readable across every room.
- Custom-wallpaper scrims explicitly strip the existing alpha before adding it. Horizontal alpha varies from 160 to 64; vertical alpha from 48 to 128. The resulting combined tint is about 44%-81% opaque depending on position, never the previous fully opaque bottom. Exact readability remains to be checked against the user's image and TV.
- Settings > System > Visual build identifies this candidate as Glass home preview 1. Package version remains 2.4 / code 15, so the Android version number alone cannot distinguish the builds.

## Real blur versus approximation

`Glass.dialogShown` uses Android's actual blur-behind API only on Android 12/API 31 or newer and only while WindowManager reports cross-window blur enabled. A listener adapts to runtime enable/disable changes and is removed on dialog dismissal. Supported dialogs request a modest 16dp blur behind their window. Unsupported devices/OS versions use the tinted dialog surface; when blur is disabled, the dim amount is stronger. No overlay, accessibility or other new permission is required.

This effect applies to a different window behind the dialog. It does NOT blur Leo's own Home/sidebar/shelf background. Home materials are translucent-gradient approximations at all API levels. No per-frame wallpaper capture, RenderEffect on the app's text/icons, or CPU bitmap blur was added. The Apple design's continuous-corner optics and refraction are not reproduced; native rounded rectangles approximate that shape language. Apple fonts, logos and proprietary assets are not bundled.

Design and API references checked for this pass:
- https://www.apple.com/os/macos/
- https://source.android.com/docs/core/display/window-blurs

## Validation

Completed locally:
- Existing aapt2/javac/d8 build, signature verification and zip alignment.
- 683 host-side layout assertions, including smaller presets, four-track small-library layout and narrow-screen fallback.
- 18 source-level access checks from the previous audit.
- 24 structural/alpha guardrails for Home materials, outline state, small presets and blur gating. These do not validate appearance.
- XML parsing and diff whitespace checks.
- Source comparison: HomePrefs, FolderStore, PinStore, BackupUtil and the manifest remain unchanged from main.

Not completed:
- No valid screenshot of this candidate running on Android exists. The saved emulator snapshot produced graphics-buffer errors. A bounded cold boot reached Android, but installation and screen capture did not finish reliably. Both emulator attempts were stopped. The user's earlier photo is evidence for the previous build only.
- Real TV pixel inspection, D-pad navigation, focus-ring clipping/shadow visibility, folder mosaic at Tiny, long labels and Arabic text, Light/dark contrast, custom-wallpaper readability, dialog blur enabled/disabled transitions, performance and upgrade tests remain required.
- No update signing compatibility is claimed. Existing CI still makes a fresh debug key per run. Do not uninstall an existing setup to bypass a signature mismatch without backing up and explicit user permission.
- Earlier documented notification/PIN bypass, backup validation, PIN retry and power-action concerns are not fixed by this visual pass.

This is a source/build-checked review candidate, not a visually approved release. Do not treat an HTML illustration, CI success or a successful APK export as proof of its appearance on the user's TV.
