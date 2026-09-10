# Forge 1.20.1 world-load fix

## Diagnosis

The July 24 logs contain two different failures:

1. The 12:02 run failed during mod construction because `VanillaReworkEvents.clearStaleAnvilState` was a private `@SubscribeEvent` method. The current source no longer registers that private helper directly.
2. The later run started the integrated server, reached `Preparing start region for dimension minecraft:overworld`, then stopped at 0-4% spawn preparation without an exception. This matches the Forge-only `AuraNodeWorldGeneration` chunk-load callback re-requesting the same chunk while its load future was still completing.

## Changes

- `AuraNodeWorldGeneration` now queues chunk positions from `ChunkEvent.Load` and processes at most two already-loaded chunks per server tick.
- The Ancient City scan uses the supplied `LevelChunk`; it no longer calls `ServerLevel#getChunk` from the chunk-load path.
- Added queue cleanup on server shutdown.
- Removed the invalid `attraction_catalyst` entry from the pickaxe mining tag.
- Removed two retired placeholder recipe JSON files that Forge 1.20.1 parsed as malformed recipes:
  - `tidal_turtle_helmet.json`
  - `resonant_crafting_table.json`

## Static checks completed

- No private method remains directly annotated with `@SubscribeEvent` in the Forge Java source.
- `AuraNodeWorldGeneration` contains no blocking `level.getChunk(...)` call.
- All 128 remaining Forge recipe JSON files contain a string recipe `type`.
- The patched Java file has balanced braces.

## Build and test

The uploaded source did not contain `gradle/wrapper/gradle-wrapper.jar`, so a binary Forge JAR could not be compiled in this environment. Restore or regenerate the Gradle 8.10.2 wrapper JAR, use JDK 17, then run:

```powershell
.\gradlew.bat :forge-1.20.1:clean :forge-1.20.1:build
```

Test with a newly created world. Delete or rename the partially generated `New World` directory before retesting so the result is not affected by the interrupted first generation.

A Windows helper is included at `RESTORE_WRAPPER_AND_BUILD_FORGE.ps1`. It restores the wrapper from the pinned Gradle 8.10.2 repository file, verifies the official SHA-256 checksum, requires JDK 17, and builds only Forge 1.20.1.
