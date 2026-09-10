# Velorise EMC

**More than transmutation — an entire progression system powered by EMC.**

Velorise EMC combines personal EMC storage, equivalent exchange, World Tier progression, Resonant equipment, Core upgrades, Arcane Weapons, utility items, machines, world-generated EMC structures and a final endgame encounter into one connected system.

This document is the player wiki for **Velorise EMC 1.9.0-alpha.61**. It describes the current gameplay implemented for:

- Forge 1.20.1
- NeoForge 1.21.1
- Single-player and multiplayer
- Client and dedicated server

Most numbers below are the default server values. Server owners can change many costs, ranges, progression requirements and scaling rules.

> Alpha notice: progression, recipes, configuration and saved item data may still change between alpha versions.

---

## Contents

1. [What makes Velorise EMC different](#what-makes-velorise-emc-different)
2. [Quick start](#quick-start)
3. [EMC and transmutation](#emc-and-transmutation)
4. [Philosopher's Stone](#philosophers-stone)
5. [Alchemical fuel](#alchemical-fuel)
6. [World Tier progression](#world-tier-progression)
7. [Resonant ores and materials](#resonant-ores-and-materials)
8. [Resonant tools and armor](#resonant-tools-and-armor)
9. [Wireless Cores](#wireless-cores)
10. [Alchemical Anvils and Core installation](#alchemical-anvils-and-core-installation)
11. [Universal Core upgrades](#universal-core-upgrades)
12. [Arcane Weapons](#arcane-weapons)
13. [Utility items](#utility-items)
14. [Machines and special blocks](#machines-and-special-blocks)
15. [Resonance Spires](#resonance-spires)
16. [World Threat and multiplayer scaling](#world-threat-and-multiplayer-scaling)
17. [Advancements](#advancements)
18. [The Last Echo](#the-last-echo)
19. [Controls](#controls)
20. [Server configuration overview](#server-configuration-overview)
21. [Frequently asked questions](#frequently-asked-questions)
22. [Complete content index](#complete-content-index)

---

## What makes Velorise EMC different

Velorise EMC is not only an item-conversion mod and not only a magic-equipment mod. Its systems are designed around one shared economy:

```text
Explore and fight
→ unlock World Tiers
→ reveal Resonant ores
→ craft Wireless Cores
→ strengthen equipment, utilities and machines
→ spend EMC to operate advanced effects
→ complete field tests and progression challenges
→ reach the Last Echo endgame
```

EMC is used in several different ways:

- Items can be burned into a personal EMC fund.
- Learned items can be recreated from that fund.
- EMC-powered machines store EMC locally and spend it only when work succeeds; fuel-based machines use their own fuel rules.
- Utility items spend EMC for teleportation, growth, movement or control.
- Core upgrades increase power, range, capacity or throughput.
- The same successful action normally keeps its fixed unit price instead of becoming cheaper at higher Core tiers.

The result is one progression loop rather than a collection of unrelated features.

---

## Quick start

A normal survival route is:

1. Craft a **Philosopher's Stone**.
2. Craft a **Transmutation Table**.
3. Burn items with EMC values and learn useful materials.
4. Open the **World Progress** screen from a Transmutation interface.
5. Complete the Overworld Hunt to unlock World Tier I.
6. Mine Verdant Resonance and craft Wireless Core I.
7. Craft Alchemical Anvil I from a Vanilla Anvil and Core I.
8. Use Core I to create your first upgraded utility, machine or imbued item.
9. Complete the Nether, End and Deep World conquests for Core II–IV.
10. Build Arcane Weapons, Resonant equipment and higher-tier machines.
11. Craft the Eye of the Last Echo after World Tier IV.
12. Find the Citadel, defeat the Echobound Dragon and craft Echobound equipment.

There is no single mandatory build order after Core I. Players may focus on transmutation, combat, machines, transport, utility or equipment while progressing through the same World Tiers.

---

## EMC and transmutation

### Personal EMC fund

Every player has a personal EMC account. The account is not stored inside one specific table, so the same balance is available through:

- Transmutation Table
- Arcane Transmutation Table
- Transmutation Tablet
- EMC Hub and HUD systems
- EMC-powered equipment and utility actions

Machine buffers are separate. Fuel placed into a machine belongs to that machine until used or recovered through its normal interaction rules.

### Burning items

Burning an item converts it into EMC and teaches that item when the server policy allows it.

Important rules:

- The item must have a positive EMC value.
- Blacklisted or non-materializable items may be prevented from being learned or recreated.
- Damaged items can be valued according to remaining durability.
- Stored EMC inside supported containers or equipment is handled by the relevant item logic.
- Enchantments do not automatically create extra resale value.

### Learning items

Once an item is learned, it can appear in the player's Transmutation interface and be recreated by spending EMC.

The learned-item system is personal. Two players do not automatically share knowledge.

### Materializing items

Materialization consumes the item's EMC value plus any active tax surcharge. The operation fails cleanly if the player cannot afford the complete cost.

Materialization does not create negative EMC and does not partially create an unaffordable stack.

### Default and Balanced value modes

The server selects one of two EMC value profiles:

| Mode | Purpose |
|---|---|
| Default | A permissive ProjectE-like economy with broad access to powerful conversions. |
| Balanced | Revalues important items, enables materialization tax by default and reduces easy inflation loops. |

Examples from the Balanced profile:

| Item | EMC |
|---|---:|
| Emerald | 512 |
| Diamond | 4,096 |

The server may also define custom EMC values, item blacklists and materialization permissions.

### Materialization tax

Balanced Mode enables a personal tax meter by default.

- Every materialized item adds one point.
- The meter recovers one point per real-time second.
- Tax never disables materialization.
- The current bracket adds a flat EMC surcharge to every created item.

| Tax points | Extra EMC per item |
|---:|---:|
| 0–64 | 0 |
| 65–128 | 2 |
| 129–256 | 4 |
| 257–512 | 8 |
| 513–1,024 | 32 |
| Above 1,024 | 64 |

This slows extreme bulk conversion without preventing ordinary use.

### Transmutation content

| Item or block | Function |
|---|---|
| Philosopher's Stone | Reusable crafting catalyst and block-transmutation tool. |
| Transmutation Table | Main personal EMC, learning and materialization interface. |
| Arcane Transmutation Table | Larger interface with double inventory space and integrated crafting access. |
| Transmutation Tablet | Portable Transmutation interface. |
| Tome of Knowledge | Consumed to learn every item currently permitted by the server EMC policy. |
| Knowledge Sharing Book | Stores one player's permitted learned-item list and imports it through a Transmutation interface. |

Neither Transmutation Table generates passive EMC.

### Knowledge Sharing Book

An empty Knowledge Sharing Book can be filled with the owner's permitted learned items by **Shift + Right-clicking** it.

A written book stores:

- Owner name
- Learned item registry IDs
- Number of stored entries

Use the written book through a Transmutation interface to import its permitted knowledge. The receiving server still applies its own current EMC policy.

---

## Philosopher's Stone

### Recipe

```text
Glowstone Dust | Redstone       | Glowstone Dust
Redstone       | Diamond        | Redstone
Glowstone Dust | Redstone       | Glowstone Dust
```

The Philosopher's Stone is reusable in supported crafting recipes and returns to the crafting grid instead of being consumed.

### Block transmutation

Right-click a supported block to cycle it into the next block in its family.

Supported families include:

- Wood planks
- Logs and stems
- Stripped logs and stems
- Wood blocks
- Leaves
- Saplings
- Wool and carpet colors
- Concrete and concrete powder colors
- Terracotta and glazed terracotta colors
- Stained glass and panes
- Candles
- Shulker box colors
- Stone and stone-brick variants
- Sandstone and red sandstone variants
- Dirt and soil variants
- Sand, red sand and gravel
- Ice variants
- Deepslate variants
- Quartz variants
- Copper oxidation and cut variants
- Nether and blackstone variants
- Prismarine variants
- Sponge and wet sponge

If the target block is worth more EMC than the source block, the Stone charges only the positive difference. Conversions to equal or cheaper blocks do not create an EMC refund.

Log and pillar orientation is preserved when both blocks support the same axis property.

---

## Alchemical fuel

The Philosopher's Stone is the reusable catalyst for the fuel chain.

| Fuel | Recipe | EMC | Burn time |
|---|---|---:|---:|
| Alchemical Coal | 4 Coal + Philosopher's Stone | 512 | 12,800 ticks |
| Mobius Fuel | 4 Alchemical Coal + Philosopher's Stone | 2,048 | 102,400 ticks |
| Aeternalis Fuel | 4 Mobius Fuel + Philosopher's Stone | 8,192 | 819,200 ticks |

Nine fuel items form the matching fuel block. Fuel blocks can also be unpacked.

| Fuel block | EMC |
|---|---:|
| Alchemical Coal Block | 4,608 |
| Mobius Fuel Block | 18,432 |
| Aeternalis Fuel Block | 73,728 |

These fuels work in Vanilla-compatible fuel slots, including every Resonant Furnace-family machine.

---

## World Tier progression

The world has four shared progression stages. Each stage unlocks a Wireless Core tier and a Resonant material tier.

### World Tier requirements

| World Tier | Default objectives | Unlock |
|---|---|---|
| I — Overworld Hunt | 180 total hostile kills; Zombie family 40; Skeleton family 35; Spider family 25; Creeper 20; Enderman 8; Witch 5; Illager 12 | Core I and Verdant Resonance |
| II — Nether Conquest | 180 total Nether hostile kills; Piglin family 35; Zombified Piglin 40; Blaze 25; Magma Cube 20; Ghast 8; Wither Skeleton 20; Wither 1 | Core II and Infernal Resonance |
| III — End Conquest | Enderman 50; Shulker 20; Ender Dragon 1 | Core III and Aetheric Resonance |
| IV — Deep World Conquest | Guardian 40; Elder Guardian 3; Warden 1 | Core IV, Echoing Resonance and the Last Echo route |

### Multiplayer contribution scaling

Default progression scaling is multiplicative:

```text
Requirement × 1.20^(additional contributors)
```

The final scale is capped at 3× by default. Boss objectives do not scale unless the server enables that option.

The server can change:

- Scaling mode: none, linear or multiplicative
- Per-player multiplier
- Maximum scale
- Maximum counted contributors
- Whether boss objectives scale
- Every individual kill requirement

### World Progress screen

A book button in the Transmutation interface opens the read-only World Progress screen.

It shows:

- Current World Tier
- Every objective and current count
- Contributor scaling
- Next Wireless Core unlock
- Progress shared by the current world/server

### Retrogen and existing worlds

Unlocking a World Tier does not require creating a new world.

The mod applies deterministic Resonant ore retrogen to existing chunks and marks processed chunks by tier. Newly unlocked ore can therefore appear in previously explored areas without repeatedly reprocessing the same tier.

---

## Resonant ores and materials

### Ore tiers

| Tier | Dimension and region | Required pickaxe | Raw/ore EMC | Ingot EMC | Cooking time |
|---|---|---|---:|---:|---:|
| Verdant | Overworld stone and deepslate | Diamond, Netherite or Verdant+ | 32,768 | 33,024 | 200 ticks / 10 s |
| Infernal | Nether | Netherite or Verdant+ | 65,536 | 66,048 | 400 ticks / 20 s |
| Aetheric | End | Infernal+ | 131,072 | 132,096 | 800 ticks / 40 s |
| Echoing | Deep Overworld | Aetheric+ | 262,144 | 264,192 | 1,600 ticks / 80 s |

Every ore drops its matching raw material.

- Fortune can increase the drop.
- Final output is capped at four raw items per ore.
- Silk Touch returns the ore block.
- Ore and raw material can be cooked in a Furnace, Blast Furnace or Smoker.

### Storage blocks

Nine ingots form one storage block.

| Block | EMC |
|---|---:|
| Verdant Resonant Block | 297,216 |
| Infernal Resonant Block | 594,432 |
| Aetheric Resonant Block | 1,188,864 |
| Echoing Resonant Block | 2,377,728 |

Infernal, Aetheric and Echoing material items are fire resistant. Verdant remains burnable.

---

## Resonant tools and armor

Each Resonant tier contains:

- Sword
- Pickaxe
- Axe
- Shovel
- Hoe
- Helmet
- Chestplate
- Leggings
- Boots

They use normal Vanilla-shaped crafting patterns with their matching ingot. They do not require separate Resonant smithing templates.

### Tool statistics

| Tier | Tool durability | Mining speed | Sword attack damage | Enchantability |
|---|---:|---:|---:|---:|
| Verdant | 1,800 | 8.5 | 7.5 | 12 |
| Infernal | 3,500 | 12.0 | 11.0 | 17 |
| Aetheric | 6,000 | 15.0 | 13.0 | 21 |
| Echoing | 10,000 | 18.0 | 15.0 | 26 |

### Armor statistics

The slot order below is Helmet / Chestplate / Leggings / Boots.

| Tier | Defense | Durability | Toughness | Knockback resistance per piece |
|---|---|---|---:|---:|
| Verdant | 3 / 8 / 6 / 3 | 310 / 451 / 423 / 366 | 2.5 | 0.05 |
| Infernal | 4 / 9 / 7 / 3 | 440 / 640 / 600 / 520 | 4.0 | 0.15 |
| Aetheric | 5 / 10 / 7 / 4 | 560 / 815 / 764 / 661 | 5.5 | 0.20 |
| Echoing | 6 / 11 / 8 / 5 | 760 / 1,105 / 1,036 / 899 | 7.5 | 0.25 |

### Full-set bonuses

Only a complete matching four-piece set activates its set bonus.

| Full set | Bonus |
|---|---|
| Verdant | Foundation equipment for the Resonance progression. |
| Infernal | 10% less incoming damage; +10% Inferno Rod, Ghastfire Staff and Wither Scepter power. |
| Aetheric | 20% less incoming damage; +10% Dragonfire and Shulker power; +10% Void Translocator range. |
| Echoing | 30% less incoming damage; +50% Resonant Echo power; +10% other Arcane Weapon power. |
| Echobound | 80% less incoming damage after normal armor reduction; Creative flight. |

The strongest matching damage-reduction set is applied once; reductions are not stacked together.

---

## Wireless Cores

Wireless Cores are the main upgrade currency for equipment, machines and utility items.

### Core I recipe

```text
Diamond    | Ender Pearl           | Diamond
Ender Pearl| Verdant Resonant Ore  | Ender Pearl
Diamond    | Ender Pearl           | Diamond
```

Core I has a fixed EMC value of **67,584**.

### Nine-to-one condensation

Every Condensed Core requires nine matching Wireless Cores:

```text
Core | Core | Core
Core | Core | Core
Core | Core | Core
```

Nine matching Condensed Cores create a Condensed Core Generator block.

### Core-bound equipment bonuses

Core upgrades are passive. They do not require an operating mode and do not continuously drain EMC.

| Core | Damage bonus | Armor per piece | EMC repair cost per durability |
|---|---:|---:|---:|
| I | +1 | +1 | 64 |
| II | +2 | +2 | 32 |
| III | +4 | +4 | 16 |
| IV | +8 | +8 | 8 |

Core binding does not increase the mining speed or mining tier of ordinary pickaxes, axes, shovels or other tools. Its universal tool benefit is EMC repair; damage-capable items also receive the configured damage bonus. The Echobound Pickaxe is a separate post-boss item whose universal near-instant mining is intrinsic to that pickaxe, not granted by a Core.

### Core ascension chain

Core II–IV use the Vanilla Smithing Table.

| Result | Template | Base | Addition | Fixed result EMC |
|---|---|---|---|---:|
| Core II | Infernal Ascension Template | Condensed Core I | Alchemical Coal Block | 719,328 |
| Core III | End Ascension Template | Condensed Core II | Mobius Fuel Block | 6,685,440 |
| Core IV | Deep Convergence Template | Condensed Core III | Aeternalis Fuel Block | 60,709,152 |

### Ascension templates

#### Infernal Ascension Template

Uses a Netherite Upgrade Smithing Template in the center, surrounded by:

- Blaze Rod
- Ghast Tear
- Magma Cream
- Crying Obsidian
- Wither Skeleton Skull
- Ancient Debris
- Nether Wart
- Gilded Blackstone

Fixed EMC: **106,464**.

#### End Ascension Template

Uses the Infernal Ascension Template in the center, surrounded by:

- Dragon's Breath
- Shulker Shell
- Dragon Head
- End Crystal
- Chorus Flower
- End Rod
- Purpur Block
- Spire Armor Trim Smithing Template

Fixed EMC: **193,056**.

#### Deep Convergence Template

Uses the End Ascension Template in the center, surrounded by:

- Echo Shard
- Recovery Compass
- Disc Fragment 5
- Sculk Catalyst
- Silence Armor Trim Smithing Template
- Heart of the Sea
- Sponge
- Tide Armor Trim Smithing Template

Fixed EMC: **466,464**.

### Condensed Core Generators

A Condensed Core Generator produces EMC only for eligible Survival players standing within a three-block cube around it.

| Generator | Total output per second |
|---|---:|
| Condensed Core Generator I | 256 EMC/s |
| Condensed Core Generator II | 2,304 EMC/s |
| Condensed Core Generator III | 20,736 EMC/s |
| Condensed Core Generator IV | 186,624 EMC/s |

Rules:

- Creative and Spectator players do not receive output.
- A generator has one fixed total output.
- Nearby eligible players split that output instead of duplicating it.
- Multiple nearby generators can contribute independently.
- The player must remain close; these are not unlimited chunk-wide generators.

---

## Alchemical Anvils and Core installation

### Creating the anvil line

The four Alchemical Anvils are crafted as shapeless recipes. They are not created inside a Vanilla Anvil:

| Ingredients | Result |
|---|---|
| Vanilla Anvil + Core I | Alchemical Anvil I |
| Alchemical Anvil I + Core II | Alchemical Anvil II |
| Alchemical Anvil II + Core III | Alchemical Anvil III |
| Alchemical Anvil III + Core IV | Alchemical Anvil IV |

An Alchemical Anvil:

- Does not degrade through use.
- Removes the Vanilla “Too Expensive!” limit.
- Can pay missing experience levels with EMC.
- Charges 128 EMC per missing level by default.
- Gates the highest Core tier that may be installed.

### Exact station tiers

| Core being installed | Required station |
|---|---|
| Core I | Alchemical Anvil I |
| Core II | Alchemical Anvil II |
| Core III | Alchemical Anvil III |
| Core IV | Alchemical Anvil IV |

The tier must match exactly. A Vanilla Anvil cannot install a Core, and a lower or higher Alchemical Anvil cannot substitute. The corresponding World Tier must also be unlocked.

### Sequential upgrades

Most already-upgraded equipment and machines must progress one Core at a time.

Special cases:

- Matterburst TNT and Matterburst Minecart can be created directly at the inserted Core tier.
- Crafted Arcane Weapons begin at Core I and must advance one tier at a time in the exactly matching Alchemical Anvil.
- Fixed Core-I portable workstations do not store upgrade tiers.

### Shared passive Core bonuses

| Core | Damage bonus when the item deals damage | Armor per piece | EMC repair cost per durability |
|---|---:|---:|---:|
| I | +1 | +1 | 64 EMC |
| II | +2 | +2 | 32 EMC |
| III | +4 | +4 | 16 EMC |
| IV | +8 | +8 | 8 EMC |

Core upgrades normally increase capability rather than reducing the unit cost of the same action.

---

## Universal Core upgrades

Many Vanilla and compatible modded items can be imbued through an Alchemical Anvil.

### Generic damageable equipment

Eligible weapons, tools and armor can receive:

- Core tier data
- Passive weapon damage
- Armor bonus
- EMC repair
- Active mode where the item category supports it

Core binding does not make normal pickaxes, axes or shovels mine faster. Server compatibility lists can force specific modded items to be included or restricted to repair-only behavior.

### Core-bound hoes

A mature-crop harvest expands by Core tier:

| Core | Harvest area |
|---|---:|
| I | 3×3 |
| II | 5×5 |
| III | 7×7 |
| IV | 9×9 |

Only successful mature-crop interactions are processed.

### Imbued Fishing Rod

A successful catch creates more catch output while charging one fixed successful-catch price.

| Core | Catch output |
|---|---:|
| I | 2× |
| II | 4× |
| III | 8× |
| IV | 16× |

Default successful-catch cost: **256 EMC**.

### Imbued Elytra

The Elytra boost key applies a forward impulse while gliding.

| Core | Boost impulse |
|---|---:|
| I | 0.90 |
| II | 1.10 |
| III | 1.35 |
| IV | 1.65 |

- Cooldown: 3 seconds.
- EMC cost: the current EMC value of one Firework Rocket per successful boost.
- The Elytra also receives the normal Core armor bonus.

### Imbued Turtle Helmet

Activation costs **2,048 EMC** and grants:

- Water Breathing for 60 seconds
- Dolphin's Grace for 30 seconds

### Far-Sight Glass

A Core-I Spyglass conversion with mouse-wheel zoom while in use. It has no artificial gameplay zoom cap.

### Bound Compass

A Core-I Compass conversion that locks to a chosen block position.

Default lock cost: **256 EMC**.

### Soulbound Recovery Compass

A Core-I Recovery Compass conversion that channels toward the player's stored death position.

- Channel time: 3 seconds.
- Movement tolerance: 0.5 blocks.
- Cooldown: 15 seconds.
- Cost: distance-based EMC plus a cross-dimension surcharge when applicable.
- The destination safety search checks a small horizontal and vertical area.

### Cursed Igniter

Created by applying Core I to Flint and Steel.

- Places custom blue Soul Fire on valid blocks.
- Block placement has no durability loss and no normal block-use cooldown.
- Entity ignition costs 1,024 EMC at every tier.
- Entity cooldown: 6 seconds.

| Core | Direct damage | Burn duration |
|---|---:|---:|
| I | 4 | 5 s |
| II | 8 | 8 s |
| III | 12 | 12 s |
| IV | 16 | 16 s |

Fire-based Arcane attacks ignite targets before damage, allowing supported food-dropping mobs to produce cooked drops when killed by the attack.

---

## Arcane Weapons

Arcane Weapons are crafted at Core I and upgraded through Alchemical Anvils. Their difficulty, ingredients and Core-I strength reflect the mob or location that inspired them. Higher Core tiers increase their signature mechanic as well as damage, range or duration.

Tooltips show:

- Current Core tier
- Damage or primary effect
- Range
- EMC cost
- Cooldown or reload behavior

### Shared staff recipe silhouette

Most Arcane Weapons use this shape:

```text
Empty | Optional material | Signature material
Empty | Wireless Core I   | Optional material
Stick | Empty             | Empty
```

Inferno Rod is the exception and uses a Fire Charge instead of a Stick.

### Complete weapon statistics

| Weapon | Core mechanic | Damage I / II / III / IV | Range I / II / III / IV | Default EMC | Main cooldown |
|---|---|---:|---:|---:|---:|
| Inferno Rod | Three rapid fire shots | 3 / 4 / 5 / 6 each | 10 / 15 / 20 / 25 | 128 / 256 / 512 / 1,024 per shot | 3 s after burst |
| Ghastfire Staff | Explosive fireball; radius 3 / 4 / 5 / 6 | 6 / 8 / 10 / 12 | 10 / 15 / 20 / 25 | 256 / 512 / 1,024 / 2,048 | 5 s |
| Dragonfire Scepter | Dragon fireball and five-second damage cloud | 8 / 12 / 16 / 20 direct | 15 / 20 / 25 / 30 | 256 / 512 / 1,024 / 2,048 | 6 s |
| Resonant Echo | Charged piercing sonic beam | 10 / 15 / 20 / 25 | 16 fixed | 512 / 1,024 / 2,048 / 4,096 | 6 s |
| Wither Scepter | Two casts; 1 / 2 / 3 / 4 skulls per cast | 7 / 10 / 13 / 16 each | 15 / 20 / 25 / 30 | 384 / 768 / 1,536 / 3,072 per cast | 5 s after second cast |
| Shulker Launcher | 1 / 2 / 3 / 4 manually aimed bolts | 4 each | 14 / 18 / 22 / 26 | 256 / 384 / 512 / 768 | 4 s |
| Guardian Lens | Channeled precision beam | 6 / 9 / 12 / 15 per damage step | 14 / 18 / 22 / 26 | 24 / 48 / 96 / 192 EMC/s | 3 s |
| Evoker Fang Staff | Five fang positions repeated for 1 / 2 / 3 / 4 waves | Vanilla fang damage | 8 / 11 / 14 / 17 | 384 / 768 / 1,536 / 3,072 | 5 s |
| Stormcaller | 1 / 2 / 3 / 4 lightning calls before reload | 8 / 11 / 14 / 17 | 16 / 20 / 24 / 28 | 1,024 / 2,048 / 4,096 / 8,192 per call | 8 s after final call |
| Frostbolt Wand | 1 / 2 / 3 / 4 slowing shots before recovery | 3 / 5 / 7 / 9 | 18 / 22 / 26 / 30 | 64 / 128 / 256 / 512 per shot | 1.5 s after final shot |
| Webcaster | Three shots; temporary web and venom | Poison I / I / I / II | 16 / 20 / 24 / 28 | 64 / 128 / 256 / 512 per shot | 3 s after burst |

### Inferno Rod

Recipe:

```text
Empty       | Blaze Rod       | Blaze Rod
Empty       | Wireless Core I | Blaze Rod
Fire Charge | Empty           | Empty
```

Behavior:

- Fires three small projectiles in a rapid burst.
- Ignites before applying direct damage.
- Burn duration is five seconds.
- Full Infernal armor increases its power by 10%.

### Ghastfire Staff

Recipe:

```text
Empty | Fire Charge     | Ghast Tear
Empty | Wireless Core I | Gunpowder
Stick | Empty           | Empty
```

Behavior:

- Fires an explosive Ghast-style projectile.
- Blast radius grows from 3 to 6 blocks.
- Ignites before direct damage.
- Full Infernal armor increases power by 10%.

### Dragonfire Scepter

Recipe:

```text
Empty | Dragon's Breath | Dragon Head
Empty | Wireless Core I | Fire Charge
Stick | Empty           | Empty
```

Behavior:

- Fires a Dragon-style projectile.
- Creates a five-second pulsing cloud.
- The cloud pulses every 0.5 seconds at a fraction of direct damage.
- Full Aetheric armor increases power by 10%.

### Resonant Echo

Recipe:

```text
Empty | Echo Shard      | Recovery Compass
Empty | Wireless Core I | Echo Shard
Stick | Empty           | Empty
```

Behavior:

- Charges for one second.
- Fires a piercing sonic beam through targets.
- Fixed 16-block range.
- Full Echoing armor increases its damage by 50%.

This is separate from the post-boss Echobound Greatsword. The Greatsword has its own much wider endgame Sonic Boom.

### Wither Scepter

Recipe:

```text
Empty | Wither Skeleton Skull | Wither Skeleton Skull
Empty | Wireless Core I       | Wither Skeleton Skull
Stick | Empty                 | Empty
```

Behavior:

- Allows two activations before its main cooldown.
- Each activation fires 1 / 2 / 3 / 4 skulls by Core tier.
- Applies Wither for 4 / 5 / 6 / 7 seconds.
- Skull explosion radius grows slightly by tier.
- Full Infernal armor increases power by 10%.

At Core IV, two complete casts release eight skulls before reload.

### Shulker Launcher

Registry ID: `velorise_emc:shulker_seeker`.

Recipe:

```text
Empty | End Rod         | Shulker Shell
Empty | Wireless Core I | Popped Chorus Fruit
Stick | Empty           | Empty
```

Behavior:

- Bolts are aimed by the player and do not automatically seek targets.
- Fires 1 / 2 / 3 / 4 bolts by Core tier.
- Every bolt deals 4 damage.
- Levitation lasts 3 / 4 / 5 / 6 seconds.
- Full Aetheric armor increases power by 10%.

### Guardian Lens

Recipe:

```text
Empty | Prismarine Crystals | Heart of the Sea
Empty | Wireless Core I     | Nautilus Shell
Stick | Empty               | Empty
```

Behavior:

- Channels a Guardian-style beam while held on a target.
- Maximum channel duration is 6 / 8 / 10 / 12 seconds.
- Charges EMC continuously while the beam operates.
- Cooldown begins after the channel ends.

### Evoker Fang Staff

Recipe:

```text
Empty | Bone Block      | Totem of Undying
Empty | Wireless Core I | Emerald Block
Stick | Empty           | Empty
```

Behavior:

- Chooses five fang positions once.
- Repeats the exact same positions for 1 / 2 / 3 / 4 delayed waves.
- Higher tiers do not extend the line farther; they repeat the original attack area.

### Stormcaller

Recipe:

```text
Empty | Lightning Rod   | Trident
Empty | Wireless Core I | Copper Block
Stick | Empty           | Empty
```

Behavior:

- Allows 1 / 2 / 3 / 4 calls before the main reload.
- Damages and ignites targets in a small Core-scaled area.
- The displayed lightning remains controlled and does not freely ignite structures.
- Creepers struck by Stormcaller can become charged Creepers.

### Frostbolt Wand

Recipe:

```text
Empty | Snowball        | Blue Ice
Empty | Wireless Core I | Amethyst Shard
Stick | Empty           | Empty
```

Behavior:

- Allows 1 / 2 / 3 / 4 rapid shots before recovery.
- Slowness lasts 3 / 4 / 5 / 6 seconds.
- Cheap per shot, intended for repeated single-target control.

### Webcaster

Recipe:

```text
Empty | Cobweb          | Fermented Spider Eye
Empty | Wireless Core I | Cobweb
Stick | Empty           | Empty
```

Behavior:

- Fires three shots before its normal cooldown.
- Creates temporary cobweb terrain for 15 / 30 / 45 / 60 seconds.
- Venom remains active for only 6 / 9 / 12 / 15 seconds.
- Core I–III apply Poison I.
- Core IV applies Poison II.
- Overlapping webs refresh the strongest valid effect instead of stacking repeated damage every tick.

### Armor synergy summary

| Armor | Arcane synergy |
|---|---|
| Infernal | +10% Inferno, Ghastfire and Wither power |
| Aetheric | +10% Dragonfire and Shulker power |
| Echoing | +50% Resonant Echo; +10% all other Arcane Weapons |
| Echobound | Counts as the strongest Echoing-family set for Arcane multipliers |

---

## Utility items

### Attraction Catalyst

Right-click to toggle item and entity attraction.

| State | Area diameter |
|---|---:|
| No Core | 3 |
| Core I | 5 |
| Core II | 7 |
| Core III | 9 |
| Core IV | 11 |

- Cost: 8 EMC per active tick.
- Pull strength: 0.25 by default.
- Core increases area, not unit price.

Recipe:

```text
Iron Block     | Empty           | Iron Block
Redstone Block | Empty           | Lapis Block
Redstone Block | Wireless Core I | Lapis Block
```

### Chalice of Transmutation

Use directly on a growable block or fluid.

| State | Horizontal area |
|---|---:|
| No Core | 3×3 |
| Core I | 5×5 |
| Core II | 7×7 |
| Core III | 9×9 |
| Core IV | 11×11 |

Default costs:

- Grow one valid plant: 64 EMC
- Begin fluid-clearing operation: 200 EMC
- Water removed: 10 EMC per block
- Lava removed: 50 EMC per block
- Other supported fluid: 20 EMC per block

Only successful blocks are charged.

### Magic Mirror

Hold right-click for three seconds to return to:

- Bed spawn
- World spawn when no valid bed is available
- A linked Waypoint Anchor when using the alternate interaction

| Core | Same-dimension maximum range | EMC per block |
|---|---:|---:|
| I | 1,024 | 2.0 |
| II | 2,048 | 1.5 |
| III | 4,096 | 1.0 |
| IV | 8,192 | 0.5 |

- Cross-dimension surcharge: 2,500 EMC.
- Channel time: 3 seconds.
- Cooldown: 15 seconds.

### Waypoint Anchor

A placed destination that can be linked to the Magic Mirror.

Recipe materials:

- Obsidian around the edges
- Ender Pearl
- Wireless Core I
- Lodestone

### Void Translocator

Right-click to teleport safely in the direction you are looking.

| Core | Range |
|---|---:|
| I | 15 blocks |
| II | 20 blocks |
| III | 25 blocks |
| IV | 30 blocks |

- Cost: 1,024 EMC per use.
- Cooldown: 6 seconds.
- Full Aetheric or Echoing-family armor adds 10% range.

### Purifying Milk

A reusable Core-powered Milk Bucket conversion.

- Removes harmful effects according to the installed Core's eligibility.
- Fixed cost: 4,096 EMC per successful use.
- Higher Core tiers increase what may be removed rather than reducing the price.

### Portable workstations

These fixed Core-I utilities open their Vanilla-style menus while held and retain normal placed-block behavior:

- Resonant Crafting Table
- Portable Stonecutter
- Portable Loom
- Portable Cartography Table

They have no higher Core levels and do not scan nearby storage inventories.

---

## Machines and special blocks

### Resonant Furnace family

Core I applied to the matching Vanilla machine in an Alchemical Anvil creates:

- Resonant Furnace (`velorise_emc:resonant_furnace`)
- Resonant Smoker (`velorise_emc:resonant_smoker`)
- Resonant Blast Furnace (`velorise_emc:resonant_blast_furnace`)

Each family member uses one permanent registry ID. Core level is stored on the item and Block Entity, so separate `_i`, `_ii`, `_iii` and `_iv` block registrations are not used. Breaking and replacing the machine preserves its installed Core.

Upgrade each machine sequentially with Core II, III and IV in the matching Alchemical Anvil tier.

| Core | Cooking speed |
|---|---:|
| I | ×2 |
| II | ×4 |
| III | ×8 |
| IV | ×16 |

Rules:

- The fuel slot uses normal Vanilla fuel logic. Coal, wood, Alchemical Fuel and compatible modded fuels work normally.
- The family has no personal-EMC loading button and no local EMC heat buffer.
- Core level multiplies cooking progress; it does not duplicate output or alter recipe output count.
- Furnace uses smelting recipes.
- Smoker prefers smoking recipes and supports the shared smelting fallback implemented by the machine family.
- Blast Furnace prefers blasting recipes and supports the same smelting fallback.
- A valid recipe, fuel and enough output space are still required.
- Resonant ores may be processed in any compatible member of the family; ore tier is not gated by machine Core.
- The Vanilla flame and arrow progress animations are rendered on both Forge 1.20.1 and NeoForge 1.21.1.
- All three machines are exposed as JEI catalysts and appear in the Velorise EMC creative tab as Core I stacks.

### Alchemical Hourglass

The Hourglass uses a full three-dimensional frame, glass chamber and visible sand model. It accelerates compatible nearby machines once per second.

Default behavior:

- Local EMC capacity: 2,000,000.
- Pulse interval: 20 ticks.
- Adds a fixed +20 virtual machine ticks per pulse.
- Costs 2 EMC for each virtual tick that actually advances a target.
- Failed or ineligible targets are not charged.

| Core | Radius |
|---|---:|
| I | 4 blocks |
| II | 6 blocks |
| III | 8 blocks |
| IV | 10 blocks |

The crafting recipe already consumes Core I, so the Hourglass has no Core-0 variant. Existing untagged Hourglasses are normalized to Core I. Upgrade the same item sequentially with Core II-IV in the exactly matching Alchemical Anvil tier. Core increases reachable area, not the +20 tick bonus.

### Purification Grindstone

A Core-powered Grindstone conversion for extracting enchantments.

- Fixed cost: 8,096 EMC per successful operation.
- Core I can extract all normal enchantments from the primary item.
- Core II can extract one normal enchantment.
- Core III–IV unlock the second input slot for curse extraction.
- Extracted operations return experience according to the preview.
- The operation does not complete unless the player can afford the full EMC cost.

### Tidal Conduit

A Core-powered Conduit aura machine.

| Core | Range |
|---|---:|
| I | 32 blocks |
| II | 64 blocks |
| III | 96 blocks |
| IV | 128 blocks |

- Local EMC capacity: 2,000,000.
- Operating cost: 64 EMC per second.
- Pulses every 10 seconds and maintains an 11-second effect duration by default.

### Matterburst TNT and Minecart

A Core may be applied directly to TNT or TNT Minecart at any unlocked tier.

| Core | Excavation radius |
|---|---:|
| I | 4 |
| II | 8 |
| III | 16 |
| IV | 32 |

Default excavation profile:

- Depth multiplier: 0.625× radius
- Minimum depth: 3
- Entity damage radius: 1.5× excavation radius
- Base entity damage: 24
- Additional damage: 4 per radius block
- Work is spread across ticks to avoid deleting the full area in one server tick

Matterburst Minecart places a Core-tagged TNT Minecart on rails.

### Deepbound Ender Chest

A personal expanded Ender-linked storage block.

- Core I conversion from an Ender Chest.
- Storage follows the player rather than one physical chest.
- It is the only active Deepbound storage block in the current version.

### Resonance Jukebox

A Core-powered multi-disc Jukebox.

| Core | Accessible disc slots |
|---|---:|
| I | 8 |
| II | 12 |
| III | 16 |
| IV | 20 |

Features:

- Stacked duplicate discs are consolidated where possible.
- Play, stop, next and previous controls.
- Repeat modes.
- Shuffle.
- Adjustable volume from 10% to 100%.
- Redstone comparator output follows the selected disc.
- Local EMC capacity: 250,000.
- Default operating cost: 1 EMC per second.

The advancement **The World's Soundtrack** asks for every available music-disc type, capped at 20 distinct discs.

### Resonant Crafting Table

A fixed Core-I portable Crafting Table.

- Held use opens crafting.
- Placed use behaves like a normal crafting station.
- It does not access nearby inventories.
- It has no Core levels or range upgrade.

---

## Resonance Spires

Resonance Spires are world-generated EMC structures activated by World Tier.

They alternate between an active output phase and a recharge phase.

| Tier | Reserve | Output per nearby player per second | Radius | Siphon damage per second | Default spacing |
|---|---:|---:|---:|---:|---:|
| I | 1,228,800 | 2,048 | 8 | 1 | 600 blocks |
| II | 4,915,200 | 8,192 | 12 | 2 | 1,000 blocks |
| III | 19,660,800 | 32,768 | 16 | 4 | 1,500 blocks |
| IV | 78,643,200 | 131,072 | 24 | 8 | 2,000 blocks |

Default cycle:

- Approximately 10 active minutes.
- Approximately 10 recharge minutes.
- During recharge, nearby players may suffer **Vital Siphon**.

Other defaults:

- No Spire within 256 blocks of world spawn.
- 50% deterministic chance for a Tier IV Spire at an Ancient City structure start.

World Spires and Condensed Core Generators are separate systems. Spires are progression structures; Condensed Core Generators are player-crafted short-range blocks.

---

## World Threat and multiplayer scaling

### World Threat

Hostile mobs can become stronger as World Tier increases.

Default scaling:

- +10 maximum health per World Tier.
- +2 attack damage per World Tier.
- Existing low-health mobs preserve at least half-health proportionality when scaling is applied.
- Modded hostile mobs are included by default unless excluded by server configuration.

Eligible Vanilla hostile mobs may also spawn with legal Vanilla equipment and enchantments based on World Tier.

- Generated equipment has zero drop chance.
- Already-loaded mobs are not repeatedly upgraded.
- The system does not inject Velorise EMC equipment into ordinary mob loot.

### Boss player scaling

The Ender Dragon, Wither and Warden can scale with nearby eligible players.

Defaults:

- Enabled.
- Maximum counted players: 10.
- +100 health per counted player.
- +2 damage per counted player.

Advancements track defeating bosses at higher configured scaling levels.

---

## Advancements

Velorise EMC uses separate advancement tabs for:

- **Equivalent Exchange** — EMC, materials, Core and equipment progression
- **Resonant Engineering** — machines, utilities and Arcane Weapon field tests
- **World Resonance** — World Tier, Spires, threat and multiplayer boss scaling
- **The Last Echo** — final boss and post-boss equipment

### Equivalent Exchange progression

| Advancement | Requirement |
|---|---|
| Equivalent Exchange | Create a Philosopher's Stone. |
| Matter, Rewritten | Build a Transmutation Table. |
| Everything Has a Price | Burn the first item into EMC. |
| Matter from Memory | Materialize the first learned item. |
| Condensed Wealth | Hold at least 65,536 EMC. |
| Alchemical Fortune | Hold at least 1,048,576 EMC. |
| Industrial Materialization | Materialize at least 1,024 items over time. |
| Alchemy Without Walls | Obtain a Transmutation Tablet. |
| The Greater Exchange | Obtain an Arcane Transmutation Table. |
| Shared Understanding | Obtain a Knowledge Sharing Book. |
| Coal Beyond Coal | Create Alchemical Coal. |
| Perpetual Motion | Create Mobius Fuel. |
| Eternal Flame | Create Aeternalis Fuel. |
| Verdant Resonance | Mine Raw Verdant Resonance with the required pickaxe. |
| Infernal Resonance | Mine Raw Infernal Resonance with the required pickaxe. |
| Aetheric Resonance | Mine Raw Aetheric Resonance with the required pickaxe. |
| Echoing Resonance | Mine Raw Echoing Resonance with the required pickaxe. |
| Clad in the Deep | Collect the complete Echoing armor set. |
| A Core of Possibility | Create Core I. |
| Nine to One | Compress nine Wireless Cores into one Condensed Core. |
| Management Block | Compress nine Condensed Cores into a generator block. |
| Core Values | Carry Core I–IV at the same time. |
| Awakened Matter | Bind a Core to compatible equipment. |
| Perfect Resonance | Raise a Core-bound item to Core IV. |
| No Skipping Orientation | Upgrade the same item sequentially through every Core tier. |
| Resonance Incarnate | Wear Core IV on every armor piece. |
| Overqualified | Install Core IV in an Arcane Weapon. |
| Full Promotion | Raise every Arcane Weapon to Core IV. |

### Resonant Engineering progression

Machine and utility milestones include:

| Advancement | Requirement |
|---|---|
| First Pulse | Create or upgrade any Core I machine. |
| Industry Ascendant | Raise a Core-powered machine to Core IV. |
| Resonant Industry | Obtain a Resonant Furnace. |
| The Complete Foundry | Collect Furnace, Smoker and Blast Furnace variants. |
| Microwave Economics | Collect 64 products from the Furnace family. |
| Not a Duplication Bug | Take at least 16 items at once from a Core IV Furnace-family output slot. |
| Industrial Revolution, Again | Collect output from all three Core IV Furnace-family machines. |
| Borrowed Seconds | Obtain an Alchemical Hourglass. |
| Paid by the Second | Spend 4,096 EMC through Hourglass acceleration. |
| Clean Slate | Obtain a Purification Grindstone. |
| Storage Without Distance | Obtain a Deepbound Ender Chest. |
| Tides Obey | Obtain a Tidal Conduit. |
| A Song Remembered | Obtain a Resonance Jukebox. |
| The World's Soundtrack | Store every available disc type, up to 20. |
| Home Is a Reflection Away | Obtain a Magic Mirror. |
| Step Through Nothing | Obtain a Void Translocator. |
| Everything Comes to You | Obtain an Attraction Catalyst. |
| Master of Convenient Matter | Collect Mirror, Translocator and Chalice. |
| Purification | Obtain Purifying Milk. |

### Arcane Weapon field tests

| Advancement | Requirement |
|---|---|
| Fighting Fire with Fire | Hit a Blaze with Inferno Rod. |
| Take Your Ball | Kill a Ghast with Ghastfire Staff. |
| Breath Mint | Use Dragonfire on the Ender Dragon. |
| Echo Chamber | Answer a Warden with Resonant Echo. |
| Internal Promotion | Use Wither Scepter on a Wither Skeleton. |
| Package Returned | Launch a Shulker projectile at a Shulker. |
| Look Into My Eye | Hold Guardian Lens on one Guardian for six seconds. |
| Performance Review | Let an Evoker die during its own fang demonstration. |
| Channeling Not Included | Call Stormcaller lightning on a Drowned. |
| Thermal Disagreement | Hit a Blaze with Frostbolt. |
| Web Developer | Deploy Webcaster directly onto a Spider. |
| Mob-Sourced Quality Assurance | Complete every main Arcane Weapon field test. |

### Hidden combat challenges

These remain hidden until discovered:

| Challenge | Requirement |
|---|---|
| You're Fired | Kill a Blaze with Inferno Rod. |
| Well Done | Kill a food-dropping mob with an Arcane fire weapon and cook the drop. |
| Collateral Architecture | Hit at least five creatures with one Ghastfire explosion. |
| Eight Business Skulls | Fire both complete Core IV Wither volleys. |
| Skeleton Management | Kill a Wither Skeleton with Wither Scepter. |
| Ground Control | Raise a creature at least 12 blocks with Shulker Launcher. |
| Air Mail | Cause a creature to die from falling after Shulker levitation. |
| Dental Appointment | Keep an Evoker in all four Core IV fang waves. |
| Highly Conductive | Create a charged Creeper with Stormcaller. |
| Fourcast | Use all four Core IV Stormcaller calls before reload. |
| Cold Reception | Kill a Blaze with Frostbolt. |
| Brain Freeze | Land all four Core IV Frostbolts on one target. |
| Terms and Conditions | Keep a creature in venomous web for 10 seconds. |
| Incognito Mode | Catch a Cave Spider with Webcaster. |
| Speak Up | Hit at least 10 creatures with one Resonant Echo. |
| Urban Renewal | Kill five creatures with one Resonant Echo in the Deep Dark. |
| Dragon These Nuts | Finish the Ender Dragon with Dragonfire. |
| Elemental Disagreement | Hit one creature with fire, frost and Stormcaller within 10 seconds. |
| Crowd Control Department | Apply web, poison, slowness and levitation to one creature. |
| This Seems Personal | Complete six matching-mob rematches. |

### World Resonance progression

| Advancement | Requirement |
|---|---|
| The World Noticed | Complete World Tier I. |
| This Was Your Idea | Complete World Tier II. |
| Management Has Arrived | Complete World Tier III. |
| Final Warning | Complete World Tier IV. |
| One Boss Remaining | Reach World Tier IV and prepare for the final encounter. |
| The Tower Hungers | Experience Vital Siphon from a recharging Spire. |
| The World Hardens | Defeat a World-Threat-strengthened hostile. |
| Apex Survivor | Defeat a hostile at World Threat Tier IV. |
| A Stronger Foe | Defeat a boss affected by player-count scaling. |
| War Party | Defeat a boss scaled for at least five players. |
| United Front | Defeat a boss at the configured player cap. |

### Hidden world and economy challenges

| Challenge | Requirement |
|---|---|
| Bankruptcy | Complete an EMC transaction that leaves the account at exactly zero. |
| Not Rich Enough | Hold at least 1,048,576 EMC and still fail to afford an action. |
| A Questionable Investment | Burn an item and recreate the same item within 30 seconds. |
| Absolutely Purified | Spend a Core IV Purifying Milk use with nothing harmful to remove. |
| This Seems Unsafe | Survive 15 continuous seconds of Vital Siphon. |
| Understaffed | Spawn a boss at the configured scaling cap with only one eligible player in the dimension. |
| Meeting Could Have Been an Email | Defeat a cap-scaled boss with only one eligible player nearby. |
| Human Resources Problem | Be killed by a cap-scaled Warden's Sonic Boom. |

---

## The Last Echo

The Last Echo is the Tier-IV endgame route after the normal World Tier conquests.

### Eye of the Last Echo

Recipe:

```text
Eye of Ender | Eye of Ender      | Eye of Ender
Eye of Ender | Condensed Core IV | Eye of Ender
Eye of Ender | Eye of Ender      | Eye of Ender
```

The Eye:

- Is reusable.
- Is fire resistant.
- Only functions after World Tier IV.
- Only locates the Citadel while used in the End.
- Points toward one world Citadel with Sculk Soul particles.
- Reports horizontal distance.
- Has a five-second use cooldown.

The Citadel is selected approximately 5,000–10,000 blocks from the End origin and builds when players approach its wider trigger area.

### Echobound Dragon

The Echobound Dragon is an independent boss and does not replace the Vanilla Ender Dragon fight.

Default health scaling:

| Nearby players | Maximum health |
|---:|---:|
| 1 | 4,000 |
| 2 | 6,000 |
| 3 | 8,000 |
| 4+ | 10,000 cap |

The fight has four health phases.

| Phase | Health range | Echo crystals | Attack-speed trend | Damage multiplier |
|---|---|---:|---|---:|
| I | Above 75% | 8 | Slowest | 1.00× |
| II | 50–75% | 6 | Faster | 1.25× |
| III | 25–50% | 4 | Faster | 1.55× |
| IV | Below 25% | 0 | Fastest | 2.00× |

Crystals heal the boss by 5 health per second while active.

Its attack set includes:

- Sonic attacks
- Multi-projectile volleys
- Wither-skull variants at higher phases
- Dragon-breath dives
- Ground slams
- Ender rain attacks

The arena receives temporary encounter protection while the boss is active.

### Rewards

| Reward | EMC |
|---|---:|
| Heart of the Last Echo | 134,217,728 |
| Echobound Dragon Scale | 4,194,304 each |

Scale drop: 6–10 by default.

### Echobound equipment

Use the Deep Convergence Template in a Smithing Table.

| Result | Base | Addition |
|---|---|---|
| Echobound Helmet | Echoing Helmet | Dragon Scale |
| Echobound Chestplate | Echoing Chestplate | Heart of the Last Echo |
| Echobound Leggings | Echoing Leggings | Dragon Scale |
| Echobound Boots | Echoing Boots | Dragon Scale |
| Echobound Greatsword | Echoing Sword | Heart of the Last Echo |
| Echobound Pickaxe | Echoing Pickaxe | Dragon Scale |

### Echobound armor

| Piece | Defense | Durability |
|---|---:|---:|
| Helmet | 12 | 8,000 |
| Chestplate | 20 | 12,000 |
| Leggings | 16 | 11,000 |
| Boots | 10 | 9,000 |

Other statistics:

- Toughness: 8
- Knockback resistance: 0.25 per piece
- Enchantability: 30
- Full-set damage multiplier: 0.20 after normal armor calculation
- Full-set Creative flight

### Echobound Greatsword

Base characteristics:

- 25 attack damage from its material and sword construction.
- 16,000 durability.
- Fire resistant.

Every successful melee attack begins a one-second 20-hit echo sequence:

- First five echoes: 100 damage each.
- Remaining fifteen echoes: 25 damage each.

Charged Sonic Boom:

- Charge time: 1 second.
- Range: 64 blocks.
- Cost: 4,096 EMC.
- Cooldown: 4 seconds.
- Fully penetrates blocks and earlier targets.
- Damage is delivered as 50 separate pulses over 2.5 seconds.
- First five pulses: 100 damage each.
- Remaining forty-five pulses: 10 damage each.
- Damage area is approximately five times wider than the normal Resonant Echo beam.
- The visual uses nine Sonic Boom lanes to match the true attack width.
- The release sound is the actual Warden Sonic Boom sound, emitted at the wielder with increased volume so the attack remains full-bodied in first person.

This extreme strength is intentional: the Greatsword is a post-final-boss reward.

### Echobound Pickaxe

- Post-boss companion reward to the Echobound Greatsword.
- Mines every breakable block at near-instant speed, including blocks normally associated with other tool classes.
- Bedrock and other blocks with an unbreakable destroy time remain protected.
- This mining privilege belongs to the Echobound Pickaxe itself; installing a Core on an ordinary pickaxe does not grant it.
- Durability: 16,000.
- Fire resistant.
- Repair material: Echobound Dragon Scale.

---

## Controls

All keybinds can be changed in Minecraft Controls under **Velorise EMC**.

| Keybind | Function |
|---|---|
| Open EMC Hub | Opens personal EMC information and server-controlled EMC options. |
| Shift Alchemical Charge | Changes the selected charge or mode on supported equipment. |
| Repair Imbued Equipment | Manually spends EMC to repair the held or supported Core-bound item. |
| Aetheric Elytra Boost | Activates the Imbued Elytra boost while gliding. |
| Activate Aquatic Breathing | Activates the Imbued Turtle Helmet effects. |

Common interactions:

- Right-click Philosopher's Stone on a supported block to transmute it.
- Right-click Transmutation Tablet to open portable transmutation.
- Shift + Right-click an empty Knowledge Sharing Book to capture knowledge.
- Hold right-click with Magic Mirror to channel teleportation.
- Shift-use Magic Mirror for a linked Waypoint Anchor destination.
- Right-click Attraction Catalyst to toggle attraction.
- Use Chalice directly on a fluid or growable block.
- Right-click Void Translocator to teleport forward.
- Mouse wheel while using Far-Sight Glass to change zoom.
- Held portable workstations open their menu by right-clicking in air or inventory use.

---

## Server configuration overview

Server-authoritative settings are stored in `config/velorise_emc/gameplay.json`. Clients receive the normalized gameplay snapshot from the server.

Current configuration schema: **24**.

Major configurable groups:

### Economy

- Tax enabled in Default or Balanced mode
- Tax recovery rate
- Tax brackets and surcharges
- EMC value mode
- Custom item EMC values
- Burn, learn and materialization policies

### Machines

- Hourglass capacity, pulse rate, virtual ticks, cost and radius
- Furnace-family cooking-speed multipliers
- Tidal Conduit capacity, range and operating cost
- Matterburst radius, depth, damage and work rate
- Resonance Jukebox capacity and running cost
- Purification Grindstone operation cost

### Items and Arcane Weapons

- Utility costs, ranges and cooldowns
- Core armor and repair values
- Arcane damage, range, radius, duration, cost and cooldown
- Modded-item compatibility lists and per-item overrides

### Progression

- Every World Tier objective
- Contributor scaling mode and multiplier
- Boss objective scaling
- Maximum counted contributors

### World systems

- Resonance Spire generation and output
- Boss player-count scaling
- World Threat health and damage scaling
- Modded hostile inclusion and exclusions

The values in this README describe defaults, not guarantees on every server.

---

## Frequently asked questions

### Do Transmutation Tables generate EMC?

No. They store, teach, burn and materialize through the player's account. EMC must come from items, world systems or active generators implemented elsewhere.

### Is EMC shared between players?

No. Personal EMC funds and learned-item lists are player-specific. World Tier progression is shared by the world/server.

### Can I use the mod in an existing world?

Yes. Resonant ore retrogen is designed to bless existing chunks when new World Tiers unlock.

### Does Core IV make actions cheaper?

Usually no. Core IV increases power, range, throughput or capacity. The same successful unit of work normally keeps its fixed EMC price.

### Can Arcane Weapons run without EMC?

No. A shot, channel, teleport or powered action fails when the player cannot afford its complete required cost.

### Why are some weapons much stronger than others?

Core-I strength is balanced around the inspired mob, recipe difficulty, availability and weapon mechanic. Higher tiers then strengthen each weapon's identity. A multi-shot or control weapon is not balanced only by its displayed single-hit damage.

### Does Stormcaller use real Vanilla lightning?

It uses controlled lightning behavior. It can charge Creepers, but it does not freely apply every Vanilla lightning transformation or uncontrolled structure fire.

### Do fire weapons cook meat drops?

Yes, where the mob's Vanilla loot supports a cooked variant and the fire attack kills it while burning.

### Does Shulker Launcher home onto enemies?

No. The player aims every bolt manually. Core increases bolt count and control duration.

### Why does Evoker Fang repeat in one place?

The staff records the original five fang positions and repeats those exact positions for later waves. Higher Core tiers add waves, not a longer attack line.

### Is the Echobound Greatsword supposed to be extremely strong?

Yes. It is crafted only after defeating the final Echobound Dragon and is intentionally a post-boss reward.

### Are Condensed Core blocks passive generators?

They generate only for eligible Survival players standing within three blocks, and their fixed output is split between nearby players. They are not unlimited long-range chunk generators.

### Are all modded weapons and armor automatically compatible with Core upgrades?

The mod supports broad damageable-item compatibility, but custom item classes can behave differently. Servers can force-include items, restrict them to repair-only or provide per-item overrides.

---

## Complete content index

### Directly registered special items

- Philosopher's Stone
- Transmutation Tablet
- Tome of Knowledge
- Knowledge Sharing Book
- Wireless Core I–IV
- Condensed Core I–IV
- Infernal Ascension Template
- End Ascension Template
- Deep Convergence Template
- Void Translocator
- Attraction Catalyst
- Chalice of Transmutation
- Magic Mirror
- Cursed Igniter
- Purifying Milk
- Eye of the Last Echo
- Heart of the Last Echo
- Echobound Dragon Scale

### Arcane Weapons

- Inferno Rod
- Ghastfire Staff
- Dragonfire Scepter
- Resonant Echo
- Wither Scepter
- Shulker Launcher
- Guardian Lens
- Evoker Fang Staff
- Stormcaller
- Frostbolt Wand
- Webcaster

### Alchemical fuel

- Alchemical Coal
- Mobius Fuel
- Aeternalis Fuel
- Alchemical Coal Block
- Mobius Fuel Block
- Aeternalis Fuel Block

### Resonant material families

Each of Verdant, Infernal, Aetheric and Echoing includes:

- Ore
- Raw material
- Ingot
- Storage block
- Sword
- Pickaxe
- Axe
- Shovel
- Hoe
- Helmet
- Chestplate
- Leggings
- Boots

Verdant also has a Deepslate ore variant.

### Echobound equipment

- Echobound Helmet
- Echobound Chestplate
- Echobound Leggings
- Echobound Boots
- Echobound Greatsword
- Echobound Pickaxe

### Placeable and machine blocks

- Verdant Resonant Ore
- Deepslate Verdant Resonant Ore
- Infernal Resonant Ore
- Aetheric Resonant Ore
- Echoing Resonant Ore
- Four Resonant storage blocks
- Transmutation Table
- Arcane Transmutation Table
- Three fuel blocks (Alchemical Coal Block, Mobius Fuel Block, Aeternalis Fuel Block)
- Alchemical Hourglass
- Alchemical Anvil I–IV
- Purification Grindstone
- Portable Crafting Table
- Waypoint Anchor
- Matterburst TNT
- Matterburst Minecart
- Core Aura Machine
- Resonant Furnace I–IV
- Infernal Altar, Aetheric Altar, Deepbound Altar & Last Echo Altar

### Utility items & special conversions

- Attraction Catalyst
- Chalice of Transmutation
- Magic Mirror
- Void Translocator
- Cursed Igniter
- Eye of the Last Echo
- Eye of the Crucible
- Aetheric Bulwark Eye
- Deepbound Eye
- Purifying Milk
- Generic Core-bound weapons, tools and armor

### Content intentionally not present

The following older concepts were removed and are not part of current gameplay:

- Alchemical Dust & Alchemical Shears
- Tidal Heart & Bottomless Bucket
- Deepbound Chest, Barrel, Shulker Box & Ender Chest
- Tidal Conduit & Resonance Jukebox
- Separate Resonant Smoker & Resonant Blast Furnace (consolidated into Resonant Furnace I–IV)
- Far-Sight Glass, Bound Compass & Soulbound Recovery Compass
- Imbued Fishing Rod, Imbued Elytra & Imbued Turtle Helmet
- Resonant Beacon & Restorative Hearth
- Revealing Bell & Resonant Smithing Forge
- Verdant Ascension Template
- Passive EMC generation from Transmutation Tables
- Aegis Staff and Shield Staff

---

Velorise EMC is built around a simple rule: **everything may use EMC, but every system should still have a reason to explore, fight, build and progress.**
