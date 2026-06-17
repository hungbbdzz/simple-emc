# Simple EMC

A lightweight, high-performance Minecraft mod built for **NeoForge 1.21.1**. This mod is designed to provide the core gameplay mechanics of item-value transmutation (EMC) without the bloated, over-powered (OP) endgame armor, weapons, or tools found in other mods like ProjectE. It focuses purely on convenience, automation, and basic alchemical machinery.

## Features

### 🌟 Core Transmutation
* **EMC System**: Almost all vanilla items and blocks have custom EMC (Energy Matter Covalence) values.
* **Transmutation Table**: Store items to convert them into EMC, and retrieve any item you have previously learned.
* **Transmutation Tablet**: A portable, pocket-sized version of the Transmutation Table for transmutation on the go.
* **Arcane Transmutation Table**: An advanced tier table featuring double inventory capacity and a built-in crafting grid for ultimate convenience.

### 🧪 Alchemical Tools
* **Philosopher's Stone**: The primary catalyst. Right-click blocks in the world to transmute them (e.g., Cobblestone to Stone, Sand to Grass), or use it as a reusable crafting ingredient.
* **Tome of Knowledge**: A creative/dev-only item that instantly teaches the player all registered EMC items when burned in a table.
* **Knowledge Sharing Book**: Shift+Right-click to capture your current learned items, then share it with other players or burn it in a table to merge knowledge.

### ⚙️ Alchemical Machinery & Catalysts
* **Alchemical Hourglass**: Accelerates random ticks for adjacent blocks (like crops) and ticks for adjacent block entities (like furnaces or spawners). Automatically consumes alchemical fuels from nearby players' inventories.
* **Attraction Catalyst**: A toggleable inventory item that pulls nearby dropped items directly to you. Consumes alchemical fuels while active.
* **Chalice of Transmutation**: A versatile tool with two modes: voiding liquid blocks in the world, or accelerating crop growth in a small area. Consumes alchemical fuels.
* **Alchemical Fuels**: Three tiers of compressed fuels used to power alchemical machines:
  1. **Alchemical Coal** (8x efficiency of normal coal)
  2. **Mobius Fuel** (8x efficiency of Alchemical Coal)
  3. **Aeternalis Fuel** (8x efficiency of Mobius Fuel)

## Building from Source

To compile the mod yourself, make sure you have Java 21 installed, then run the following command in the project root:

```powershell
.\gradlew build
```

The compiled JAR file will be outputted to:
`../mods/simpleemc-1.21.1-neoforge-1.0.0.jar`

## Credits
Inspired by the classic EMC mechanics of **Equivalent Exchange 2** (by x3n0ph0b3) and **ProjectE** (by SinKillerJ). Built entirely from scratch.
