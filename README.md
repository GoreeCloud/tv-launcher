# GoreeCloud TV Launcher

An original, native, D-pad-first home screen for **Android TV and Google TV**, built for direct APK installation. Development preview v0.1.3.

## Features

- Android `HOME` intent handler and Android TV `LEANBACK_LAUNCHER` entry point.
- Display installed TV apps and compatible launchable apps; launch with remote OK.
- Original cinematic-inspired composition: fullscreen backdrop, translucent floating favorites dock, large D-pad focusable app tiles, and an installed-app library.
- User-selected local image backgrounds through Android's document picker where available, plus private ADB file import on TVs with no working picker; no promotional imagery is bundled.
- Local-only app search and pinned/favorite tiles (long-press OK or press Menu to toggle).
- Current date/time, app refresh, TV settings shortcut, and user-driven Home selection.
- Visible first-focus affordance, D-pad focus outlines, large cards, landscape/overscan spacing and no touch requirement.
- Zero network permission, no telemetry, ads, usage stats, sensitive accessibility services, or device-admin access.
- Original GC UI-inspired semantic foreground, surfaces and focus states; colors are this app's design choice, not ecosystem brand defaults.

**Known limits:** OEM firmware may block third-party HOME selection; Google TV manufacturer launchers can impose restrictions. The app never disables a system package, suppresses OTA updates or auto-claims the Home role. Pinned apps are local and may disappear if uninstalled. The installed preview was launched on an onn. 4K Streaming Box with stock Home fallback retained; current redesigned version still requires fresh physical-TV D-pad and accessibility acceptance. Wallpaper selection depends on the TV having a compatible document picker.

## Build

Use JDK 21 and the Android SDK platform 35 with build tools 35+.

```sh
./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. This is a debug-signed **development** build, not a stable release.

## Wireless ADB installation

On TV: **Settings → Device Preferences / System → About → Android TV OS build** (click several times to enable Developer options), then **Developer options → Wireless debugging** where supported. Use a trusted private LAN. On Android 11+ pairing-capable devices, use the displayed IP and *pairing port*, enter the on-screen code, then connect to the distinct *debugging port*.

```sh
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:DEBUGGING_PORT
adb -s TV_IP:DEBUGGING_PORT install -r app/build/outputs/apk/debug/app-debug.apk
adb -s TV_IP:DEBUGGING_PORT shell am start -n com.goreecloud.tvlauncher/.MainActivity
```

For older Android TV ADB-over-network firmware, the procedure differs and may require USB to enable TCP/IP debugging first. Never expose ADB to the public Internet. Do not run `pm disable-user`, uninstall the original launcher, or alter device-owner settings to force a default.

In GoreeCloud TV, use **Choose Home** and complete the system confirmation if supported, then press Home to test. If the firmware does not offer the chooser, the app can still be opened as a normal TV app. Exact wireless ADB pairing and default selection depend on TV firmware.

## Private backdrop import for TVs without an image picker

Some Google TV firmwares resolve `ACTION_OPEN_DOCUMENT` only to a nonfunctional system stub. For **debug development builds only**, a trusted computer can copy an image that you personally own or are licensed to use into the app's private directory. No network, storage or gallery permissions are needed:

```sh
# Provide a local image (JPEG or PNG; BitmapFactory identifies its content).
# The package is debuggable; never expose wireless ADB outside your private LAN.
adb -s TV_IP:DEBUGGING_PORT exec-in run-as com.goreecloud.tvlauncher sh -c 'mkdir -p files && cat > files/backdrop.jpg' < /path/to/your-owned-image.jpg
```

Reopen GoreeCloud TV to load `files/backdrop.jpg`. The selected document-picker image, if any, takes priority. **Reset** removes the picker selection and falls back to this private file or the default gradient; it never deletes private user artwork. To remove your ADB-loaded image, do so explicitly with the app's `run-as` access. Limit images to practical TV resolution (ideally 1920×1080 or 3840×2160); the app downsamples large images. Release builds cannot rely on `run-as` and must use a separately tested provider/permission UX. The founder-provided reference screenshots are not bundled, transferred or used as wallpaper assets.

## Architecture

Single-activity native Java Android app with first-party Android SDK views (no runtime third-party dependencies). Installed apps are discovered through scoped manifest `<queries>` for launchable activities, *not* `QUERY_ALL_PACKAGES`. Only favorite component IDs are stored in Android private preferences. App backups are disabled. No Internet permission.

## Release and rollback

For testing, keep the stock launcher enabled and use the TV's Settings to switch Home back. APK uninstall: `adb uninstall com.goreecloud.tvlauncher` *only after* restoring another default Home and verifying its Home button behavior. Do not uninstall while the app is the only usable Home handler. Debug APKs are for development. A production release requires a release key, TV-specific tests, accessibility audit and security review.

Licensed under MIT. See [LICENSE](LICENSE). The Gradle wrapper code is distributed under its own Gradle license.
