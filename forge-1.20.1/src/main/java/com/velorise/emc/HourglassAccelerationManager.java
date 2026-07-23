package com.velorise.emc;

import com.velorise.emc.common.EmcMath;
import com.velorise.emc.common.HourglassProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Event-driven target cache for Alchemical Hourglasses.
 *
 * <p>Each loaded Hourglass scans only when first used or when a nearby block topology event marks
 * its cache dirty. Normal pulses address cached positions directly. A target can have only one
 * owner, selected deterministically by distance and then source position.</p>
 */
public final class HourglassAccelerationManager {
    private static final Map<ServerLevel, LevelState> LEVELS = new WeakHashMap<>();

    private static final Field FURNACE_PROGRESS = findField(AbstractFurnaceBlockEntity.class,
        "cookingProgress", "f_58318_");
    private static final Field FURNACE_TOTAL = findField(AbstractFurnaceBlockEntity.class,
        "cookingTotalTime", "f_58319_");
    private static final Field BREW_TIME = findField(BrewingStandBlockEntity.class,
        "brewTime", "f_59007_");

    private HourglassAccelerationManager() {}

    public static PulseResult processPulse(ServerLevel level, AlchemicalHourglassBlockEntity source) {
        LevelState state = LEVELS.computeIfAbsent(level, ignored -> new LevelState());
        long sourceKey = source.getBlockPos().asLong();
        SourceCache cache = state.sources.computeIfAbsent(sourceKey, ignored -> new SourceCache());

        if (cache.dirty || !cache.initialized) rebuildCache(level, source, state, cache);
        boolean lostTarget = removeInvalidTargets(level, source, state, cache);
        if (lostTarget) rebuildCache(level, source, state, cache);

        int targetCount = cache.targets.size();
        if (targetCount == 0 || !source.canAccelerate()) {
            source.finishPulse(0, 0L, targetCount);
            return new PulseResult(0, 0L, targetCount);
        }

        long costPerTick = HourglassProfile.emcPerSuccessfulTick();
        long affordable = costPerTick <= 0L ? Long.MAX_VALUE : source.getStoredEmc() / costPerTick;
        if (affordable <= 0L) {
            source.finishPulse(0, 0L, targetCount);
            return new PulseResult(0, 0L, targetCount);
        }

        int successful = 0;
        int perTarget = HourglassProfile.bonusTicksPerPulse();
        int start = Math.floorMod(cache.cursor, targetCount);

        for (int offset = 0; offset < targetCount && successful < affordable; offset++) {
            int index = (start + offset) % targetCount;
            BlockPos targetPos = BlockPos.of(cache.targets.get(index));
            int targetSuccess = 0;
            for (int tick = 0; tick < perTarget && successful < affordable; tick++) {
                if (!accelerateOneTick(level, targetPos)) break;
                successful++;
                targetSuccess++;
            }
            if (targetSuccess > 0 && level.random.nextFloat() < 0.08F) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CLOCK)),
                    targetPos.getX() + 0.5D, targetPos.getY() + 1.1D, targetPos.getZ() + 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        cache.cursor = targetCount == 0 ? 0 : (start + 1) % targetCount;
        long totalCost = costPerTick <= 0L ? 0L : EmcMath.saturatingMultiply(successful, costPerTick, Long.MAX_VALUE);
        source.finishPulse(successful, totalCost, targetCount);
        return new PulseResult(successful, totalCost, targetCount);
    }

    public static void requestRefresh(ServerLevel level, BlockPos sourcePos) {
        LevelState state = LEVELS.computeIfAbsent(level, ignored -> new LevelState());
        state.sources.computeIfAbsent(sourcePos.asLong(), ignored -> new SourceCache()).dirty = true;
    }

    /** Called by block place/break events. Iterates only loaded Hourglass positions, never the world. */
    public static void markDirtyAround(ServerLevel level, BlockPos changedPos) {
        LevelState state = LEVELS.get(level);
        if (state == null || state.sources.isEmpty()) return;
        Iterator<Map.Entry<Long, SourceCache>> iterator = state.sources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, SourceCache> entry = iterator.next();
            BlockPos sourcePos = BlockPos.of(entry.getKey());
            BlockEntity blockEntity = level.getBlockEntity(sourcePos);
            if (!(blockEntity instanceof AlchemicalHourglassBlockEntity source) || source.isRemoved()) {
                releaseOwnedTargets(state, entry.getKey(), entry.getValue());
                iterator.remove();
                continue;
            }
            long radius = HourglassProfile.radius(source.getInstalledCoreLevel());
            if (distanceSquared(sourcePos, changedPos) <= radius * radius) entry.getValue().dirty = true;
        }
    }

    public static int getOwnedTargetCount(ServerLevel level, BlockPos sourcePos) {
        LevelState state = LEVELS.get(level);
        if (state == null) return 0;
        SourceCache cache = state.sources.get(sourcePos.asLong());
        return cache == null ? 0 : cache.targets.size();
    }

    public static void releaseSource(ServerLevel level, BlockPos sourcePos) {
        LevelState state = LEVELS.get(level);
        if (state == null) return;
        long sourceKey = sourcePos.asLong();
        SourceCache cache = state.sources.remove(sourceKey);
        if (cache != null) releaseOwnedTargets(state, sourceKey, cache);
    }

    private static boolean removeInvalidTargets(ServerLevel level, AlchemicalHourglassBlockEntity source,
                                                LevelState state, SourceCache cache) {
        boolean removed = false;
        long sourceKey = source.getBlockPos().asLong();
        long maximumDistance = (long) HourglassProfile.radius(source.getInstalledCoreLevel())
            * HourglassProfile.radius(source.getInstalledCoreLevel());
        Iterator<Long> iterator = cache.targets.iterator();
        while (iterator.hasNext()) {
            long targetKey = iterator.next();
            BlockPos targetPos = BlockPos.of(targetKey);
            if (distanceSquared(source.getBlockPos(), targetPos) > maximumDistance
                || !level.hasChunkAt(targetPos)
                || !isEligibleTarget(level, targetPos)) {
                iterator.remove();
                state.ownerByTarget.remove(targetKey, sourceKey);
                removed = true;
            }
        }
        if (removed) cache.dirty = true;
        return removed;
    }

    private static void rebuildCache(ServerLevel level, AlchemicalHourglassBlockEntity source,
                                     LevelState state, SourceCache cache) {
        long sourceKey = source.getBlockPos().asLong();
        releaseOwnedTargets(state, sourceKey, cache);
        cache.targets.clear();
        cache.initialized = true;
        cache.dirty = false;
        cache.cursor = 0;

        int radius = HourglassProfile.radius(source.getInstalledCoreLevel());
        long maximumDistance = (long) radius * radius;
        BlockPos origin = source.getBlockPos();
        List<Candidate> candidates = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos targetPos = origin.offset(dx, dy, dz);
                    long distance = distanceSquared(origin, targetPos);
                    if (distance > maximumDistance || !level.hasChunkAt(targetPos)) continue;
                    if (!isEligibleTarget(level, targetPos)) continue;
                    candidates.add(new Candidate(targetPos.asLong(), distance));
                }
            }
        }

        candidates.sort(Comparator.comparingLong(Candidate::distance).thenComparingLong(Candidate::targetPos));
        for (Candidate candidate : candidates) {
            long currentOwner = state.ownerByTarget.getOrDefault(candidate.targetPos, Long.MIN_VALUE);
            if (currentOwner != Long.MIN_VALUE && currentOwner != sourceKey) {
                if (!shouldReplaceOwner(level, candidate.targetPos, sourceKey, currentOwner)) continue;
                SourceCache oldCache = state.sources.get(currentOwner);
                if (oldCache != null) {
                    oldCache.targets.remove(candidate.targetPos);
                    oldCache.dirty = true;
                }
            }
            state.ownerByTarget.put(candidate.targetPos, sourceKey);
            cache.targets.add(candidate.targetPos);
        }
    }

    private static boolean shouldReplaceOwner(ServerLevel level, long targetKey, long challenger, long current) {
        BlockPos target = BlockPos.of(targetKey);
        BlockPos challengerPos = BlockPos.of(challenger);
        BlockPos currentPos = BlockPos.of(current);
        BlockEntity currentEntity = level.getBlockEntity(currentPos);
        if (!(currentEntity instanceof AlchemicalHourglassBlockEntity) || currentEntity.isRemoved()) return true;
        long challengerDistance = distanceSquared(challengerPos, target);
        long currentDistance = distanceSquared(currentPos, target);
        return challengerDistance < currentDistance
            || (challengerDistance == currentDistance && challenger < current);
    }

    private static void releaseOwnedTargets(LevelState state, long sourceKey, SourceCache cache) {
        for (long targetKey : cache.targets) state.ownerByTarget.remove(targetKey, sourceKey);
    }

    private static boolean isEligibleTarget(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ResonantMachine) return false;
        return entity instanceof AbstractFurnaceBlockEntity || entity instanceof BrewingStandBlockEntity;
    }

    private static boolean accelerateOneTick(ServerLevel level, BlockPos pos) {
        BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof ResonantMachine) return false;
        if (target instanceof AbstractFurnaceBlockEntity furnace) return accelerateFurnace(level, pos, furnace);
        if (target instanceof BrewingStandBlockEntity brewingStand) return accelerateBrewing(level, pos, brewingStand);
        return false;
    }

    private static boolean accelerateFurnace(ServerLevel level, BlockPos pos, AbstractFurnaceBlockEntity furnace) {
        if (FURNACE_PROGRESS == null || FURNACE_TOTAL == null) return false;
        try {
            int beforeProgress = FURNACE_PROGRESS.getInt(furnace);
            int total = FURNACE_TOTAL.getInt(furnace);
            if (total <= 0 || beforeProgress < 0 || furnace.getItem(0).isEmpty()) return false;
            ItemStack beforeInput = furnace.getItem(0).copy();
            ItemStack beforeOutput = furnace.getItem(2).copy();
            BlockState state = level.getBlockState(pos);
            AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
            int afterProgress = FURNACE_PROGRESS.getInt(furnace);
            return afterProgress != beforeProgress
                || !ItemStack.matches(beforeInput, furnace.getItem(0))
                || !ItemStack.matches(beforeOutput, furnace.getItem(2));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean accelerateBrewing(ServerLevel level, BlockPos pos, BrewingStandBlockEntity brewingStand) {
        if (BREW_TIME == null) return false;
        try {
            int beforeTime = BREW_TIME.getInt(brewingStand);
            if (beforeTime <= 0) return false;
            List<ItemStack> before = new ArrayList<>(brewingStand.getContainerSize());
            for (int slot = 0; slot < brewingStand.getContainerSize(); slot++) before.add(brewingStand.getItem(slot).copy());
            BrewingStandBlockEntity.serverTick(level, pos, level.getBlockState(pos), brewingStand);
            int afterTime = BREW_TIME.getInt(brewingStand);
            if (afterTime != beforeTime) return true;
            for (int slot = 0; slot < before.size(); slot++) {
                if (!ItemStack.matches(before.get(slot), brewingStand.getItem(slot))) return true;
            }
            return false;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Field findField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        System.err.println("[VeloriseEMC] Hourglass could not access a required field on " + owner.getSimpleName());
        return null;
    }

    private static long distanceSquared(BlockPos first, BlockPos second) {
        long dx = first.getX() - second.getX();
        long dy = first.getY() - second.getY();
        long dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public record PulseResult(int successfulTicks, long emcCost, int targetCount) {}

    private static final class LevelState {
        private final Map<Long, SourceCache> sources = new HashMap<>();
        private final Map<Long, Long> ownerByTarget = new HashMap<>();
    }

    private static final class SourceCache {
        private final List<Long> targets = new ArrayList<>();
        private boolean initialized;
        private boolean dirty = true;
        private int cursor;
    }

    private record Candidate(long targetPos, long distance) {}
}
