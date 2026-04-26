#!/usr/bin/env python3
"""One-off: move com/aipick -> com/sparklink and replace com.aipick with com.sparklink in sources."""
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)

MOVES = [
    ("src/main/java/com/aipick", "src/main/java/com/sparklink"),
    ("src/test/java/com/aipick", "src/test/java/com/sparklink"),
]

for src, dst in MOVES:
    if os.path.isdir(src):
        if os.path.exists(dst):
            raise SystemExit(f"Refuse to overwrite existing: {dst}")
        shutil.move(src, dst)
        print(f"OK: {src} -> {dst}")
    else:
        print(f"Skip (no dir): {src}")

for root, _, files in os.walk("src"):
    for name in files:
        if not name.endswith((".java", ".xml")):
            continue
        path = os.path.join(root, name)
        with open(path, encoding="utf-8") as f:
            text = f.read()
        if "com.aipick" not in text:
            continue
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text.replace("com.aipick", "com.sparklink"))
        print(f"Updated: {path}")

mapper = "src/main/resources/mapper/PartnerMapper.xml"
if os.path.isfile(mapper):
    with open(mapper, encoding="utf-8") as f:
        text = f.read()
    if "com.aipick" in text:
        with open(mapper, "w", encoding="utf-8", newline="\n") as f:
            f.write(text.replace("com.aipick", "com.sparklink"))
        print(f"Updated: {mapper}")

print("Done.")
