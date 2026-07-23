package com.velorise.emc;

import com.velorise.emc.common.AuraNodeProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Deterministic Resonance Spire placement.
 *
 * <p>Tiers I-III use sparse region candidates. Tier IV is associated with an Ancient City, but is placed on a stable
 * surface ring outside the city footprint so it cannot overwrite the city or
 * become buried in the underground structure.</p>
 */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AuraNodeWorldGeneration {
    private static final ResourceLocation ANCIENT_CITY_ID = new ResourceLocation("minecraft", "ancient_city");

    private AuraNodeWorldGeneration() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!AuraNodeProfile.enabled() || !(event.getLevel() instanceof ServerLevel level)) return;
        ChunkPos chunk = event.getChunk().getPos();
        // Chunk callbacks may run before FULL. Delay every read/write to the server thread.
        level.getServer().execute(() -> placeForDimension(level, chunk));
    }

    private static void placeForDimension(ServerLevel level, ChunkPos chunk) {
        if (!level.hasChunk(chunk.x, chunk.z)) return;
        if (Level.OVERWORLD.equals(level.dimension())) {
            tryPlaceRegionalTower(level, chunk, 1);
            tryPlaceAncientCityTower(level, chunk);
        } else if (Level.NETHER.equals(level.dimension())) {
            tryPlaceRegionalTower(level, chunk, 2);
        } else if (Level.END.equals(level.dimension())) {
            tryPlaceRegionalTower(level, chunk, 3);
        }
    }

    private static void tryPlaceRegionalTower(ServerLevel level, ChunkPos chunk, int tier) {
        int spacing = AuraNodeProfile.spacingBlocks(tier);
        int blockX = chunk.getMinBlockX();
        int blockZ = chunk.getMinBlockZ();
        int regionX = Math.floorDiv(blockX, spacing);
        int regionZ = Math.floorDiv(blockZ, spacing);
        long seed = mix(level.getSeed(), regionX, regionZ, tier);
        RandomSource random = RandomSource.create(seed);
        int jitter = Math.max(1, spacing / 8);
        int x = regionX * spacing + spacing / 2 + random.nextInt(jitter * 2 + 1) - jitter;
        int z = regionZ * spacing + spacing / 2 + random.nextInt(jitter * 2 + 1) - jitter;
        x = alignTowerCenter(x);
        z = alignTowerCenter(z);
        if ((x >> 4) != chunk.x || (z >> 4) != chunk.z) return;
        if (hasTowerInColumn(level, x, z)) return;
        if (tier == 1 && (long)x * x + (long)z * z < (long)AuraNodeProfile.spawnExclusionRadius() * AuraNodeProfile.spawnExclusionRadius()) return;
        if (tier == 3 && (long)x * x + (long)z * z < 1_024L * 1_024L) return;

        BlockPos foundation = findRegionalFoundation(level, x, z, tier);
        if (foundation == null) return;
        BlockPos nodePos = foundation.above(AuraNodeTowerStructure.nodeOffsetY(tier));
        if (level.getBlockState(nodePos).is(VeloriseEMC.RESONANCE_SPIRE.get())) return;
        AuraNodeTowerStructure.place(level, foundation, tier, true);
    }

    private static void tryPlaceAncientCityTower(ServerLevel level, ChunkPos loadedChunk) {
        var chunk = level.getChunk(loadedChunk.x, loadedChunk.z);
        for (var entry : chunk.getAllStarts().entrySet()) {
            ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(entry.getKey());
            StructureStart start = entry.getValue();
            if (!ANCIENT_CITY_ID.equals(id) || start == null || !start.isValid()) continue;
            if (!start.getChunkPos().equals(loadedChunk)) continue;
            if (!selectedAncientCity(level, start)) return;

            BlockPos foundation = findAncientCitySurfaceFoundation(level, start);
            if (foundation == null) return;
            if (hasTowerInColumn(level, foundation.getX(), foundation.getZ())) return;
            BlockPos nodePos = foundation.above(AuraNodeTowerStructure.nodeOffsetY(4));
            if (level.getBlockState(nodePos).is(VeloriseEMC.RESONANCE_SPIRE.get())) return;
            AuraNodeTowerStructure.place(level, foundation, 4, true);
            return;
        }
    }


    /**
     * Places Tier IV outside the Ancient City footprint and on the natural
     * Overworld surface above it. The old implementation used the structure
     * start chunk and the city's underground minimum Y, which could erase the
     * city centre or bury the tower inside deepslate.
     */
    private static BlockPos findAncientCitySurfaceFoundation(ServerLevel level, StructureStart start) {
        var box = start.getBoundingBox();
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        int halfWidth = Math.max(16, (box.maxX() - box.minX() + 1) / 2);
        int halfDepth = Math.max(16, (box.maxZ() - box.minZ() + 1) / 2);
        int towerRadius = AuraNodeTowerStructure.radiusForTier(4);
        int cityClearance = towerRadius + 40;
        int[] extraRings = {48, 72, 96, 128};
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
        };

        int rotation = Math.floorMod((int)mix(level.getSeed(), start.getChunkPos().x, start.getChunkPos().z, 4), directions.length);
        for (int ring : extraRings) {
            for (int index = 0; index < directions.length; index++) {
                int[] direction = directions[(index + rotation) % directions.length];
                double length = Math.sqrt((double)direction[0] * direction[0] + (double)direction[1] * direction[1]);
                int distanceX = halfWidth + cityClearance + ring;
                int distanceZ = halfDepth + cityClearance + ring;
                int x = alignTowerCenter(centerX + (int)Math.round(direction[0] / length * distanceX));
                int z = alignTowerCenter(centerZ + (int)Math.round(direction[1] / length * distanceZ));
                if (insideExpandedCity(box, x, z, cityClearance)) continue;

                BlockPos foundation = findStableSurfaceFoundation(level, x, z, towerRadius);
                if (foundation != null) return foundation;
            }
        }
        return null;
    }

    private static boolean insideExpandedCity(net.minecraft.world.level.levelgen.structure.BoundingBox box,
                                               int x, int z, int clearance) {
        return x >= box.minX() - clearance && x <= box.maxX() + clearance
            && z >= box.minZ() - clearance && z <= box.maxZ() + clearance;
    }

    /** Requires a dry, exposed and reasonably level footprint. */
    private static BlockPos findStableSurfaceFoundation(ServerLevel level, int x, int z, int radius) {
        int airY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int maxFoundationY = level.getMaxBuildHeight() - AuraNodeTowerStructure.heightForTier(4) - 2;
        if (airY <= level.getSeaLevel() - 8 || airY - 1 > maxFoundationY) return null;

        BlockPos centerAir = new BlockPos(x, airY, z);
        BlockPos centerGround = centerAir.below();
        if (!level.getFluidState(centerAir).isEmpty() || !level.getFluidState(centerGround).isEmpty()) return null;
        if (!level.getBlockState(centerGround).isFaceSturdy(level, centerGround, Direction.UP)) return null;
        if (!level.canSeeSky(centerAir)) return null;

        int minY = airY;
        int maxY = airY;
        int sampleRadius = radius + 2;
        int step = Math.max(3, sampleRadius / 2);
        for (int dx = -sampleRadius; dx <= sampleRadius; dx += step) {
            for (int dz = -sampleRadius; dz <= sampleRadius; dz += step) {
                int sampleAirY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                BlockPos sampleAir = new BlockPos(x + dx, sampleAirY, z + dz);
                BlockPos sampleGround = sampleAir.below();
                if (!level.getFluidState(sampleAir).isEmpty() || !level.getFluidState(sampleGround).isEmpty()) return null;
                if (!level.getBlockState(sampleGround).isFaceSturdy(level, sampleGround, Direction.UP)) return null;
                minY = Math.min(minY, sampleAirY);
                maxY = Math.max(maxY, sampleAirY);
                if (maxY - minY > 4) return null;
            }
        }
        return centerGround;
    }

    private static boolean selectedAncientCity(ServerLevel level, StructureStart start) {
        double chance = AuraNodeProfile.ancientCitySpawnChance();
        if (chance <= 0.0D) return false;
        if (chance >= 1.0D) return true;
        ChunkPos startChunk = start.getChunkPos();
        long mixed = mix(level.getSeed(), startChunk.x, startChunk.z, 4);
        double roll = (mixed >>> 11) * 0x1.0p-53;
        return roll < chance;
    }

    private static BlockPos findRegionalFoundation(ServerLevel level, int x, int z, int tier) {
        if (tier == 1 || tier == 3) {
            int airY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int maxFoundationY = level.getMaxBuildHeight() - AuraNodeTowerStructure.heightForTier(tier) - 2;
            if (airY <= level.getMinBuildHeight() + 4 || airY - 1 > maxFoundationY) return null;
            BlockPos air = new BlockPos(x, airY, z);
            if (!level.getFluidState(air).isEmpty() || !level.getFluidState(air.above()).isEmpty()) return null;
            BlockPos foundation = air.below();
            if (!level.getBlockState(foundation).isFaceSturdy(level, foundation, Direction.UP)) return null;
            return foundation;
        }

        int min = Math.max(level.getMinBuildHeight() + 8, 24);
        int max = Math.min(level.getMaxBuildHeight() - AuraNodeTowerStructure.heightForTier(tier) - 3, 112);
        if (max <= min) return null;
        int start = min + Math.floorMod((int)mix(level.getSeed(), x, z, tier), Math.max(1, max - min));
        for (int offset = 0; offset < max - min; offset++) {
            int airY = min + Math.floorMod(start - min + offset, max - min);
            BlockPos air = new BlockPos(x, airY, z);
            BlockPos foundation = air.below();
            if (level.getBlockState(air).isAir()
                && level.getBlockState(foundation).isFaceSturdy(level, foundation, Direction.UP)) {
                return foundation;
            }
        }
        return null;
    }

    private static int alignTowerCenter(int coordinate) {
        return Math.floorDiv(coordinate, 16) * 16 + 8;
    }

    /** Prevents chunk reloads from treating the roof of an existing tower as a new foundation. */
    private static boolean hasTowerInColumn(ServerLevel level, int x, int z) {
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(VeloriseEMC.RESONANCE_SPIRE.get())) return true;
        }
        return false;
    }

    private static long mix(long seed, int x, int z, int tier) {
        long value = seed ^ (long)x * 341873128712L ^ (long)z * 132897987541L ^ (long)tier * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27; value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
