# GoreeCloud TV Launcher — Signing, Upgrade and Rollback Gates

**Status:** Development guidance and verified alpha.4 constraints; **no Stable signing key provisioned or production upgrade authorized**.

## Current proof and limitation

- Native package: `com.goreecloud.tvlauncher`; current development source preview 0.2.0-alpha.6 / versionCode 10. The physical TV still has 0.2.0-alpha.4 / versionCode 8 and its locally installed debug signer.
- On the authorized onn. 4K Streaming Box, the locally installed alpha.4 APK was built using a privately held local **debug** signer. The hosted GitHub Actions debug APK from the exact merged source uses a **different debug signer**. Equal package names, code revisions and version numbers do not override Android's certificate verification.
- Prior local APK and private preferences were backed up on the authorized laptop before a previously verified in-place alpha.4 update. These are rollback evidence, **not** an accepted release-key migration.
- Android currently lists GoreeCloud as a Home role holder but still resolves HOME to stock Google TV. Never disable, uninstall, or alter the manufacturer launcher.

## Read-only operator preflight

Build a candidate APK with an authorized, trusted JDK/Android SDK toolchain. Then run:

```sh
export ANDROID_HOME="$HOME/Android/Sdk"
python3 .github/scripts/tv-update-preflight.py \
  --serial "TV_IP:DEBUG_PORT" \
  --expected-model "onn. 4K Streaming Box" \
  --apk "/trusted/local/path/launcher.apk"
```

This script **never installs an APK**. It verifies an exact online ADB serial and device model, package identity, absence of newly requested permissions, non-decreasing versionCode and the cryptographic signing-certificate match to the installed `base.apk`. It makes a private temporary copy of the installed base APK via `adb pull` and automatically removes that copy. Failed checks exit nonzero without uninstalling, clearing preferences or sending remote keys. The script is **not** a malware scanner, a supply-chain audit, a backup implementation or a complete production release approval. Keep wireless debugging LAN-only and close it when QA is finished.

## Protected production signing plan (still pending)

1. Establish a **dedicated release-signing identity**, protected outside public source and GitHub Actions logs; assign tightly restricted signing ownership, an offline recovery copy and a key-loss procedure. Do **not** publish signing keystores, passwords, private keys, their raw bytes or a reconstructable combination of secrets.
2. Pin the approved public release certificate digest, package ID and version-code policy in a reviewed, non-secret manifest, then validate them against each produced candidate artifact. Debug certificates and CI-generated unsigned/debug artifacts must be clearly segregated.
3. Choose and test a migration path for the current debug-signed package. **A differently signed build cannot be installed as an in-place Android update.** Plan around platform constraints; do not silently uninstall the development build or claim that Android signing lineage bridges unrelated random debug keys.
4. Validate recoverability and data handling before any removal: enumerate the small on-device private preference set, test an explicit user-controlled data transfer or a clean-install flow, and preserve the stock Home activity as a fallback.
5. Verify a protected release pipeline with reproducible source provenance, dependency/license inventory, static and package privacy checks, matching signer digest, SHA-256 artifact identity, and isolated Android/Google TV emulator coverage. Store each standalone deliverable `.apk` in `GoreeCloud/Artifacts` and independently read it back.
6. On an idle, authorized physical TV with a named rollback owner and observed backup, verify navigation, apps, original wallpapers, Home and stock recovery, startup, 720p/1080p/4K behavior, scaling, TalkBack, contrast, and foreground/background lifecycle. Obtain founder visual acceptance before a Stable designation.

The [attended physical-device acceptance checklist](PHYSICAL_ACCEPTANCE.md) defines the open remote, display, accessibility, OEM Home, and user signoff gates. It does not authorize an install or a launcher-role change.

## Guardrails

- There is **no automatic installation** in the public workflows or the preflight script.
- Any update needing uninstall, data deletion, role reassignment, stock Home disablement, or unreviewed certificates is blocked pending explicit authorization and a verified recovery path.
- A passing read-only signer check is only one release gate; it is never permission to change the user's active TV session.

### Native 4K test limitation discovered November 7, 2026

The hosted Google TV API 34 emulator (physical framebuffer 1920×1080) **ignored** attempts to set `wm size 3840x2160`: a readback of `wm size` still reported only `Physical size: 1920x1080` with no logical override, and the PNG screenshot was 1920×1080. The UI remained focusable, but **no 4K layout or screenshot validation occurred**. Failed [first](https://github.com/GoreeCloud/tv-launcher/actions/runs/42411097450) and [second](https://github.com/GoreeCloud/tv-launcher/actions/runs/42411529061) exploratory CI runs are preserved as evidence; the ineffective 4K test was removed from the required smoke workflow instead of inventing coverage. The standard 1080p and 720p emulator tests still run, and native/physical 4K QA remains a mandatory release gate.
