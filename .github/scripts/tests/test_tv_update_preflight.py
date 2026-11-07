"""Offline fail-closed TV preflight tests. All ADB/SDK commands are mocked."""
import argparse
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

source = Path(__file__).resolve().parents[1] / "tv-update-preflight.py"
spec = importlib.util.spec_from_file_location("goreecloud_preflight", source)
preflight = importlib.util.module_from_spec(spec)
spec.loader.exec_module(preflight)
A, B = "a" * 64, "b" * 64
SERIAL = "mock-tv:12345"
MODEL = "onn. 4K Streaming Box"


class ParserTests(unittest.TestCase):
    def test_package_identity(self):
        self.assertEqual(("com.goreecloud.tvlauncher", 8, "alpha.4"),
                         preflight.package_identity(
                             "package: name='com.goreecloud.tvlauncher' "
                             "versionCode='8' versionName='alpha.4'\n"))
        with self.assertRaises(preflight.PreflightBlocked):
            preflight.package_identity("invalid")
    def test_installed_version(self):
        self.assertEqual(8, preflight.installed_version("   versionCode=8 minSdk=23"))
        with self.assertRaises(preflight.PreflightBlocked):
            preflight.installed_version("versionCode=8\nversionCode=9")
    def test_certificate(self):
        self.assertEqual((A,), preflight.signing_digests(
            "Signer #1 certificate SHA-256 digest: " + A + "\n"))
        with self.assertRaises(preflight.PreflightBlocked):
            preflight.signing_digests("Unsigned")
    def test_base_apk(self):
        self.assertEqual("/data/app/abc/base.apk", preflight.installed_base_path(
            "package:/data/app/abc/base.apk\npackage:/data/app/abc/split.apk"))
        with self.assertRaises(preflight.PreflightBlocked):
            preflight.installed_base_path("package:/sdcard/base.apk")
    def test_online_serial(self):
        preflight.online_serial(SERIAL + " device\nother offline", SERIAL)
        with self.assertRaises(preflight.PreflightBlocked):
            preflight.online_serial(SERIAL + " offline", SERIAL)


class IntegrationMocks(unittest.TestCase):
    def setUp(self):
        self.t = tempfile.TemporaryDirectory()
        self.addCleanup(self.t.cleanup)
        root = Path(self.t.name)
        self.apk = root / "incoming.apk"
        self.apk.write_bytes(b"mock apk; sdk commands are not executed")
        self.sdk = root / "sdk"
        for f in ("platform-tools/adb", "build-tools/35.0.0/aapt",
                  "build-tools/35.0.0/apksigner"):
            p = self.sdk / f
            p.parent.mkdir(parents=True, exist_ok=True)
            p.touch()
        self.args = argparse.Namespace(apk=self.apk, serial=SERIAL, expected_model=MODEL)
        self.calls = []
        self.installed_cert = A
        self.incoming_cert = A
        self.installed_version = 8
        self.package = preflight.PACKAGE
        self.permissions = ""

    def run_mock(self, *args):
        command = " ".join(args)
        self.calls.append(command)
        if "devices -l" in command: return SERIAL + " device\n"
        if "getprop ro.product.model" in command: return MODEL + "\n"
        if "dump badging" in command:
            return f"package: name='{self.package}' versionCode='8' versionName='alpha.4'\n"
        if "dump permissions" in command: return self.permissions
        if "verify --print-certs" in command:
            cert = self.installed_cert if "installed-base.apk" in command else self.incoming_cert
            return "Signer #1 certificate SHA-256 digest: " + cert + "\n"
        if "pm path" in command: return "package:/data/app/abc/base.apk\n"
        if "dumpsys package" in command:
            return f"    versionCode={self.installed_version} minSdk=23\n"
        if " pull " in command:
            Path(args[-1]).write_bytes(b"mock installed APK")
            return "1 file pulled\n"
        raise AssertionError("Unexpected command " + command)

    def run_check(self):
        with patch.dict(os.environ, {"ANDROID_HOME": str(self.sdk)}), patch.object(
                preflight, "run", side_effect=self.run_mock):
            return preflight.verify(self.args)

    def assert_read_only(self):
        for cmd in self.calls:
            for word in (" install ", " uninstall ", " input ", " settings put ",
                         " pm clear ", " disable-user "):
                self.assertNotIn(word, cmd)

    def test_same_signer_passes_without_mutation(self):
        self.run_check()
        self.assert_read_only()

    def test_signer_mismatch_blocks(self):
        self.installed_cert = B
        with self.assertRaisesRegex(preflight.PreflightBlocked, "Signing certificate differs"):
            self.run_check()
        self.assert_read_only()

    def test_downgrade_blocks_before_pull(self):
        self.installed_version = 9
        with self.assertRaisesRegex(preflight.PreflightBlocked, "Downgrade blocked"):
            self.run_check()
        self.assertFalse(any(" pull " in cmd for cmd in self.calls))

    def test_wrong_package_blocks(self):
        self.package = "com.example.notgoree"
        with self.assertRaisesRegex(preflight.PreflightBlocked, "not the GoreeCloud"):
            self.run_check()
        self.assert_read_only()

    def test_permissions_block(self):
        self.permissions = "uses-permission: name='android.permission.INTERNET'\n"
        with self.assertRaisesRegex(preflight.PreflightBlocked, "requests Android permissions"):
            self.run_check()
        self.assert_read_only()


if __name__ == "__main__":
    unittest.main()
