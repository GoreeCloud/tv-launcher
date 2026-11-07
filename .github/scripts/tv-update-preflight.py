#!/usr/bin/env python3
"""Read-only, fail-closed preflight for an installed GoreeCloud TV debug APK.

No ADB command in this module installs, uninstalls, changes settings, starts an
activity, or sends input. In particular, a certificate mismatch must never be
"fixed" by uninstalling the existing launcher.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

PACKAGE = "com.goreecloud.tvlauncher"
ANDROID_BUILDTOOLS = "35.0.0"


class PreflightBlocked(Exception):
    """Safety gate rejects a potentially destructive or unverified update."""


def require(ok: bool, reason: str) -> None:
    if not ok:
        raise PreflightBlocked(reason)


def package_identity(badging: str) -> tuple[str, int, str]:
    # aapt emits a single package: line; never accept a matching substring.
    found = re.findall(
        r"(?m)^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",
        badging,
    )
    require(len(found) == 1, "APK package name/version could not be verified")
    name, code, version = found[0]
    return name, int(code), version


def installed_version(dumpsys: str) -> int:
    codes = re.findall(r"(?m)^\s+versionCode=(\d+)(?:\s|$)", dumpsys)
    require(len(codes) == 1, "Could not unambiguously read installed versionCode")
    return int(codes[0])


def signing_digests(apksigner_output: str) -> tuple[str, ...]:
    digests = re.findall(
        r"(?m)^Signer #[0-9]+ certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$",
        apksigner_output,
    )
    require(bool(digests), "APK signing certificate was not reported")
    require(len(digests) == len(set(digests)), "Duplicate certificate digest detected")
    return tuple(sorted(s.lower() for s in digests))


def installed_base_path(pm_path: str) -> str:
    paths = [line.removeprefix("package:").strip()
             for line in pm_path.splitlines() if line.startswith("package:")]
    bases = [path for path in paths if path.endswith("/base.apk")]
    require(len(bases) == 1 and bases[0].startswith("/data/app/"),
            "Installed base APK path missing or ambiguous")
    return bases[0]


def online_serial(devices: str, serial: str) -> None:
    matching = [line for line in devices.splitlines()
                if line.split() and line.split()[0] == serial]
    require(len(matching) == 1 and len(matching[0].split()) >= 2
            and matching[0].split()[1] == "device",
            "Requested ADB serial is not uniquely authorized/online")


def run(*args: str) -> str:
    result = subprocess.run(args, text=True, capture_output=True, check=False,
                            timeout=45)
    require(result.returncode == 0,
            "Read-only inspection command failed: " + Path(args[0]).name
            + " " + str(args[1] if len(args) > 1 else ""))
    return result.stdout.replace("\r", "")


def verify(args: argparse.Namespace) -> None:
    apk = args.apk.resolve(strict=True)
    require(apk.is_file() and apk.suffix.lower() == ".apk", "Use a regular .apk file")
    require(bool(args.serial.strip()) and bool(args.expected_model.strip()),
            "Explicit device serial and expected model are required")
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or "")
    adb = sdk / "platform-tools/adb"
    aapt = sdk / ("build-tools/" + ANDROID_BUILDTOOLS) / "aapt"
    apksigner = sdk / ("build-tools/" + ANDROID_BUILDTOOLS) / "apksigner"
    require(adb.is_file() and aapt.is_file() and apksigner.is_file(),
            "SDK platform-tools and build-tools 35.0.0 are required")

    online_serial(run(str(adb), "devices", "-l"), args.serial)
    def tv(*commands: str) -> str:
        return run(str(adb), "-s", args.serial, "shell", *commands)

    model = tv("getprop", "ro.product.model").strip()
    require(model == args.expected_model, "TV model differs from the explicit target")
    badging = run(str(aapt), "dump", "badging", str(apk))
    pkg, incoming_code, version = package_identity(badging)
    require(pkg == PACKAGE, "Incoming APK is not the GoreeCloud TV launcher")
    requested = run(str(aapt), "dump", "permissions", str(apk))
    require(not re.search(r"(?m)^uses-permission", requested),
            "Incoming APK unexpectedly requests Android permissions")
    incoming_signer = signing_digests(run(str(apksigner), "verify",
                                         "--print-certs", str(apk)))

    installed_paths = tv("pm", "path", PACKAGE)
    base = installed_base_path(installed_paths)
    current_code = installed_version(tv("dumpsys", "package", PACKAGE))
    require(incoming_code >= current_code,
            "Downgrade blocked; backup, migration and rollback are not approved")
    # The temp directory is private, automatically removed, and never uploaded.
    with tempfile.TemporaryDirectory(prefix="goreecloud-tv-apk-") as directory:
        pulled = Path(directory) / "installed-base.apk"
        run(str(adb), "-s", args.serial, "pull", base, str(pulled))
        require(pulled.is_file() and pulled.stat().st_size > 0,
                "Installed APK could not be read")
        installed_signer = signing_digests(
            run(str(apksigner), "verify", "--print-certs", str(pulled)))
    require(incoming_signer == installed_signer,
            "Signing certificate differs from installed TV build. Do NOT uninstall "
            "or replace it with this APK.")

    print("PASS: Read-only package, device, zero-permissions, version and signer checks")
    print("  Package:", pkg, "incoming", version, f"({incoming_code})",
          "installed versionCode", current_code)
    if incoming_code == current_code:
        print("  Note: incoming APK has the same versionCode as the installed build")
    print("  A matching signer is NECESSARY but not sufficient for an upgrade.")
    print("  No installation, data backup, Home-role change or device input was performed.")
    print("  Separately verify backups, free TV session, rollback and UI acceptance.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Exact ADB device serial")
    parser.add_argument("--expected-model", required=True, help="Exact ro.product.model")
    parser.add_argument("--apk", type=Path, required=True, help="Local APK to inspect")
    args = parser.parse_args()
    try:
        verify(args)
        return 0
    except (PreflightBlocked, OSError, subprocess.TimeoutExpired) as error:
        print("BLOCK: " + str(error), file=sys.stderr)
        print("No installation was attempted.", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
