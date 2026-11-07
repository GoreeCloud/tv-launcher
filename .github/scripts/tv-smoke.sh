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
grep -Eq 'GOREECLOUD TV|Make yourself at home' "$OUT_DIR/window.xml"
adb exec-out screencap -p > "$OUT_DIR/home.png"
adb shell pidof com.goreecloud.tvlauncher > "$OUT_DIR/pid.txt"
test -s "$OUT_DIR/pid.txt"
adb logcat -d -t 600 -v brief > "$OUT_DIR/logcat-tail.txt"
echo "PASS: APK installed, TV intent filters registered, activity visible, D-pad events accepted, UI hierarchy confirmed, process alive."
