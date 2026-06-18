# Simple EMC

A lightweight, high-performance Minecraft mod built for NeoForge 1.21.1. This mod is designed to provide the core gameplay mechanics of item-value transmutation (EMC) without the bloated, over-powered (OP) endgame armor, weapons, or tools found in other mods like ProjectE. It focuses purely on convenience, automation, and basic alchemical machinery.

## Features

### Core Transmutation
- EMC System: Almost all vanilla items and blocks have custom EMC (Energy Matter Covalence) values.
- Transmutation Table: Store items to convert them into EMC, and retrieve any item you have previously learned.
- Transmutation Tablet: A portable, pocket-sized version of the Transmutation Table for transmutation on the go.
- Arcane Transmutation Table: An advanced tier table featuring double inventory capacity and a built-in crafting grid for ultimate convenience.

### Alchemical Tools
- Philosopher's Stone: The primary catalyst. Right-click blocks in the world to transmute them (e.g., Cobblestone to Stone, Sand to Grass), or use it as a reusable crafting ingredient.
- Tome of Knowledge: A creative/dev-only item that instantly teaches the player all registered EMC items when burned in a table.
- Knowledge Sharing Book: Shift+Right-click to capture your current learned items, then share it with other players or burn it in a table to merge knowledge.

### Alchemical Machinery & Catalysts
- Alchemical Hourglass: Accelerates random ticks for adjacent blocks (like crops) and ticks for adjacent block entities (like furnaces or spawners). Automatically consumes alchemical fuels from nearby players' inventories.
- Attraction Catalyst: A toggleable inventory item that pulls nearby dropped items directly to you. Consumes alchemical fuels while active.
- Chalice of Transmutation: A versatile tool with two modes: voiding liquid blocks in the world, or accelerating crop growth in a small area. Consumes alchemical fuels.
- Alchemical Fuels: Three tiers of compressed fuels used to power alchemical machines:
  1. Alchemical Coal (8x efficiency of normal coal)
  2. Mobius Fuel (8x efficiency of Alchemical Coal)
  3. Aeternalis Fuel (8x efficiency of Mobius Fuel)

## Custom Items & Blocks Added

### Core Transmutation Items
- Philosophers Stone - Primary catalyst for transmutation
- Transmutation Tablet - Portable transmutation table
- Knowledge Sharing Book - Share learned EMC with other players
- Tome of Knowledge - Creative-only: teaches all registered EMC items
- Wireless Core - Utility component for advanced machinery
- Magic Mirror - Special item (stackable: 1)

### Alchemical Utility Items
- Attraction Catalyst - Pull nearby dropped items to inventory
- Chalice of Transmutation - Dual-mode: void liquids or accelerate crops

### Alchemical Fuels (3 Tiers)
1. Alchemical Coal (EMC: 1024)
   - Crafted from regular coal + alchemical essence
   - 8x efficiency of normal coal

2. Mobius Fuel (EMC: 8192)
   - Compressed alchemical coal
   - 8x efficiency of Alchemical Coal

3. Aeternalis Fuel (EMC: 65536)
   - Ultimate fuel tier
   - 8x efficiency of Mobius Fuel

### Alchemical Fuel Blocks (Compressed Storage)
- Alchemical Coal Block (EMC: 9216) - Stores 9 Alchemical Coal
- Mobius Fuel Block (EMC: 73728) - Stores 9 Mobius Fuel
- Aeternalis Fuel Block (EMC: 589824) - Stores 9 Aeternalis Fuel

### Transmutation Blocks
- Transmutation Table - Primary block for learning and transmuting items
- Arcane Transmutation Table - Advanced tier with 2x inventory + built-in crafting grid

### Specialized Machinery
- Alchemical Hourglass - Accelerates random ticks for crops and block entities (furnaces, spawners, etc.)

## Compatibility & Mod Integration

### Optional Mod Support
Simple EMC is compatible with and supports several popular mods:

- Jade - Shows EMC item information when hovering over transmutation tables and items
- JEI (Just Enough Items) - Displays EMC values and transmutation recipes in the recipe viewer

These mods are completely optional but provide enhanced user experience for item lookup and recipe discovery.

## Building from Source

To compile the mod yourself, make sure you have Java 21 installed, then run the following command in the project root:

```powershell
.\gradlew build
```

The compiled JAR file will be outputted to:
../mods/simpleemc-1.21.1-neoforge-1.0.1.jar

## Credits
Inspired by the classic EMC mechanics of Equivalent Exchange 2 (by x3n0ph0b3) and ProjectE (by SinKillerJ). Built entirely from scratch.
