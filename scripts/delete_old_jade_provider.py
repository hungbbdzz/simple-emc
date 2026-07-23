import os
from pathlib import Path

ROOT = Path(r"c:\Users\hunga\Documents\Coder\Minecraft\myMod\simple_emc")
files = [
    ROOT / "forge-1.20.1/src/main/java/com/velorise/emc/compat/jade/ResonantFurnaceJadeProvider.java",
    ROOT / "neoforge-1.21.1/src/main/java/com/velorise/emc/compat/jade/ResonantFurnaceJadeProvider.java"
]

for p in files:
    if p.exists():
        p.unlink()
        print(f"Deleted old provider: {p}")
