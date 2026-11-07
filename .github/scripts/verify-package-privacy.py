#!/usr/bin/env python3
"""Verify the Android TV APK actually packages first-party art and least privilege.

Uses only stdlib and the Android SDK build-tools binaries already required to
build the project. This is a build gate, not a malware scan or Stable certification.
"""
from __future__ import annotations

import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
SDK = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
ANDROID = "{http://schemas.android.com/apk/res/android}"
EXPECTED_PACKAGE = "com.goreecloud.tvlauncher"
EXPECTED_DRAWABLES = {
    "tv_banner.png",
    "tv_wordmark.png",
    "wp_horizon.webp",
    "wp_aperture.webp",
    "wp_cloudforge.webp",
    "wp_harbor.webp",
}


def run(*args: str) -> str:
    completed = subprocess.run(
        args, check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )
    if completed.stderr and "WARNING:" not in completed.stderr:
        print(completed.stderr, file=sys.stderr)
    return completed.stdout


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


def main() -> None:
    require(SDK is not None, "ANDROID_HOME or ANDROID_SDK_ROOT must be set")
    build = Path(SDK) / "build-tools/35.0.0"
    aapt = str(build / "aapt")
    apksigner = str(build / "apksigner")
    require(APK.is_file(), f"APK missing: {APK}")
    require(Path(aapt).is_file(), "Android SDK build-tools 35.0.0 missing")

    badging = run(aapt, "dump", "badging", str(APK))
    permissions = run(aapt, "dump", "permissions", str(APK))
    require(f"package: name='{EXPECTED_PACKAGE}'" in badging, "unexpected APK package")
    require("leanback-launchable-activity:" in badging, "TV launcher activity not packaged")
    require("MainActivity" in badging, "MainActivity missing")
    require("tv_banner.png" in badging, "branded TV banner not packaged")
    require("ic_launcher.png" in badging, "branded app icon not packaged")
    require(not re.search(r"(?m)^uses-permission", permissions),
            "APK must request no Android permissions")
    require("0.2.0-alpha" in badging, "not an alpha-preview build")
    run(apksigner, "verify", str(APK))

    root = ET.parse(MANIFEST).getroot()
    require(root.tag == "manifest", "invalid Android manifest")
    require(not root.findall("uses-permission"), "manifest adds permissions")
    require(not root.findall("uses-permission-sdk-23"), "manifest adds SDK23 permissions")
    require(root.find("queries") is not None, "scoped app discovery missing")
    app = root.find("application")
    require(app is not None, "application missing")
    require(app.get(ANDROID + "allowBackup") == "false", "backup must remain disabled")
    require(app.get(ANDROID + "usesCleartextTraffic") == "false",
            "cleartext traffic must remain disabled")
    require(app.get(ANDROID + "icon") == "@mipmap/ic_launcher",
            "application must reference official icon")
    require(app.get(ANDROID + "banner") == "@drawable/tv_banner",
            "application must reference official banner")
    activity = app.find("activity")
    require(activity is not None and activity.get(ANDROID + "exported") == "true",
            "Home activity must be exported")
    cats = {
        child.get(ANDROID + "name")
        for filt in activity.findall("intent-filter")
        for child in filt.findall("category")
    }
    require(
        {"android.intent.category.HOME", "android.intent.category.LEANBACK_LAUNCHER"}
        <= cats,
        "HOME and LEANBACK_LAUNCHER intent categories must both be declared",
    )

    with zipfile.ZipFile(APK) as archive:
        names = archive.namelist()
        require(
            not any(p.startswith("/") or ".." in Path(p).parts for p in names),
            "unsafe archive entry name",
        )
        for asset in EXPECTED_DRAWABLES:
            require(
                sum(p.startswith("res/") and p.endswith("/" + asset) for p in names) == 1,
                f"first-party packaged artwork missing or duplicated: {asset}",
            )
        require(
            sum(p.startswith("res/") and p.endswith("/ic_launcher.png") for p in names) == 1,
            "official launcher icon missing/duplicated",
        )
    print("PASS: valid signed APK, original TV icon/banner, 4 wallpapers and wordmark,")
    print("      HOME/LEANBACK intent filters, zero Android permissions, backups and cleartext off.")


if __name__ == "__main__":
    main()
