#!/usr/bin/env python3
"""Fail CI if bundled GoreeCloud TV identity diverges from pinned official assets."""
from pathlib import Path
import hashlib
import json
root=Path(__file__).resolve().parents[2]
pin=json.loads((root/"docs/BRAND_ASSET_PIN.json").read_text())
assert pin["repository"]=="GoreeCloud/brand-assets"
assert len(pin["commit"])==40
assert len(pin["files"])==7
for filename,expected in pin["files"].items():
    path=root/filename
    if not path.is_file():
        raise SystemExit(f"Missing official art: {filename}")
    actual=hashlib.sha256(path.read_bytes()).hexdigest()
    if actual!=expected:
        raise SystemExit(f"Official artwork checksum mismatch: {filename}")
print("Verified 4 offline wallpapers, TV icon, wordmark and TV banner against pinned GoreeCloud brand-assets commit",pin["commit"][:8])
