# GoreeCloud TV Launcher — Visual Direction (November 7, 2026)

Status: founder-supplied inspiration captured as design direction; **not** approval to redistribute reference imagery, copy platform branding, or claim visual acceptance. Scope: native Android TV / Google TV. GoreeCloud UI is the sole governing design language; no ecosystem-wide mandatory color.

## Observed visual principles

The seven founder-provided TV interface examples emphasize (1) immersive edge-to-edge backdrops, (2) clean identity and a compact clock near the upper corners, (3) a floating, softly translucent, rounded horizontal TV-app dock anchored in the lower visual field, (4) generous icon spacing with strong contrast and distinct remote highlight, and (5) supporting content and app-discovery rows beneath the dock. Several include streaming promotional posters or a recommendation row. Those assets and third-party brand graphics are **reference material only**; no licensed content, screenshots, external tracking, or simulated Continue Watching is bundled or represented as functional.

## Native implementation plan

- Use an original immersive composition instead of reproducing proprietary layouts. Place GoreeCloud identity at top-left, a readable clock capsule top-right, a user-owned wallpaper area, a single Your Apps dock, and truthful recently opened / app-discovery rows beneath.
- Use platform-supplied icons from installed apps; do not clone logos from the reference images. Show all installed launchable apps in the top Your Apps dock; optional locally prioritized apps appear first without hiding others.
- Allow the owner to select a background locally via Android's scoped document picker where the TV provides one. No Internet permission, advertising, login, telemetry, or third-party artwork downloads. Use a built-in gradient if image access is unavailable.
- Use rounded translucent surfaces, readable scrims, focus rings, focus enlargement, accessible labels, and remote OK, Back, directional keys, long-press OK, and Menu handling. Ensure important controls are accessible without touch.
- Do not invent a profile avatar, functional recommendation feed, streaming watch history, or content-entitlement integration where none exists.
- Retain stock Google TV Home as a reversible fallback. The custom app may request user-mediated Home selection only.
- Keep this product's colors independent; verify contrast/legibility under custom light/dark imagery. Avoid claims of completed GoreeCloud UI acceptance without TV screenshot review and accessibility tests.

## Verification gate

1. Build debug APK, verify the current JVM app-order, app-filter, and Home-routing tests, and require Android Lint 0 errors.
2. Install verified current APK on authorized onn. 4K Streaming Box via wireless ADB, after safeguarding earlier preview and confirming non-default status.
3. Check visible 1920x1080 screenshot and hierarchy; exactly one top Your Apps row and no Favorites section, D-pad focus movement, readable gradient app cards, horizontal scrolling, tile launching, priority persistence, search and absence of crashes.
4. Verify any document picker resolution and permission behavior without importing promotional art. Actual wallpaper-selection user experience remains provisional if not tested end to end.
5. Press physical/Home input and confirm return to original system Home without disabling, uninstalling, or changing unrelated packages. No unsupported default-Home reassignment.
6. Publish tested source and evidence, update relevant Drive status and Todoist, distinguish installed preview from stable release.

All reference imagery stays in the founder-provided conversation; none is checked into the public repository.

## Implemented v0.2.0-alpha.2

Original four-wallpaper and official product identity source is maintained in [brand-assets PR #14](https://github.com/GoreeCloud/brand-assets/pull/14); Android consumer copies are checksum-pinned. The hero, wallpaper selection controls, floating D-pad dock, recent app shortcuts (not episode history), locally selected installed-app recommendations, and searchable app grid are implemented in native source. These are not claims of a real streaming/watch-position service or an independently accepted Stable release. Founder mockup pixels are not bundled.

## Visual refinement / scene acceptance

The four wallpaper rasters were re-rendered with richer original planetary shading and atmosphere in central brand commit `0b9a767`, merged as [brand-assets PR #14](https://github.com/GoreeCloud/brand-assets/pull/14). The launcher includes brighter hero lighting, original wallpaper-backed launch-shortcut cards with truthful Open App labels, and an installed-app-only Recommendations rail. All four scene choices were confirmed on the physical onn. TV as persisting correctly and the previous scene selection was restored. Default on a clean install remains Horizon Expedition. Do not infer actual streaming recommendations, video history or media playback.

## Founder refinement — single top Your Apps rail (November 7, 2026)

**Direction:** The current physical TV experience is close to the supplied mockup, but visual parity is not yet accepted. Remove the visible **Favorites** section and keep **Your Apps** as the only installed-app section at the top, immediately after the hero. Do not create or copy images as part of this change.

**Implementation in alpha.4 source:** The floating rounded dock is labeled **YOUR APPS**, holds every installed launchable app, and scrolls naturally with the D-pad. Existing prioritized apps move to the front without hiding unpinned apps. The redundant lower app grid, separate Favorites/Quick Launch heading and bottom search field are removed. A focused **Find an app** action filters this single row. Tinted native gradient app cards, a larger hero title and content-rail ordering (recent apps, installed app discovery, then original offline worlds) bring the composition nearer the founder's screenshot while respecting GC UI hierarchy, text contrast and original first-party art. The original four wallpapers and identity are unchanged.

**Acceptance limits:** Source/build CI and Android emulator XML checks cannot stand in for the founder's visual acceptance on the TV. Test the actual physical display, remote focus and long rail at 720p/1080p/4K where supported; document whether an idle TV is available before any non-disruptive installation. Do not disable the stock Home app.
