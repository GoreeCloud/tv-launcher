# GoreeCloud TV Launcher

**v0.2.0-alpha.6 · Native Android TV / Google TV · Development preview**

The authorized onn. TV remains on v0.2.0-alpha.4. Source alpha.6 retains alpha.5's verified search-dialog focus fix and adds return-from-app focus preservation and stale-app inventory recovery; it is not installed, human-approved, or Stable.

An original, offline-first, D-pad-native launcher inspired by the founder's cinematic TV mockup. This is a source-level prototype and not an Apple TV replica, a licensed streaming service, a video player or a Stable release.

## Implemented home UI

- **Immersive hero:** featured first-party wallpaper, GoreeCloud TV logo, clock, original world title/description, real Explore Apps and Choose Wallpaper controls, and four selectable scene indicators.
- **Your Apps at the top:** one floating translucent, horizontally scrollable dock immediately below the cinematic hero, with all installed launchable apps, colored accent gradients, high-contrast remote focus, and optional priority ordering. There is no separate Favorites section or duplicated app grid.
- **Continue in Your Apps:** actionable recently launched app shortcuts stored only in private on-device preferences. The empty state shows launchable app shortcuts; this is explicitly **not** a fabricated Continue Watching history or episode-progress feed.
- **GoreeCloud Worlds:** a horizontally scrollable, D-pad-operable visual rail to select one of four bundled original wallpapers.
- **Recommended for You:** a discoverable carousel of actual installed apps. The selection is deterministic and local; no behavioral personalization, sponsor payments, external recommendations or tracking.
- **App search and settings:** Find an app filters the single top Your Apps dock; TV settings, Home-role request (where permitted), and the clock remain available.
- Remote OK to launch, Menu or long-press OK to prioritize/deprioritize an app within the same Your Apps row, keyboard/remote focus retention, actual installed app icons, scalable cards, touch-free controls and Android TV banner registration.

### Four built-in original wallpapers

All wallpapers are **1920×1080 WebP**, permanently packaged in the APK, selectable entirely offline:

| Name | Role | Scene |
| --- | --- | --- |
| **Horizon Expedition** | Default | Sunset planetary expedition and future skyline |
| **Aperture City** | Built-in | Violet celestial city with skyline |
| **Cloudforge Peaks** | Built-in | Mountain cloudscape and floating natural islands |
| **Neon Harbor** | Built-in | Night-lit skyline over a reflective harbor |

**Wallpaper menu:** select *Choose wallpaper*, a world scene card or hero indicator. Selected wallpaper persists in private local settings. No Internet access or image-fetching service.

**Optional personal image:** Android's scoped system document picker is used where implemented. On some Google TV firmwares, the OS resolves the picker to a nonfunctional stub. For *debug builds only*, an authorized trusted computer can load a file locally through wireless ADB:

```sh
adb -s TV_IP:DEBUGGING_PORT exec-in run-as com.goreecloud.tvlauncher sh -c 'mkdir -p files && cat > files/backdrop.jpg' < /path/to/your-owned-image.jpg
```

Then select **Use my own image** in the wallpaper chooser. This image stays in the app's private files; it does not become part of the public repository. Choose *Horizon Expedition* to restore the default. No third-party streaming artwork, copyrighted promotional images or screenshots from the provided mockup are bundled.

## Official GoreeCloud TV identity

The first-party branded cloud-and-TV icon, light/dark vector wordmarks, native raster wordmark, TV banner and four wallpaper masters are maintained in [GoreeCloud/brand-assets](https://github.com/GoreeCloud/brand-assets/tree/main/products/tv-launcher) at pinned commit `0b9a767bf8dc9a6587b3a7d7eec2206d1568f93f`. The seven packaged consumer images are verified byte-for-byte by [the asset pin manifest](docs/BRAND_ASSET_PIN.json) and `.github/scripts/verify-asset-pin.py`. No default Android framework identity remains in the APK manifest. GoreeCloud UI remains the sole interface design-language authority; its color freedom applies here.

## Privacy, security and capabilities

The launcher requests **no Internet permission**, no package-wide visibility, usage-history or accessibility-control permissions, and no analytics. Only locally selected wallpaper, optional app ordering and recently launched app component identifiers are stored in app-private preferences; Android backup is disabled, including on legacy Android versions with fullBackupContent=false. The original Google TV Home stays installed/enabled. Firmware-specific third-party HOME selection remains user-mediated and may be blocked by an OEM.

**Content integrity:** There is no real media-provider account, title recommendation engine, video playback backend or TV Watch Next entitlement. Hero artwork is labeled as original scenery rather than pretend playable content; all app cards actually open installed apps. Real cross-app watch progress is not claimed.

The isolated Google TV emulator QA suite also checks returning from an opened
synthetic application twice with its D-pad focus restored and search filter
retained, then uninstalls the 12-entry disposable fixture and confirms stale
dock, recent, and recommended app entries vanish. These are emulator-only
functional checks, **not** physical-TV approval.

## Build and validate

Requires JDK 21 and Android SDK 35:

```sh
python3 .github/scripts/verify-asset-pin.py
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (debug-signed). CI also runs an isolated Google TV emulator smoke workflow; read the **current run conclusion** before citing it as successful.

For physical release gates, use the [attended TV acceptance checklist](docs/PHYSICAL_ACCEPTANCE.md). Emulator success is not a substitute for hardware, accessibility, effective Home recovery or founder signoff.

## Wireless ADB to your own Google/Android TV

Pair wireless debugging on the private LAN (when supported) and explicitly target your TV's serial, never a connected phone:

```sh
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:DEBUGGING_PORT
adb -s TV_IP:DEBUGGING_PORT shell getprop ro.product.model
adb -s TV_IP:DEBUGGING_PORT install -r app/build/outputs/apk/debug/app-debug.apk
adb -s TV_IP:DEBUGGING_PORT shell am start -n com.goreecloud.tvlauncher/.MainActivity
```

**Before any installation attempt, run the [read-only, fail-closed TV update preflight](docs/SIGNING_AND_ROLLBACK.md).** It compares the candidate's signed identity and version to the already-installed APK and blocks a different debug signer without touching app data or TV Home. Never skip backups or visual approval merely because this compatibility check passes.

Never expose ADB to the public Internet, disable/uninstall the original TV launcher, or force a HOME role with system package disabling. The **Choose Home** button requests the system role where supported. A TV may report that this role is held while its manufacturer Home still handles the Home key. The app now checks the resolved Home activity and provides a non-destructive explanation and system Home settings option. Installing an APK or obtaining a role alone does not prove effective default routing. Debug signing certificates can differ across machines and prevent in-place updates; back up the prior build and data and assess a rollback before replacing.

## Additional package and small-screen release checks

The dedicated [packaged privacy check](.github/scripts/verify-package-privacy.py) verifies APK signature, HOME/LEANBACK registration, the official icon/banner/wordmark and all four wallpapers, zero requested Android permissions, disabled backups and cleartext, and archive path safety. GitHub Actions exercises an isolated Google TV API 34 emulator at **1920×1080 and 1280×720**, checking actual screenshot dimensions and actionable D-pad focus. **No automated UHD coverage is claimed**: the hosted Google TV image ignored attempts to change its size to 3840×2160 and remained 1920×1080. A dedicated 4K-compatible device or emulator is still required for native UHD QA and founder visual acceptance. These automated checks are stronger evidence than source declarations alone but do not replace physical TalkBack or 4K acceptance.

## Known acceptance gaps

The **emulator-only** `qa-fixture` module declares twelve synthetic launchable aliases with no app permissions. Its separate APK is never shipped as the GoreeCloud product, never uploaded as a user install artifact, and must not be installed on an actual phone/TV. The smoke harness refuses any device except an isolated ADB `emulator-5554`, then exercises real D-pad movement in an overfull Your Apps rail, app search/filter/reset (including a no-match focus recovery) and launching/returning from a synthetic app. These are **emulator functional tests**, not physical-TV or Assistive Technology certification.

Device-specific accessibility (TalkBack, reduced motion/high contrast), 720p/4K visual review, OEM Home chooser limitations, real personal-file picker usability, cross-device installation, long-term stability and signed release packaging are separate gates. The app is not production-certified.

Copyright (c) 2026 GoreeCloud. Source license: MIT; see [LICENSE](LICENSE). Product artwork ownership/provenance is documented separately in the central brand-assets repository.

## Single Your Apps rail (alpha.4)

The founder-approved launcher layout places **Your Apps** immediately below the cinematic featured wallpaper. This one translucent, D-pad-scrollable rail with contrasting original accent gradients exposes **all installed launchable apps**, with no separate Favorites area or duplicated lower app grid. **Find an app** filters that same row; **Show all apps** clears the filter. Holding OK / Menu can optionally prioritize an app at the front of **Your Apps** without hiding others. The user-selected order is kept locally using the earlier persisted preference for backward compatibility. Below are local recently opened apps, installed-app suggestions and the four original offline wallpapers. The wording makes no claim of actual streaming playback/watch progress.
