# Changelog

All notable changes to Velorise EMC will be documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [2.0.2] - 2026-10-25

### Added
- **Boss Visual Overhaul & Dynamic Client Animations:**
  - Implemented dynamic flight tilt, hover bobbing, cast pulsing, and dash trails in `InfernalCrucibleRenderer`.
  - Added dynamic shell pulsing, slam animations, and teleport bursts in `AethericBulwarkRenderer`.
  - Added banking angle interpolation, turning factor, roar pulse, and beam charging animations in `EchoboundDragonRenderer` and `EchoboundDragonModel`.
  - Handled unloaded chunks gracefully in `ArenaSummonService` during rituals and expanded living boss detection to 96m to prevent Eye item waste.
  - Enhanced arena positioning, structure location commands, and ritual progression in `PromotionBossArenaService` and `LastEchoCitadelService`.
- **Autonomous Arena Wandering & Retaliation AI (All 4 Bosses):**
  - All four bosses (`InfernalCrucibleEntity`, `AethericBulwarkEntity`, `DeepboundWardenEntity`, `EchoboundDragonEntity`) now actively wander, orbit, and patrol their arenas when no survival players are present, instead of remaining idle or freezing.
  - Added revenge/retaliation targeting: when any mob or entity attacks a boss (e.g. Iron Golems, skeletons, Withers, or other mod bosses), the boss immediately retaliates, targets the attacker, and executes combat skills against them.
  - Minions spawned by bosses remain protected from friendly-fire targeting.
  - Prevented Vanilla Warden inactivity dig-despawn while bound to an arena, keeping the Warden patrolling and responsive.
  - Generalized all skill targeting and damage areas to support any `LivingEntity` combatant while preserving survival player targeting priority.

### Fixed
- **Vanilla 1024 Max HP Cap:** Implemented Virtual Encounter Health Pools for `InfernalCrucibleEntity` (1,800 HP), `AethericBulwarkEntity` (2,200 HP), and `DeepboundWardenEntity` (3,600 HP) mapped over 1000-HP physical health segments, bypassing Vanilla's 1024 max health attribute ceiling.
- **Admin `/kill` & Invulnerability Bypasses:** Allowed damage types tagged with `DamageTypeTags.BYPASSES_INVULNERABILITY` (such as `/kill` or void damage) to immediately kill all four boss entities, bypassing crystal shields, arena bounds, and segment caps.
- **Phantom AI Conflict (Echobound Dragon):** Overrode `registerGoals()` and `customServerAiStep()` to decouple from Vanilla `PhantomMoveControl` which was pulling the dragon toward `(0, 0, 0)` in outer End coordinates; added a dedicated 3D arena flight controller with smooth pitch/yaw interpolation.
- **Citadel Respawn Fountain Loop:** Fixed `LastEchoCitadelService.ensureRespawnFountain` checking `center.above(3)` instead of `above(6)`, eliminating the 1,075-block regeneration every 4 ticks and preserving the Exit Portal and Dragon Egg after defeat.
- **Vanilla Warden Brain Conflict:** Overrode `customServerAiStep()` in `DeepboundWardenEntity` to isolate custom skill states from Vanilla Warden Brain routines; actively updated anger levels via `increaseAngerAt()` to synchronize heartbeat audio and glowing tendril visuals.
- **Infernal Crucible Movement & Skills:** Refactored `infernalDash` into a multi-tick charge with continuous hitbox collision; reactivated `rapidFireballBarrage`; and introduced a 28-tick telegraphed ground particle warning for `lavaGeyser`.
- **Aetheric Bulwark Strike Timing & Floor Snapping:** Implemented `PendingStrike` queue respecting `visualTicks` telegraphed rings before detonation; added floor snapping for `spatialSwap` so the boss never teleports into open air; rebalanced `ironShellGuard` to Resistance III with dedicated cooldown.
- **Target Filtering & Persistence:** Filtered out Creative and Spectator players across all bosses; added persistence flags (`removeWhenFarAway(false)`, `shouldDespawnInPeaceful(false)`) and weak reference server caching in `ArenaSummonService` to avoid leaks during world unload/reloads.
- **Arena Exploits & Minion Limits:** Capped active minions (6 for Infernal Minions, 4 for Aetheric Shulkerlings) and automatically purged them upon boss death; excluded encounter bosses and minions from `ThreatScalingEvents`, `ArcaneMobEvents`, and `WorldProgressionEvents`.
- **Altar Protection & Summon Eye:** Added `isBulwarkAltarProtected` checks to prevent altar destruction and fixed `PromotionBossEyeItem` on NeoForge 1.21.1 to correctly activate anywhere on the altar structure.

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
