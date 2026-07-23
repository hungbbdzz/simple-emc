import json
import os

shared_models = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "shared-resources", "assets", "velorise_emc", "models", "block")

def build_core_socket_all(tier):
    tex = f"velorise_emc:block/core_socket_t{tier}"
    elements = []
    
    # 4 Top Face Corners (Y = 15.98..16.02)
    corners_top = [
        {"from": [0, 15.98, 0], "to": [3, 16.02, 3]},
        {"from": [13, 15.98, 0], "to": [16, 16.02, 3]},
        {"from": [0, 15.98, 13], "to": [3, 16.02, 16]},
        {"from": [13, 15.98, 13], "to": [16, 16.02, 16]}
    ]
    for c in corners_top:
        elements.append({
            "from": c["from"],
            "to": c["to"],
            "faces": {
                "up": {"uv": [0, 0, 16, 16], "texture": "#socket"}
            }
        })
        
    # Side Top Corners
    # North face (Z = -0.02..0.02)
    elements.append({"from": [0, 13, -0.02], "to": [3, 16, 0.02], "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    elements.append({"from": [13, 13, -0.02], "to": [16, 16, 0.02], "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    # South face (Z = 15.98..16.02)
    elements.append({"from": [0, 13, 15.98], "to": [3, 16, 16.02], "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    elements.append({"from": [13, 13, 15.98], "to": [16, 16, 16.02], "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    # West face (X = -0.02..0.02)
    elements.append({"from": [-0.02, 13, 0], "to": [0.02, 16, 3], "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    elements.append({"from": [-0.02, 13, 13], "to": [0.02, 16, 16], "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    # East face (X = 15.98..16.02)
    elements.append({"from": [15.98, 13, 0], "to": [16.02, 16, 3], "faces": {"east": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})
    elements.append({"from": [15.98, 13, 13], "to": [16.02, 16, 16], "faces": {"east": {"uv": [0, 0, 16, 16], "texture": "#socket"}}})

    return {
        "textures": {
            "particle": tex,
            "socket": tex
        },
        "elements": elements
    }

def build_core_socket_center(tier):
    tex = f"velorise_emc:block/core_socket_t{tier}"
    # For Grindstone / Centered models, 4 surface corner cubes matching top surface corners
    elements = [
        {"from": [2, 12, 2], "to": [5, 15, 5]},
        {"from": [11, 12, 2], "to": [14, 15, 5]},
        {"from": [2, 12, 11], "to": [5, 15, 11]},
        {"from": [11, 12, 11], "to": [14, 15, 14]}
    ]
    el_list = []
    for c in elements:
        el_list.append({
            "from": c["from"],
            "to": c["to"],
            "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#socket"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#socket"},
                "north": {"uv": [0, 0, 16, 16], "texture": "#socket"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#socket"},
                "west": {"uv": [0, 0, 16, 16], "texture": "#socket"},
                "east": {"uv": [0, 0, 16, 16], "texture": "#socket"}
            }
        })
    return {
        "textures": {
            "particle": tex,
            "socket": tex
        },
        "elements": el_list
    }

for tier in range(1, 5):
    all_json = build_core_socket_all(tier)
    center_json = build_core_socket_center(tier)
    
    path_all = os.path.join(shared_models, f"core_socket_all_t{tier}.json")
    path_center = os.path.join(shared_models, f"core_socket_center_t{tier}.json")
    
    with open(path_all, "w", encoding="utf-8") as f:
        json.dump(all_json, f, indent=2)
    with open(path_center, "w", encoding="utf-8") as f:
        json.dump(center_json, f, indent=2)

print("Updated all core socket model JSON files to 3x3px surface corner markers!")
