# GoreeCloud TV Launcher — Visual Direction (November 7, 2026)

Status: founder-supplied inspiration captured as design direction; **not** approval to redistribute reference imagery, copy platform branding, or claim visual acceptance. Scope: native Android TV / Google TV. GoreeCloud UI is the sole governing design language; no ecosystem-wide mandatory color.

## Observed visual principles

The seven founder-provided TV interface examples emphasize (1) immersive edge-to-edge backdrops, (2) clean identity and a compact clock near the upper corners, (3) a floating, softly translucent, rounded favorites dock anchored in the lower visual field, (4) generous icon spacing with strong contrast and distinct remote highlight, and (5) a multi-row app library beneath the dock. Several include streaming promotional posters or a recommendation row. Those assets and third-party brand graphics are **reference material only**; no licensed content, screenshots, external tracking, or simulated Continue Watching is bundled or represented as functional.

## Native implementation plan

- Use an original immersive composition instead of reproducing proprietary layouts. Place GoreeCloud identity at top-left, a readable clock capsule top-right, a user-owned wallpaper area, a floating favorites dock, and a searchable local app library beneath.
- Use platform-supplied icons from installed apps; do not clone logos from the reference images. Prioritize explicitly pinned favorites, falling back to installed apps when no pins exist.
- Allow the owner to select a background locally via Android's scoped document picker where the TV provides one. No Internet permission, advertising, login, telemetry, or third-party artwork downloads. Use a built-in gradient if image access is unavailable.
- Use rounded translucent surfaces, readable scrims, focus rings, focus enlargement, accessible labels, and remote OK, Back, directional keys, long-press OK, and Menu handling. Ensure important controls are accessible without touch.
- Do not invent a profile avatar, functional recommendation feed, streaming watch history, or content-entitlement integration where none exists.
- Retain stock Google TV Home as a reversible fallback. The custom app may request user-mediated Home selection only.
- Keep this product's colors independent; verify contrast/legibility under custom light/dark imagery. Avoid claims of completed GoreeCloud UI acceptance without TV screenshot review and accessibility tests.

## Verification gate

1. Build debug APK and 4 current JVM app-filter tests; Android Lint 0 errors.
2. Install verified current APK on authorized onn. 4K Streaming Box via wireless ADB, after safeguarding earlier preview and confirming non-default status.
3. Check visible 1920x1080 screenshot and hierarchy; D-pad focus movement, readable app cards, scrolling, tile launching, pin/unpin persistence, search and absence of crashes.
4. Verify any document picker resolution and permission behavior without importing promotional art. Actual wallpaper-selection user experience remains provisional if not tested end to end.
5. Press physical/Home input and confirm return to original system Home without disabling, uninstalling, or changing unrelated packages. No unsupported default-Home reassignment.
6. Publish tested source and evidence, update relevant Drive status and Todoist, distinguish installed preview from stable release.

All reference imagery stays in the founder-provided conversation; none is checked into the public repository.
