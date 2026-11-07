# GoreeCloud TV Launcher

An original, native, D-pad-first home screen for **Android TV and Google TV**, built for direct APK installation. Development preview v0.1.0.

## Features

- Android `HOME` intent handler and Android TV `LEANBACK_LAUNCHER` entry point.
- Display installed TV apps and compatible launchable apps; launch with remote OK.
- Local-only app search and pinned/favorite tiles (long-press OK or press Menu to toggle).
- Current date/time, app refresh, TV settings shortcut, and user-driven Home selection.
- D-pad focus outlines, large cards, landscape/overscan spacing and no touch requirement.
- Zero network permission, no telemetry, ads, usage stats, sensitive accessibility services, or device-admin access.
- Original GC UI-inspired semantic foreground, surfaces and focus states; colors are this app's design choice, not ecosystem brand defaults.

**Known limits:** OEM firmware may block third-party HOME selection; Google TV manufacturer launchers can impose restrictions. The app never disables a system package, suppresses OTA updates or auto-claims the Home role. Pinned apps are local and may disappear if uninstalled. TV-specific acceptance, visual QA, TalkBack testing, and long-press and Menu-key behavior vary by remote and remain pending until tested on an actual TV.

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

In GoreeCloud TV, use **Set default Home** and complete the system confirmation if supported, then press Home to test. If the firmware does not offer the chooser, the app can still be opened as a normal TV app. Exact wireless ADB pairing and default selection depend on TV firmware.

## Architecture

Single-activity native Java Android app with first-party Android SDK views (no runtime third-party dependencies). Installed apps are discovered through scoped manifest `<queries>` for launchable activities, *not* `QUERY_ALL_PACKAGES`. Only favorite component IDs are stored in Android private preferences. App backups are disabled. No Internet permission.

## Release and rollback

For testing, keep the stock launcher enabled and use the TV's Settings to switch Home back. APK uninstall: `adb uninstall com.goreecloud.tvlauncher` *only after* restoring another default Home and verifying its Home button behavior. Do not uninstall while the app is the only usable Home handler. Debug APKs are for development. A production release requires a release key, TV-specific tests, accessibility audit and security review.

Licensed under MIT. See [LICENSE](LICENSE). The Gradle wrapper code is distributed under its own Gradle license.
