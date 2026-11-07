#!/usr/bin/env bash
set -euo pipefail

# Isolated emulator only. Never disable, uninstall, or reassign stock Home packages.
OUT_DIR=tv-qa
mkdir -p "$OUT_DIR"
: "${ANDROID_HOME:?ANDROID_HOME must be set}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

timeout 360 adb wait-for-device
for attempt in $(seq 1 180); do
  booted=$(adb shell getprop sys.boot_completed | tr -d '\r')
  if [ "$booted" = "1" ]; then break; fi
  sleep 2
done
test "${booted:-}" = "1" || { echo "Emulator did not finish booting"; exit 1; }
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm size 1920x1080
adb shell wm density 240
adb shell wm size > "$OUT_DIR/screen-size.txt"
adb shell getprop ro.build.version.sdk > "$OUT_DIR/api-level.txt"
adb shell getprop ro.product.model > "$OUT_DIR/device-model.txt"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm list packages | grep -Fx 'package:com.goreecloud.tvlauncher'
adb shell dumpsys package com.goreecloud.tvlauncher > "$OUT_DIR/package.txt"
grep -F 'android.intent.category.HOME' "$OUT_DIR/package.txt"
grep -F 'android.intent.category.LEANBACK_LAUNCHER' "$OUT_DIR/package.txt"
adb shell am start -n com.goreecloud.tvlauncher/.MainActivity
sleep 5
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_RIGHT
adb shell input keyevent KEYCODE_DPAD_LEFT
adb shell uiautomator dump /sdcard/window-goreecloud.xml
adb pull /sdcard/window-goreecloud.xml "$OUT_DIR/window.xml" >/dev/null
grep -Fq 'GoreeCloud TV' "$OUT_DIR/window.xml"
grep -Fq 'HORIZON EXPEDITION' "$OUT_DIR/window.xml"
grep -Fq 'YOUR APPS' "$OUT_DIR/window.xml"
python3 - "$OUT_DIR/window.xml" <<'SINGLE_APP_SECTION'
import re
import sys
import xml.etree.ElementTree as ET

nodes = ET.parse(sys.argv[1]).getroot().findall('.//node')
named = lambda name: [n for n in nodes if n.get('text') == name]
assert len(named('YOUR APPS')) == 1, 'Expected exactly one Your Apps header'
assert not named('FAVORITES'), 'Obsolete Favorites section returned'
assert not named('QUICK LAUNCH'), 'Obsolete Quick Launch header returned'
continued = named('CONTINUE IN YOUR APPS')
if continued:
    def top(n):
        match = re.match(r'\[(\d+),(\d+)\]', n.get('bounds', ''))
        return int(match.group(2)) if match else -1
    assert top(named('YOUR APPS')[0]) < top(continued[0]), (
        'Your Apps must be above the recent activity section'
    )
print('PASS: single top Your Apps section, no separate Favorites')
SINGLE_APP_SECTION
python3 - "$OUT_DIR/window.xml" <<'FOCUS_CHECK'
import sys
import xml.etree.ElementTree as ET
nodes = ET.parse(sys.argv[1]).getroot().findall('.//node')
focused = [node for node in nodes if node.attrib.get('focused') == 'true']
if not any(node.attrib.get('clickable') == 'true' for node in focused):
    raise SystemExit('D-pad has no visible, actionable focused control')
print('PASS: D-pad focus on actionable TV control')
FOCUS_CHECK
adb exec-out screencap -p > "$OUT_DIR/home.png"
adb shell pidof com.goreecloud.tvlauncher > "$OUT_DIR/pid.txt"
test -s "$OUT_DIR/pid.txt"
adb logcat -d -t 600 -v brief > "$OUT_DIR/logcat-tail.txt"
echo "PASS: APK installed, TV intent filters registered, activity visible, D-pad events accepted, UI hierarchy confirmed, process alive."
