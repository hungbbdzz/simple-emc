# Velorise EMC v2.0.0

Velorise EMC is a vanilla-focused transmutation, equipment and World Tier progression mod for **Forge 1.20.1** and **NeoForge 1.21.1**.

Convert items into EMC, learn them in a Transmutation interface and materialize them later. Shared World Tiers unlock four Wireless Core levels, four Resonant material families, stronger equipment and additional machine capacity or throughput.

## Transmutation

- Transmutation Table
- Arcane Transmutation Table with a larger interface and integrated crafting access
- Portable Transmutation Tablet
- Tome of Knowledge and Knowledge Sharing Book
- Default and Balanced EMC profiles
- Per-player materialization tax in Balanced Mode
- Server-defined EMC values, blacklists and materialization permissions

Transmutation Tables do **not** generate passive EMC.

## World Tiers and Wireless Cores

The world progresses through four shared tiers tied to Overworld, Nether, End and deep-world objectives. Each tier unlocks its matching Wireless Core and Resonant material family:

- Verdant
- Infernal
- Aetheric
- Echoing

Core installation is sequential for normal Core-bound equipment and machines. Core I-IV must be installed in the exactly matching Alchemical Anvil tier; Vanilla Anvils and lower or higher tiers cannot substitute. Higher Cores increase power, range, capacity, target count or throughput; they do not reduce the fixed price of the same successful action.

## Resonant Furnace family

The complete family is available:

- Resonant Furnace
- Resonant Smoker
- Resonant Blast Furnace

Apply Core I to the matching Vanilla block in an Alchemical Anvil, then upgrade the same machine sequentially through Core II-IV.

Each family member has one registry ID. Its Core level is stored on the item and Block Entity instead of registering separate blocks for every tier.

| Core | Cooking speed |
|---|---:|
| I | ×2 |
| II | ×4 |
| III | ×8 |
| IV | ×16 |

The machines use normal fuel, preserve normal recipe output counts and display working flame and arrow progress animations. Furnace uses smelting recipes; Smoker and Blast Furnace use their specialized recipes with the family smelting fallback. All three are available as JEI catalysts and in the mod creative tab.

## Machines and utilities

- Alchemical Anvil I-IV
- Alchemical Hourglass, starting at Core I and upgrading sequentially through Core IV
- Purification Grindstone
- Resonance Jukebox
- Matterburst TNT and Matterburst TNT Minecart
- Magic Mirror, Void Translocator, Attraction Catalyst and Chalice of Transmutation
- Far-Sight Glass, Bound Compass, Soulbound Recovery Compass and Purifying Milk

Portable workstations open their Vanilla-style menus but do not scan nearby storage.

## Arcane weapons

The current arsenal contains:

- Inferno Rod
- Ghastfire Staff
- Dragonfire Scepter
- Resonant Echo
- Wither Scepter
- Shulker Seeker
- Guardian Lens
- Evoker Fang Staff
- Stormcaller
- Frostbolt Wand
- Webcaster

Armor-family bonuses reinforce matching weapon classes, while every successful use remains subject to its configured EMC cost, ammunition rule or cooldown.

## Equipment and progression

Four Resonant material sets extend normal progression from Diamond and Netherite into Verdant, Infernal, Aetheric and Echoing equipment. Echobound endgame equipment is obtained through the final progression path.

Eligible Vanilla hostile mobs may receive stronger legal Vanilla equipment as World Tier rises. Equipment added by this system cannot drop, preventing farm-based duplication exploits.

## Economy and server control

Balanced Mode keeps renewable EMC sources useful without allowing direct equipment resale loops. Server owners can configure:

- EMC mode and custom values
- Learn, burn and materialization policies
- Materialization tax
- Core-scaled machine values
- Utility costs, ranges and cooldowns
- World Tier requirements and mob-equipment scaling

## Compatibility and source

Velorise EMC is a client-and-server mod. The source package contains both loader targets, shared gameplay code and shared resources. The project is licensed under the MIT License.
