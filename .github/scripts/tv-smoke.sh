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
# Second resolution in the same isolated emulator (no physical TV settings changed).
adb shell wm size 1280x720
adb shell wm density 240
adb shell am force-stop com.goreecloud.tvlauncher
adb shell am start -n com.goreecloud.tvlauncher/.MainActivity
sleep 4
adb shell input keyevent KEYCODE_DPAD_RIGHT
adb shell uiautomator dump /sdcard/window-goreecloud-720.xml
adb pull /sdcard/window-goreecloud-720.xml "$OUT_DIR/window-720.xml" >/dev/null
python3 - "$OUT_DIR/window-720.xml" <<'FOCUS_720'
import sys
import xml.etree.ElementTree as ET
nodes = ET.parse(sys.argv[1]).getroot().findall('.//node')
assert any(n.get('package') == 'com.goreecloud.tvlauncher' for n in nodes), 'App not shown at 720p'
assert any(n.get('focusable') == 'true' and n.get('clickable') == 'true' for n in nodes), 'No actionable TV controls at 720p'
assert any(n.get('focused') == 'true' and n.get('clickable') == 'true' for n in nodes), 'Remote focus lost at 720p'
print('PASS: 720p UI hierarchy and actionable D-pad focus')
FOCUS_720
adb exec-out screencap -p > "$OUT_DIR/home-720p.png"
python3 - "$OUT_DIR/home.png" "$OUT_DIR/home-720p.png" <<'VERIFY_PNG'
import struct
import sys
from pathlib import Path
for path, expected in zip(sys.argv[1:], [(1920, 1080), (1280, 720)]):
    raw = Path(path).read_bytes()
    assert raw[:8] == b'\x89PNG\r\n\x1a\n', 'Not a PNG screenshot'
    size = struct.unpack('>II', raw[16:24])
    assert size == expected, f'Unexpected capture size {size} for {path}'
    print('Verified emulator screenshot', path, size)
VERIFY_PNG
# Isolated UHD/4K layout smoke. This overrides only the disposable emulator.
# It does NOT change the physical owner's display mode or stored settings.
adb shell wm size 3840x2160
adb shell wm density 320
adb shell am force-stop com.goreecloud.tvlauncher
adb shell am start -n com.goreecloud.tvlauncher/.MainActivity
sleep 5
adb shell input keyevent KEYCODE_DPAD_RIGHT
adb shell uiautomator dump /sdcard/window-goreecloud-4k.xml
adb pull /sdcard/window-goreecloud-4k.xml "$OUT_DIR/window-4k.xml" >/dev/null
python3 - "$OUT_DIR/window-4k.xml" <<'FOCUS_4K'
import sys
import xml.etree.ElementTree as ET
nodes = ET.parse(sys.argv[1]).getroot().findall('.//node')
labels = [node.get('text') for node in nodes]
assert labels.count('YOUR APPS') == 1, 'Missing or duplicated UHD Your Apps heading'
assert 'FAVORITES' not in labels and 'QUICK LAUNCH' not in labels, (
    'A retired Favorites/Quick Launch heading returned at UHD'
)
assert any(n.get('focused') == 'true' and n.get('clickable') == 'true'
           for n in nodes), 'Actionable D-pad focus lost at UHD'
print('PASS: 4K Your Apps identity and focused actionable control')
FOCUS_4K
# The Google TV emulator's hardware framebuffer may remain 1920x1080
# even when Android accepts a 3840x2160 *logical* wm override.
# Record both dimensions; never mislabel a compositor-limited screenshot
# as proof of native 4K pixels or physical display certification.
adb shell wm size > "$OUT_DIR/requested-4k-mode.txt"
grep -Fq 'Override size: 3840x2160' "$OUT_DIR/requested-4k-mode.txt"
adb exec-out screencap -p > "$OUT_DIR/home-4k-request.png"
python3 - "$OUT_DIR/home-4k-request.png" "$OUT_DIR/4k-capture-limits.txt" <<'VERIFY_UHD'
import struct
import sys
from pathlib import Path
data = Path(sys.argv[1]).read_bytes()
assert data[:8] == b'\x89PNG\r\n\x1a\n', 'Invalid 4K-request screenshot'
width, height = struct.unpack('>II', data[16:24])
assert (width, height) in ((3840, 2160), (1920, 1080)), (
    f'Unexpected screenshot dimensions {width}x{height}'
)
if (width, height) == (3840, 2160):
    report = 'Native-resolution screenshot: 3840x2160; physical TV acceptance remains open'
else:
    report = ('LIMITATION: 3840x2160 logical window override, but Google TV emulator '
              'compositor captured only 1920x1080; native 4K screenshot NOT verified')
Path(sys.argv[2]).write_text(report + '\n', encoding='utf-8')
print(report)
VERIFY_UHD
adb shell wm size 1920x1080
adb shell wm density 240
adb logcat -d -t 600 -v brief > "$OUT_DIR/logcat-tail.txt"
echo "PASS: APK installed, TV intent filters registered, activity visible, D-pad events accepted, UI hierarchy confirmed, process alive."
