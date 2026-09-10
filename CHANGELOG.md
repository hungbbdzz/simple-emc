# Changelog

All notable changes to Velorise EMC will be documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [2.0.1] - 2026-09-10

### Fixed
- **Server crash fix:** The Last Echo Citadel no longer attempts to place its entire structure inline when the world reaches the required tier. Construction is now proximity-triggered and incrementally budgeted, preventing the freeze or crash that occurred on tier unlock.
- EMC recalculation is now deferred until the client level and connection are fully ready, preventing a potential crash on initial server join.
- Duplicate EMC sync packets with identical content are now skipped via fingerprint check, reducing redundant recalculation work.
- Client server-sync state (EMC fingerprint, pending recalculation flag) is now fully cleared on logout.

### Changed
- `tidal_conduit` removed from pickaxe mineable tag (item rework in progress).
- Tidal Conduit advancement now references `attraction_catalyst`.

---

## [2.0.0] - 2026-07-XX

### Added
- Universal mod EMC fallbacks — items from other mods now receive automatic EMC values.
- 128-iteration DAG solver for more accurate recipe-chain EMC propagation.
- Alchemical Anvil recipe consolidation — unified upgrade path for all Core tiers.
- Resonant equipment families: Verdant, Infernal, Aetheric, Echoing.
- Echobound endgame equipment and the Last Echo Citadel encounter.
- Arcane weapon arsenal: Inferno Rod, Ghastfire Staff, Dragonfire Scepter, Resonant Echo, Wither Scepter, Shulker Seeker, Guardian Lens, Evoker Fang Staff, Stormcaller, Frostbolt Wand, Webcaster.
- Resonant Furnace family (Furnace, Smoker, Blast Furnace) with Core I–IV speed upgrades.
- Machines: Alchemical Hourglass, Purification Grindstone, Resonance Jukebox, Matterburst TNT.
- Utility items: Magic Mirror, Void Translocator, Attraction Catalyst, Chalice of Transmutation, Far-Sight Glass, Bound Compass, Soulbound Recovery Compass, Purifying Milk.
- World Tier progression system (4 shared tiers) with Wireless Core I–IV unlocks.
- Balanced Mode with configurable materialization tax.
- Server configuration for EMC values, learn/burn/materialize policies, cave mode and mob equipment scaling.
- Dual-loader support: Forge 1.20.1 and NeoForge 1.21.1 from a single shared codebase.
- MIT License.

### Changed
- UI cleanups across Transmutation, Waypoint and config screens.

---
