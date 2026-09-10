# Dimension transition performance restructure

This source package restructures the work that previously converged when a player changed dimension, especially when entering The End.

## Client EMC calculation

- Dimension change and respawn now sync only player-specific EMC data.
- Global EMC overrides and gameplay configuration are no longer resent on every dimension transition.
- Client-level loading no longer starts an unconditional recipe graph rebuild.
- Server EMC payloads use a content fingerprint so identical payloads are ignored.
- EMCRegistry uses recipe revision, synced-state fingerprint and a re-entrancy guard to deduplicate recalculation.
- A payload received before ClientLevel creation is marked pending and flushed once the level exists.

## Resonant ore retrogen

Per server:

- Maximum 2 chunks per tick.
- 1.25 ms soft time budget per tick.
- Maximum 4,096 queued chunks.
- Chunk keys are deduplicated.
- Only already-loaded chunks are processed with getChunkNow; processing never forces a chunk load.
- Runtime queue state is removed on server shutdown.

## Resonance Spire generation

Per server:

- Chunk-load callbacks enqueue only lightweight coordinates.
- Maximum 2 chunks per tick.
- 0.75 ms soft time budget per tick.
- Maximum 2,048 queued chunks.
- Duplicate chunk jobs are rejected.
- Only already-loaded LevelChunk instances are inspected.
- Runtime queue state is removed on server shutdown.

## Boss arena progression

Normal progression no longer searches and builds every unlocked arena inline.

- At most one structure search is performed every 60 server ticks.
- Searches run only while a player is present in the target dimension.
- Found locations are queued without forcing their distant chunks to load.
- Construction starts only when a player is within 128 blocks and the center chunk is loaded.
- One active construction job is processed at a time.
- Maximum 2 build slices per tick under a 1.5 ms soft time budget.
- Infernal, Aetheric and Deepbound arenas are split into X-column clearing/floor slices and individual decoration slices.
- A paused arena job does not prevent the scheduler from continuing lightweight structure discovery.

Explicit operator placement methods remain immediate by design. The normal World Tier progression path uses the incremental scheduler.

## Last Echo Citadel

- World Tier completion only records the deterministic Citadel location.
- Automatic `placeNow` calls were removed.
- Construction remains proximity-triggered.
- Maximum 32 columns per tick under a 1.25 ms soft time budget.
- Finished-arena maintenance checks remain throttled to every fourth tick.

## Data fixes included

- Removed the empty NeoForge recipes `tidal_turtle_helmet.json` and `resonant_crafting_table.json` that contained only `{}`.
- Removed `velorise_emc:attraction_catalyst` from the NeoForge pickaxe block tag because it is an item, not a block.

## Validation performed

No Gradle task was run.

Static validation performed:

- Parsed 658 JSON resource files successfully.
- Checked balanced Java braces, parentheses and brackets for every changed Java file.
- Ran `javac` syntax diagnostics on every changed Java file and found no parser-level errors.
- Ran 52 source assertions covering dimension sync, recalculation guards, queues, limits, time budgets, incremental arena APIs, removal of automatic `placeNow`, recipe deletion and tag correction.

A real NeoForge/Forge build and in-game profiling are still required by the maintainer because dependency type resolution and runtime timing cannot be proven by static analysis alone.
