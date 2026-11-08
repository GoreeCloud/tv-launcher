# GoreeCloud TV Launcher — Physical Release Acceptance Checklist

**Status: OPEN.** This checklist is for authorized, attended physical-device verification. Passing emulator CI and a read-only APK signature check are not a Stable release, Home-default certification, or permission to interrupt a household TV session.

## 1. Preconditions (all required before physical interaction)

- The founder confirms the TV is available and approves a test session. If an unrelated app is playing or the TV is otherwise in use, **stop; perform no input, install, stop-app, launcher-role, display, or accessibility mutations.**
- Identify the exact owned TV by Android model/serial and a private-network wireless ADB endpoint. Explicitly target the TV; never the attached phone, an inferred IP, or a different ADB device. Do not publish the device ID, LAN address, user screenshots, pairing code, or private preference contents.
- Record the exact intended local APK path, SHA-256, `versionName`, `versionCode`, and **local signing-certificate digest**. Run `.github/scripts/tv-update-preflight.py` read-only against the exact TV before any planned update. Stop if it rejects any package, signer, permission, device, or version property.
- Have a protected backup of the currently installed app APK and the **user-authorized** data required to restore settings. Confirm the rollback method works for the candidate's signer and Android version; keep the OEM's stock launcher installed, enabled, and usable.
- **Never install a GitHub-hosted debug APK over the separately locally signed preview.** A matching release signer and an intentionally approved migration plan remain mandatory for a future Stable release. Do not uninstall to work around `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

## 2. Interactive TV behavior

Record **PASS / FAIL / BLOCKED / NOT TESTED** for each case, including device firmware and exact app version:

| Case | Required evidence and acceptance |
| --- | --- |
| Initial Home | One top **YOUR APPS** section and no separate **FAVORITES** section; founder reviews actual composition and readability against their authorized mockup |
| D-pad | Focus is always visible and stays on an actionable control while navigating more apps than fit on one row; scroll reaches both ends |
| App return | Launch at least two real installed apps, return with Back and Home where appropriate, and check selected-app focus and preserved search state |
| Search | Use the TV's actual remote/keyboard to filter to one real app, open it with OK, return, reset to all apps, and test no-match recovery |
| Stale apps | After a controlled, authorized app install/removal (or on disposable emulator), the dock, recent shortcuts, and recommendations do not display uninstalled entries |
| Wallpapers | Select each of the four first-party scenes, restart the launcher and verify persistence; personal document picker works only where firmware offers an actual provider |
| Home default | Examine both Android HOME role holder **and** actual `MAIN/HOME` intent resolution; a role grant alone is insufficient. Only use OEM-supported user-choice UI, never disable/remove stock Google TV Home; prove immediate stock fallback |
| Return to normal | Restore the user’s original app/session, display/accessibility preferences, and stock-Home behavior. Leave debugging off after any approved QA session when appropriate |

## 3. Visual, accessibility, and performance gates

- Verify native 720p, 1080p and **genuine UHD/4K** output on hardware that supports each mode. A 4K model label, requested `wm size`, upscaled 1080p image, or ignored emulator override does **not** prove 3840×2160 rendering. Check the actual composed frame/capture size and TV display mode independently.
- Check screen edges, overscan, text scaling (including larger Android accessibility font/display sizes), contrast of text and selected focus ring across all four backgrounds, scroll clipping, and readable focus in every rail.
- Test with real Android TalkBack/screen reader enabled by the **user**: each actionable tile/button must have a meaningful spoken label and coherent order; purely decorative artwork should not be announced. Leave these checks **NOT TESTED** if no actual assistive-device session occurs.
- Measure startup and return-to-home timing on an idle device and watch for crashes, memory pressure, battery/network side effects, and unexpected permission prompts. No performance SLA or accessibility pass is asserted without measurements.
- Keep device screenshots, TalkBack logs and real app inventory **private**. Public GitHub CI fixtures use synthetic labels only; do not upload user viewing data.

## 4. Result and rollback record

Record date, firmware, package/version, APK digest, on-device signer compatibility, test cases and their actual outcomes, visible defects, user consent/acceptance, and the verified method to return to the OEM Home app. Synchronize only verified progress with the canonical GoreeCloud Drive project records and Todoist. A failed or skipped acceptance case remains open and **blocks Stable**.

**Current known boundary:** public source v0.2.0-alpha.6 is a development candidate under test; the last verified physical TV installation was v0.2.0-alpha.4, and its effective Home target was stock Google TV. No unattended installation or Home replacement is authorized by this checklist.
