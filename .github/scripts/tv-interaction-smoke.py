#!/usr/bin/env python3
"""Interactive acceptance in a disposable Google TV emulator, NEVER a real device.

A separate, permission-free fixture presents twelve local synthetic TV launchers.
No private TV, USB phone or production application is modified.
"""
from __future__ import annotations

import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

SERIAL = "emulator-5554"
PACKAGE = "com.goreecloud.tvlauncher"
FIXTURE = "com.goreecloud.tvlauncher.qafixture"
ROOT = Path(__file__).resolve().parents[2]
FIXTURE_APK = ROOT / "qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk"
OUT = ROOT / "tv-qa"


class SmokeFailure(Exception):
    pass


def require(ok: bool, reason: str) -> None:
    if not ok:
        raise SmokeFailure(reason)


def nodes(xml: bytes) -> list[ET.Element]:
    return ET.fromstring(xml).findall(".//node")


def by_text(entries: list[ET.Element], name: str) -> list[ET.Element]:
    return [item for item in entries if item.get("text") == name]


def center(bounds: str) -> tuple[int, int]:
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    require(match is not None, "Missing or invalid accessibility bounds")
    left, top, right, bottom = map(int, match.groups())
    require(0 <= left < right and 0 <= top < bottom, "Invalid UI node bounds")
    return (left + right) // 2, (top + bottom) // 2


def count_installed(entries: list[ET.Element]) -> int:
    matches = [re.fullmatch(r"(\d+) installed", x.get("text", ""))
               for x in entries]
    counts = [int(m.group(1)) for m in matches if m]
    require(len(counts) == 1, "Expected one unfiltered installed-app count")
    return counts[0]


def match_count(entries: list[ET.Element]) -> tuple[int, int]:
    matches = [re.fullmatch(r"(\d+) / (\d+) apps", x.get("text", ""))
               for x in entries]
    values = [(int(m.group(1)), int(m.group(2))) for m in matches if m]
    require(len(values) == 1, "Expected one filtered app count")
    return values[0]


def focused_description(entries: list[ET.Element]) -> str:
    focused = [n for n in entries if n.get("focused") == "true"]
    require(len(focused) == 1, "Expected exactly one focused UI element")
    require(focused[0].get("clickable") == "true", "Focused element is not actionable")
    return focused[0].get("content-desc", "") or focused[0].get("text", "")


def adb(*arguments: str, timeout: int = 45) -> bytes:
    home = os.getenv("ANDROID_HOME") or os.getenv("ANDROID_SDK_ROOT")
    require(bool(home), "ANDROID_HOME/ANDROID_SDK_ROOT is required")
    binary = Path(home) / "platform-tools/adb"
    require(binary.is_file(), "ADB binary not found")
    result = subprocess.run([str(binary), "-s", SERIAL, *arguments],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            timeout=timeout, check=False)
    require(result.returncode == 0,
            "Emulator test failed on command: " + arguments[0]
            + " (" + result.stderr.decode(errors="replace")[:220] + ")")
    return result.stdout


def assert_disposable_emulator() -> None:
    home = os.getenv("ANDROID_HOME") or os.getenv("ANDROID_SDK_ROOT")
    require(bool(home), "Android SDK required")
    binary = Path(home) / "platform-tools/adb"
    require(binary.is_file(), "ADB binary not found")
    devices = subprocess.run([str(binary), "devices", "-l"],
                             capture_output=True, text=True, timeout=15, check=True)
    online = [line.split()[0] for line in devices.stdout.splitlines()
              if len(line.split()) > 1 and line.split()[1] == "device"]
    require(online == [SERIAL], "Refusing to run with non-emulator or multiple ADB devices")
    qemu = adb("shell", "getprop", "ro.kernel.qemu").decode().strip()
    require(qemu == "1", "Device is not a real Android emulator")
    model = adb("shell", "getprop", "ro.product.model").decode().strip()
    require(model, "Emulator model unavailable")
    print("Verified disposable emulator identity", SERIAL, model, flush=True)


def screen(tag: str) -> list[ET.Element]:
    path = "/sdcard/gc-fixture-" + tag + ".xml"
    adb("shell", "uiautomator", "dump", path, timeout=90)
    content = adb("exec-out", "cat", path)
    entries = nodes(content)
    (OUT / ("interaction-" + tag + ".xml")).write_bytes(content)
    adb("shell", "rm", path)
    return entries


def tap(entries: list[ET.Element], text: str) -> None:
    # Android AlertDialog buttons are rendered in all caps on some TV themes.
    # Accessible text case must not be mistaken for a failed search control.
    options = [item for item in entries
               if item.get("text", "").casefold() == text.casefold()]
    require(len(options) == 1, "Expected one visible control: " + text)
    x, y = center(options[0].get("bounds", ""))
    adb("shell", "input", "tap", str(x), str(y))


def send(key: str, n: int = 1) -> None:
    for _ in range(n):
        adb("shell", "input", "keyevent", key)
        time.sleep(0.12)


def wait_for_fixture(timeout_seconds: int = 8) -> None:
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        name = adb("shell", "dumpsys", "window").decode(errors="replace")
        if "mCurrentFocus=" in name and FIXTURE in name.split("mCurrentFocus=", 1)[1].splitlines()[0]:
            return
        time.sleep(0.5)
    raise SmokeFailure("Fixture activity never became foreground")


def test() -> None:
    assert_disposable_emulator()
    require(FIXTURE_APK.is_file(), "Fixture APK not built")
    OUT.mkdir(parents=True, exist_ok=True)
    installed = adb("install", "-r", str(FIXTURE_APK), timeout=100)
    require(b"Success" in installed, "Failed to install synthetic fixture")
    require(b"package:" in adb("shell", "pm", "path", FIXTURE),
            "Synthetic QA fixture package not installed")
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
    time.sleep(3)
    initial = screen("initial")
    require(len(by_text(initial, "YOUR APPS")) == 1,
            "Canonical Your Apps heading missing/duplicated")
    require(not by_text(initial, "FAVORITES"), "Retired Favorites reappeared")
    total = count_installed(initial)
    require(total >= 12, "Fixture's twelve exported TV apps were not discovered")
    print("PASS: 12 fixture entries registered, app count =", total, flush=True)

    # Exercise actual DPAD_RIGHT on a full 12+ item horizontal row.
    # Start with launcher entry focus, do not use touch to simulate the rail.
    descriptions = []
    for index in range(min(total - 1, 20)):
        send("KEYCODE_DPAD_RIGHT")
        if index in (5, 10, 14, 19):
            desc = focused_description(screen("rail-" + str(index)))
            descriptions.append(desc)
    require(any("GoreeCloud QA" in value for value in descriptions),
            "D-pad never focused a synthetic app despite a long app dock")
    print("PASS: D-pad reaches fixture tiles in overfull dock", flush=True)

    # UI tapping is used here ONLY for deterministic search-dialog selection;
    # separate D-pad testing above verifies remote navigation. No real TV inputs.
    home = screen("before-search")
    tap(home, "Find an app")
    time.sleep(0.8)
    dialog = screen("search-dialog")
    edits = [n for n in dialog if n.get("class") == "android.widget.EditText"]
    require(len(edits) == 1, "Find an app dialog did not open its text entry")
    x, y = center(edits[0].get("bounds", ""))
    adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "text", "12")
    time.sleep(0.4)
    dialog = screen("search-typed")
    tap(dialog, "Show matches")
    time.sleep(1.0)
    filtered = screen("filtered")
    matches, base_total = match_count(filtered)
    require((matches, base_total) == (1, total),
            "Search should return only GoreeCloud QA 12")
    require(by_text(filtered, "GoreeCloud QA 12"),
            "Filtered QA 12 tile not visibly available")
    print("PASS: search returns exactly one expected fixture app", flush=True)

    desc = focused_description(filtered)
    require("GoreeCloud QA 12" in desc,
            "Search did not restore focus to sole matching app")
    send("KEYCODE_DPAD_CENTER")
    wait_for_fixture()
    print("PASS: D-pad OK launches expected synthetic TV activity", flush=True)
    send("KEYCODE_BACK")
    time.sleep(1.2)
    after_back = screen("returned")
    require(len(by_text(after_back, "YOUR APPS")) == 1,
            "Returning from activity lost the TV launcher")
    print("PASS: Back returns to GoreeCloud TV launcher", flush=True)
    tap(after_back, "Find an app")
    time.sleep(0.5)
    dialog = screen("reset-dialog")
    tap(dialog, "Show all apps")
    time.sleep(0.7)
    restored = screen("all-apps-restored")
    require(count_installed(restored) == total,
            "Show all apps did not reset filtered Your Apps")
    print("PASS: Show all apps resets filter", flush=True)

    # No-result filtering must return focus to the Find an app action, not
    # abandon the remote on an unfocusable empty-state text view.
    tap(restored, "Find an app")
    time.sleep(0.5)
    no_match_dialog = screen("no-match-dialog")
    edits = [n for n in no_match_dialog
             if n.get("class") == "android.widget.EditText"]
    require(len(edits) == 1, "No-match search input missing")
    x, y = center(edits[0].get("bounds", ""))
    adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "text", "NoSuchTvApp93712")
    no_match_dialog = screen("no-match-typed")
    tap(no_match_dialog, "Show matches")
    time.sleep(1.0)
    no_matches = screen("no-matches")
    require(match_count(no_matches) == (0, total), "No-match filter count incorrect")
    require(by_text(no_matches,
                    "No matching apps. Use Find an app to clear the filter."),
            "Search empty-state guidance missing")
    require(focused_description(no_matches) == "Find an app",
            "No-match search did not restore actionable Find an app focus")
    print("PASS: empty search state gives D-pad focus to Find an app", flush=True)
    tap(no_matches, "Find an app")
    time.sleep(0.4)
    tap(screen("empty-reset-dialog"), "Show all apps")
    time.sleep(0.5)
    require(count_installed(screen("empty-restored")) == total,
            "Failed to restore complete library from empty-state search")
    print("PASS: empty-state reset restores every installed app", flush=True)
    print("PASS: all integration actions occurred in disposable emulator only", flush=True)


if __name__ == "__main__":
    try:
        test()
    except (SmokeFailure, ET.ParseError, OSError, subprocess.TimeoutExpired) as error:
        print("FAIL: " + str(error), file=sys.stderr)
        sys.exit(1)
