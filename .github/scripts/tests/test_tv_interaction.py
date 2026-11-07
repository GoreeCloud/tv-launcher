"""Offline unit tests for the disposable emulator-only TV interaction harness."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / "tv-interaction-smoke.py"
spec = importlib.util.spec_from_file_location("tv_interaction_smoke", SCRIPT)
script = importlib.util.module_from_spec(spec)
spec.loader.exec_module(script)


def node(text: str, bounds: str = "[0,0][200,50]", **kwargs) -> ET.Element:
    result = ET.Element("node", {"text": text, "bounds": bounds})
    result.attrib.update(kwargs)
    return result


class Parsing(unittest.TestCase):
    def test_bounds_center(self):
        self.assertEqual((151, 55), script.center("[100,20][202,90]"))

    def test_rejects_bad_bounds(self):
        for value in ("", "[0,0][0,1]", "garbage", "[-1,0][1,1]"):
            with self.subTest(bounds=value), self.assertRaises(script.SmokeFailure):
                script.center(value)

    def test_single_installed_count(self):
        self.assertEqual(17, script.count_installed(
            [node("YOUR APPS"), node("17 installed"), node("Find an app")]))

    def test_duplicate_count_blocked(self):
        with self.assertRaises(script.SmokeFailure):
            script.count_installed([node("17 installed"), node("16 installed")])

    def test_filtered_count(self):
        self.assertEqual((1, 17), script.match_count([node("1 / 17 apps")]))
        with self.assertRaises(script.SmokeFailure):
            script.match_count([node("17 installed")])

    def test_exact_name(self):
        items = [node("Find an app"), node("12 installed"), node("YOUR APPS")]
        self.assertEqual([items[2]], script.by_text(items, "YOUR APPS"))

    def test_alert_dialog_buttons_match_case_insensitively(self):
        dialog = [node("SHOW MATCHES", "[1152,575][1350,656]")]
        with patch.object(script, "adb", return_value=b""):
            script.tap(dialog, "Show matches")
            script.adb.assert_called_once_with(
                "shell", "input", "tap", "1251", "615")

    def test_focused_actionable(self):
        view = node("GoreeCloud QA 12", focused="true", clickable="true",
                    **{"content-desc": "GoreeCloud QA 12. Press OK to open"})
        self.assertIn("QA 12", script.focused_description([view]))

    def test_unfocused_or_inert_blocked(self):
        with self.assertRaises(script.SmokeFailure):
            script.focused_description([node("QA")])
        with self.assertRaises(script.SmokeFailure):
            script.focused_description([node("QA", focused="true", clickable="false")])

    def test_emulator_guard_rejects_non_emulator_device(self):
        # Guarantee a real user's TV or phone cannot accidentally run this test.
        with patch.object(script, "adb", return_value=b"0\n"), patch.object(
                script.os, "getenv", return_value="/sdk"), patch.object(
                script.Path, "is_file", return_value=True), patch.object(
                script.subprocess, "run") as run:
            run.return_value.stdout = "emulator-5554 device\n192.168.4.22:5555 device\n"
            with self.assertRaisesRegex(script.SmokeFailure, "multiple ADB devices"):
                script.assert_disposable_emulator()
            script.adb.assert_not_called()

    def test_shell_smoke_guards_device_before_first_mutation(self):
        shell = (Path(__file__).resolve().parents[1] / "tv-smoke.sh").read_text()
        a = shell.index("preconnected=$(adb devices")
        b = shell.index("timeout 360 adb -s emulator-5554 wait-for-device")
        c = shell.index("export ANDROID_SERIAL=emulator-5554")
        first_mutation = min(shell.index("adb shell input keyevent"),
                             shell.index("adb install -r"),
                             shell.index("adb shell wm size"))
        self.assertLess(a, b)
        self.assertLess(b, c)
        self.assertLess(c, first_mutation)
        self.assertIn('ro.kernel.qemu', shell[:first_mutation])

    def test_emulator_guard_checks_kernel_flag(self):
        with patch.object(script, "adb", return_value=b"0\n"), patch.object(
                script.os, "getenv", return_value="/sdk"), patch.object(
                script.Path, "is_file", return_value=True), patch.object(
                script.subprocess, "run") as run:
            run.return_value.stdout = "emulator-5554 device\n"
            with self.assertRaisesRegex(script.SmokeFailure, "not a real Android emulator"):
                script.assert_disposable_emulator()


if __name__ == "__main__":
    unittest.main()
