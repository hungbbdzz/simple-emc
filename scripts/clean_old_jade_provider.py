import os

files = [
    r"forge-1.20.1/src/main/java/com/velorise/emc/compat/jade/ResonantFurnaceJadeProvider.java",
    r"neoforge-1.21.1/src/main/java/com/velorise/emc/compat/jade/ResonantFurnaceJadeProvider.java"
]

for p in files:
    if os.path.exists(p):
        os.remove(p)
        print(f"Deleted old Jade provider: {p}")
