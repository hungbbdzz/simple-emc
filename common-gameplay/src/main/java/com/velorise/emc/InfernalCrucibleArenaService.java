package com.velorise.emc;

import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;

/** Tier-I unlocked Nether fortress-adjacent arena for The Infernal Crucible. */
public final class InfernalCrucibleArenaService {
    public static final int ARENA_RADIUS = 34;
    private InfernalCrucibleArenaService() {}

    public static BlockPos locateArena(ServerLevel level, BlockPos origin) {
        ResourceLocation id = ResourceLocation.tryParse("minecraft:fortress");
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        java.util.Optional<Holder.Reference<Structure>> holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id));
        if (holder.isEmpty()) return null;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(holder.get()), origin, 256, false);
        if (found == null) return null;
        BlockPos fortress = found.getFirst();
        return new BlockPos(fortress.getX() + 56, Math.max(64, fortress.getY() + 8), fortress.getZ());
    }

    public static boolean hasArena(ServerLevel level, BlockPos center) {
        return level.getBlockState(center.above(2)).is(VeloriseEMC.INFERNAL_ALTAR.get());
    }

    public static boolean isAltar(BlockPos clicked, BlockPos center) {
        int dx = Math.abs(clicked.getX() - center.getX());
        int dz = Math.abs(clicked.getZ() - center.getZ());
        int dy = clicked.getY() - center.getY();
        return dx <= 5 && dz <= 5 && dy >= -1 && dy <= 4;
    }

    public static BlockPos findAltarCenter(ServerLevel level, BlockPos clicked) {
        for (int y = -4; y <= 2; y++) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos candidate = clicked.offset(x, y, z);
                    if (hasArena(level, candidate) && isAltar(clicked, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isInfernalAltarProtected(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(VeloriseEMC.INFERNAL_ALTAR.get())) return false;
        BlockPos center = findAltarCenter(level, pos);
        return center != null && isAltar(pos, center);
    }

    public static void buildArena(ServerLevel level, BlockPos center) {
        for (int x = incrementalMinX(); x <= incrementalMaxX(); x++) buildArenaColumn(level, center, x);
        for (int step = 0; step < incrementalDecorationCount(); step++) buildArenaDecoration(level, center, step);
    }

    static int incrementalMinX() { return -40; }
    static int incrementalMaxX() { return 40; }
    static int incrementalDecorationCount() { return 95; }

    /** Builds one X slice of the expensive clear/floor phase. */
    static void buildArenaColumn(ServerLevel level, BlockPos center, int x) {
        int clearRadius = 40;
        if (x >= -clearRadius && x <= clearRadius) {
            for (int z = -clearRadius; z <= clearRadius; z++) {
                if (x * x + z * z > clearRadius * clearRadius) continue;
                for (int y = -5; y <= 30; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        int radius = ARENA_RADIUS;
        if (Math.abs(x) > radius) return;
        for (int z = -radius; z <= radius; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > radius + 0.25D) continue;
            BlockPos floor = center.offset(x, -1, z);
            set(level, floor, floorState(x, z, d));
            set(level, floor.below(), Blocks.BLACKSTONE.defaultBlockState());
            set(level, floor.below(2), Blocks.BASALT.defaultBlockState());

            if (ring(d, 9, .65) || ring(d, 18, .65) || ring(d, 28, .7)) {
                set(level, center.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
            }
            boolean bridge = (Math.abs(x) <= 2 || Math.abs(z) <= 2) && d >= 8 && d <= 33;
            if (bridge) set(level, center.offset(x, 0, z), ((Math.abs(x + z) & 3) == 0
                ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
        }
    }

    /** Builds one bounded decoration/stabilization step. */
    static void buildArenaDecoration(ServerLevel level, BlockPos center, int step) {
        if (step >= 0 && step < 4) {
            double a = step * Math.PI / 2.0D;
            buildPylon(level, center.offset(round(Math.cos(a) * 23), 0, round(Math.sin(a) * 23)), step);
            return;
        }
        if (step >= 4 && step < 12) {
            int index = step - 4;
            double a = (index + .5D) * Math.PI / 4.0D;
            buildVent(level, center.offset(round(Math.cos(a) * 16), 0, round(Math.sin(a) * 16)));
            return;
        }
        if (step == 12) {
            buildAltar(level, center);
            return;
        }
        if (step >= 13 && step < 94) {
            stabilizeOuterRingColumn(level, center, step - 13 - 40);
            return;
        }
        if (step == 94) BossArenaRegistryData.get(level.getServer()).register(BossArenaRegistryData.Kind.INFERNAL, center);
    }

    private static BlockState floorState(int x, int z, double d) {
        if (d < 6) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (ring(d, 7, .9) || ring(d, 14, .8) || ring(d, 24, .8)) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        int motif = Math.floorMod(x * 17 + z * 31 + x * z, 37);
        if (motif < 3) return Blocks.MAGMA_BLOCK.defaultBlockState();
        if (motif < 8) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        if (motif == 9) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    private static void buildPylon(ServerLevel level, BlockPos base, int index) {
        // Wide lower foundation so the pillar feels heavy all the way down.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 4 && (edge < 4 || Math.abs(x) + Math.abs(z) < 7)) {
                set(level, base.offset(x, -1, z), edge >= 3
                    ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    : Blocks.BLACKSTONE.defaultBlockState());
            }
            if (edge <= 3 && (edge < 3 || Math.abs(x) + Math.abs(z) < 5)) {
                set(level, base.offset(x, 0, z), edge == 3
                    ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                int edge = Math.max(Math.abs(x), Math.abs(z));
                BlockState state = (y == 2 || y == 5) && edge == 2
                    ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : y % 2 == 0 && edge == 2
                        ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                set(level, base.offset(x, y, z), state);
            }
            if (y == 1 || y == 4) {
                for (int[] d : new int[][]{{3,0},{-3,0},{0,3},{0,-3}}) {
                    set(level, base.offset(d[0], y, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                    set(level, base.offset(d[0] == 0 ? 0 : Integer.signum(d[0]) * 2, y, d[1] == 0 ? 0 : Integer.signum(d[1]) * 2),
                        Blocks.IRON_BARS.defaultBlockState());
                }
            }
        }

        for (int y = 6; y <= 13; y++) {
            BlockState state = y == 8 || y == 11
                ? Blocks.MAGMA_BLOCK.defaultBlockState()
                : y % 3 == 0
                    ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                set(level, base.offset(x, y, z), state);
            }
            if (y == 8 || y == 11) {
                for (int[] d : new int[][]{{2,0},{-2,0},{0,2},{0,-2}}) {
                    set(level, base.offset(d[0], y, d[1]), Blocks.GILDED_BLACKSTONE.defaultBlockState());
                    set(level, base.offset(Integer.signum(d[0]), y, Integer.signum(d[1])), Blocks.CHAIN.defaultBlockState());
                }
            }
        }

        // Large roof canopy with stronger lower silhouette.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 4 && (edge < 4 || Math.abs(x) + Math.abs(z) < 7)) {
                set(level, base.offset(x, 14, z), edge >= 3
                    ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            }
            if (edge <= 3 && (edge < 3 || Math.abs(x) + Math.abs(z) < 5)) {
                set(level, base.offset(x, 15, z), edge == 3
                    ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
        }
        for (int[] d : new int[][]{{3,3},{3,-3},{-3,3},{-3,-3}}) {
            set(level, base.offset(d[0], 15, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            set(level, base.offset(d[0], 16, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
        }
        for (int[] d : new int[][]{{4,0},{-4,0},{0,4},{0,-4}}) {
            set(level, base.offset(d[0], 14, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            set(level, base.offset(d[0], 15, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
        }

        set(level, base.above(16), Blocks.MAGMA_BLOCK.defaultBlockState());
        set(level, base.above(17), Blocks.LAVA.defaultBlockState());

        // Underside buttresses so the lower half feels as detailed as the top.
        for (int[] d : new int[][]{{2,0},{-2,0},{0,2},{0,-2},{2,2},{2,-2},{-2,2},{-2,-2}}) {
            set(level, base.offset(d[0], 1, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            set(level, base.offset(d[0], 2, d[1]), Blocks.BLACKSTONE_WALL.defaultBlockState());
        }
    }

    private static void buildVent(ServerLevel level, BlockPos base) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            set(level, base.offset(x, 0, z), Math.abs(x) + Math.abs(z) == 2
                ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState());
        set(level, base.above(), Blocks.CAMPFIRE.defaultBlockState());
        for (int[] d : new int[][]{{2,0},{-2,0},{0,2},{0,-2}})
            set(level, base.offset(d[0], 0, d[1]), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
    }

    private static void buildAltar(ServerLevel level, BlockPos center) {
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();

        // Base foundation (Y = -1, 13x13, dx/dz = -6..6)
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 6) set(level, center.offset(x, -1, z), edge == 6 ? gilded : bricks);
        }

        // Tier 1 Step (Y = 0, 9x9, dx/dz = -4..4) with inward-facing blackstone stairs
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge == 4) {
                boolean corner = Math.abs(x) == 4 && Math.abs(z) == 4;
                set(level, center.offset(x, 0, z), corner ? gilded : inwardStairs(x, z));
            } else if (edge == 3) {
                set(level, center.offset(x, 0, z), crying);
            } else if (edge <= 2) {
                set(level, center.offset(x, 0, z), bricks);
            }
        }

        // Tier 2 Step (Y = 1, 5x5, dx/dz = -2..2) with inward-facing blackstone stairs
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge == 2) {
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                set(level, center.offset(x, 1, z), corner ? chiseled : inwardStairs(x, z));
            } else if (edge <= 1) {
                set(level, center.offset(x, 1, z), chiseled);
            }
        }

        // Single central altar (Y = 2) - NO soul lanterns, NO brick walls!
        set(level, center.above(2), VeloriseEMC.INFERNAL_ALTAR.get().defaultBlockState());

        // Crying obsidian floor rays extending outward (y = 0)
        for (int[] d : new int[][]{{6,0},{-6,0},{0,6},{0,-6}}) {
            for (int step = 1; step <= 5; step++) set(level, center.offset(d[0] * step / 6, 0, d[1] * step / 6), crying);
        }
    }

    private static BlockState inwardStairs(int x, int z) {
        net.minecraft.core.Direction facing;
        if (Math.abs(x) > Math.abs(z)) {
            facing = x > 0 ? net.minecraft.core.Direction.WEST : net.minecraft.core.Direction.EAST;
        } else {
            facing = z > 0 ? net.minecraft.core.Direction.NORTH : net.minecraft.core.Direction.SOUTH;
        }
        return Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, facing);
    }

    private static void stabilizeOuterRing(ServerLevel level, BlockPos center) {
        for (int x = -40; x <= 40; x++) stabilizeOuterRingColumn(level, center, x);
    }

    private static void stabilizeOuterRingColumn(ServerLevel level, BlockPos center, int x) {
        if (Math.abs(x) > ARENA_RADIUS) return;
        for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d < 29.0D || d > 34.5D) continue;
            for (int y = 2; y >= -3; y--) {
                BlockPos pos = center.offset(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.POLISHED_BLACKSTONE_BRICK_WALL)
                    || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                    || state.is(Blocks.CHISELED_POLISHED_BLACKSTONE)
                    || state.is(Blocks.GILDED_BLACKSTONE)) {
                    BlockPos below = pos.below();
                    while (below.getY() >= center.getY() - 5 && level.getBlockState(below).isAir()) {
                        set(level, below, Blocks.BLACKSTONE.defaultBlockState());
                        below = below.below();
                    }
                }
            }
        }
    }

    public static boolean summonBoss(ServerLevel level, BlockPos center) {
        if (level.dimension() != Level.NETHER) return false;
        if (!level.getEntitiesOfClass(InfernalCrucibleEntity.class, new AABB(center).inflate(96), e -> e.isAlive()).isEmpty()) return false;
        InfernalCrucibleEntity boss = VeloriseEMC.INFERNAL_CRUCIBLE.get().create(level);
        if (boss == null) return false;
        BlockPos resolved = hasArena(level, center) ? center : PromotionBossArenaService.snapPlacementFloor(level, center);
        boss.bindArena(resolved);
        boss.moveTo(resolved.getX() + .5D, resolved.getY() + 5.0D, resolved.getZ() + .5D, 0, 0);
        return level.addFreshEntity(boss);
    }

    public static int placeArena(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.NETHER) { source.sendFailure(Component.literal("Infernal Crucible arena requires the Nether.")); return 0; }
        BlockPos center = PromotionBossArenaService.snapPlacementFloor(source.getLevel(), StructureCommandPlacement.ahead(source, 56));
        buildArena(source.getLevel(), center);
        source.sendSuccess(() -> Component.literal("Placed Infernal Crucible arena at " + center.toShortString() + "."), true);
        return 1;
    }

    public static int summon(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.NETHER) { source.sendFailure(Component.literal("Infernal Crucible requires the Nether.")); return 0; }
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos center = findAltarCenter(source.getLevel(), origin);
        if (center == null) {
            BlockPos nearest = BossArenaRegistryData.get(source.getServer()).nearest(BossArenaRegistryData.Kind.INFERNAL, origin);
            center = (nearest != null && nearest.distSqr(origin) <= 64.0D * 64.0D)
                ? nearest
                : PromotionBossArenaService.snapPlacementFloor(source.getLevel(), origin);
        }
        final BlockPos summonPos = center;
        if (!summonBoss(source.getLevel(), summonPos)) { source.sendFailure(Component.literal("Could not summon The Infernal Crucible.")); return 0; }
        source.sendSuccess(() -> Component.literal("Summoned The Infernal Crucible at " + summonPos.toShortString() + "."), true);
        return 1;
    }

    private static void clear(ServerLevel level, BlockPos center, int radius, int bottom, int top) {
        for (int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if (x*x+z*z > radius*radius) continue;
            for (int y=bottom;y<=top;y++) {
                BlockPos pos=center.offset(x,y,z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
    private static boolean ring(double d,double target,double width){return Math.abs(d-target)<=width;}
    private static int round(double d){return (int)Math.round(d);}
    private static void set(ServerLevel l,BlockPos p,BlockState s){l.setBlockAndUpdate(p,s);}
}
