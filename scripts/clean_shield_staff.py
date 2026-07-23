import os

files = [
    r"forge-1.20.1/src/main/resources/data/velorise_emc/recipes/shield_staff.json",
    r"neoforge-1.21.1/src/main/resources/data/velorise_emc/recipe/shield_staff.json"
]

for p in files:
    if os.path.exists(p):
        os.remove(p)
        print(f"Deleted retired empty recipe file: {p}")
