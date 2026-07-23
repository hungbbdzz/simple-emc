#!/usr/bin/env python3
"""Architecture guard for the shared Minecraft gameplay source root."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED = ROOT / "common-gameplay" / "src" / "main" / "java"
FORGE = ROOT / "forge-1.20.1" / "src" / "main" / "java"
NEOFORGE = ROOT / "neoforge-1.21.1" / "src" / "main" / "java"
MANIFEST = ROOT / "common-gameplay" / "SHARED_SOURCES.txt"
FORBIDDEN_IMPORTS = ("net.minecraftforge.", "net.neoforged.")


def java_map(root: Path) -> dict[Path, Path]:
    return {path.relative_to(root): path for path in root.rglob("*.java")}


def fail(messages: list[str]) -> int:
    print("Shared source integrity check failed:", file=sys.stderr)
    for message in messages:
        print(f"  - {message}", file=sys.stderr)
    return 1


def main() -> int:
    errors: list[str] = []
    shared = java_map(SHARED)
    forge = java_map(FORGE)
    neoforge = java_map(NEOFORGE)

    if not shared:
        errors.append("common-gameplay contains no Java sources")

    duplicate_forge = sorted(set(shared) & set(forge))
    duplicate_neoforge = sorted(set(shared) & set(neoforge))
    for relative in duplicate_forge:
        errors.append(f"duplicate shared/Forge class: {relative}")
    for relative in duplicate_neoforge:
        errors.append(f"duplicate shared/NeoForge class: {relative}")

    for relative, path in sorted(shared.items()):
        text = path.read_text(encoding="utf-8")
        for forbidden in FORBIDDEN_IMPORTS:
            if forbidden in text:
                errors.append(f"loader API import in shared source {relative}: {forbidden}")
        expected_package = ".".join(relative.parent.parts)
        if expected_package and f"package {expected_package};" not in text:
            errors.append(f"package/path mismatch in shared source: {relative}")

    if MANIFEST.exists():
        manifest_entries = {
            Path(line.strip())
            for line in MANIFEST.read_text(encoding="utf-8").splitlines()
            if line.strip()
        }
        missing = sorted(manifest_entries - set(shared))
        unlisted = sorted(set(shared) - manifest_entries)
        for relative in missing:
            errors.append(f"manifest entry missing from shared source: {relative}")
        for relative in unlisted:
            errors.append(f"shared source not listed in manifest: {relative}")
    else:
        errors.append("common-gameplay/SHARED_SOURCES.txt is missing")

    if errors:
        return fail(errors)

    print(
        "Shared source integrity OK: "
        f"{len(shared)} shared classes, "
        f"{len(forge)} Forge-only classes, "
        f"{len(neoforge)} NeoForge-only classes."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
