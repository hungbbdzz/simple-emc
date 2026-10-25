package com.velorise.emc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Operator/testing placement and deterministic ceremonial arenas for the Tier III-IV promotion bosses. */
public final class PromotionBossArenaService {
    private PromotionBossArenaService() {}

    public static int placeBulwark(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.END) {
            source.sendFailure(Component.literal("Aetheric Bulwark arena requires The End."));
            return 0;
        }
        BlockPos center = StructureCommandPlacement.ahead(source, 48);
        buildBulwarkArena(source.getLevel(), center);
        source.sendSuccess(() -> Component.literal("Placed Aetheric Bulwark arena at " + center.toShortString() + "."), true);
        return 1;
    }

    public static int placeDeepbound(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("Deepbound Warden arena requires the Overworld."));
            return 0;
        }
        BlockPos center = StructureCommandPlacement.ahead(source, 48);
        buildDeepboundArena(source.getLevel(), center);
        source.sendSuccess(() -> Component.literal("Placed Deepbound Warden arena at " + center.toShortString() + "."), true);
        return 1;
    }


    public static int summonBulwark(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.END) {
            source.sendFailure(Component.literal("Aetheric Bulwark summon requires The End."));
            return 0;
        }
        BlockPos center = BlockPos.containing(source.getPosition());
        if (!summonBulwarkBoss(source.getLevel(), center)) {
            source.sendFailure(Component.literal("Could not summon the Aetheric Bulwark."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Summoned Aetheric Bulwark at " + center.toShortString() + "."), true);
        return 1;
    }

    public static int summonDeepbound(CommandSourceStack source) {
        if (source.getLevel().dimension() != Level.OVERWORLD) {
            source.sendFailure(Component.literal("Deepbound Warden summon requires the Overworld."));
            return 0;
        }
        BlockPos center = BlockPos.containing(source.getPosition());
        if (!summonDeepboundBoss(source.getLevel(), center)) {
            source.sendFailure(Component.literal("Could not summon the Deepbound Warden."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Summoned Deepbound Warden at " + center.toShortString() + "."), true);
        return 1;
    }

    public static void buildBulwarkArena(ServerLevel level, BlockPos center) {
        for (int x = bulwarkMinX(); x <= bulwarkMaxX(); x++) buildBulwarkColumn(level, center, x);
        for (int step = 0; step < bulwarkDecorationCount(); step++) buildBulwarkDecoration(level, center, step);
    }
    public static void buildDeepboundArena(ServerLevel level, BlockPos center) {
        for (int x = deepboundMinX(); x <= deepboundMaxX(); x++) buildDeepboundColumn(level, center, x);
        for (int step = 0; step < deepboundDecorationCount(); step++) buildDeepboundDecoration(level, center, step);
    }

    public static boolean hasBulwarkArena(ServerLevel level, BlockPos center) {
        return level.getBlockState(center.above(2)).is(VeloriseEMC.AETHERIC_ALTAR.get());
    }

    public static boolean hasDeepboundArena(ServerLevel level, BlockPos center) {
        return level.getBlockState(center.above()).is(Blocks.SCULK_SHRIEKER);
    }

    public static boolean isBulwarkAltar(BlockPos clicked, BlockPos center) {
        int dx = Math.abs(clicked.getX() - center.getX());
        int dz = Math.abs(clicked.getZ() - center.getZ());
        int dy = clicked.getY() - center.getY();
        return dx <= 4 && dz <= 4 && dy >= 0 && dy <= 3;
    }

    public static boolean isDeepboundAltar(BlockPos clicked, BlockPos center) {
        int dx = Math.abs(clicked.getX() - center.getX());
        int dz = Math.abs(clicked.getZ() - center.getZ());
        int dy = clicked.getY() - center.getY();
        return dx <= 4 && dz <= 4 && dy >= -1 && dy <= 2;
    }

    public static BlockPos findBulwarkAltarCenter(ServerLevel level, BlockPos clicked) {
        for (int dy = -3; dy <= 1; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos candidate = clicked.offset(dx, dy, dz);
                    if (hasBulwarkArena(level, candidate) && isBulwarkAltar(clicked, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos findDeepboundAltarCenter(ServerLevel level, BlockPos clicked) {
        for (int dy = -2; dy <= 1; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos candidate = clicked.offset(dx, dy, dz);
                    if (hasDeepboundArena(level, candidate) && isDeepboundAltar(clicked, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isBulwarkAltarProtected(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(VeloriseEMC.AETHERIC_ALTAR.get())) return false;
        return hasBulwarkArena(level, pos.below(2));
    }

    public static boolean isDeepboundAltarProtected(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.SCULK_SHRIEKER) && hasDeepboundArena(level, pos.below());
    }

    public static boolean summonBulwarkBoss(ServerLevel level, BlockPos center) {
        if (level.dimension() != Level.END) return false;
        if (!level.getEntitiesOfClass(AethericBulwarkEntity.class,
            new net.minecraft.world.phys.AABB(center).inflate(96.0D), e -> e.isAlive()).isEmpty()) return false;
        AethericBulwarkEntity boss = VeloriseEMC.AETHERIC_BULWARK.get().create(level);
        if (boss == null) return false;
        boss.bindArena(center);
        // Place directly on top of the Aetheric Altar (which sits at center.above(2))
        boss.moveTo(center.getX() + 0.5D, center.getY() + 3.0D, center.getZ() + 0.5D, 0.0F, 0.0F);
        return level.addFreshEntity(boss);
    }

    public static boolean summonDeepboundBoss(ServerLevel level, BlockPos center) {
        if (level.dimension() != Level.OVERWORLD) return false;
        if (!level.getEntitiesOfClass(DeepboundWardenEntity.class,
            new net.minecraft.world.phys.AABB(center).inflate(96.0D), e -> e.isAlive()).isEmpty()) return false;
        DeepboundWardenEntity boss = VeloriseEMC.DEEPBOUND_WARDEN.get().create(level);
        if (boss == null) return false;
        boss.bindArena(center);
        boss.moveTo(center.getX() + 0.5D, center.getY() + 2.0D, center.getZ() + 0.5D, 0.0F, 0.0F);
        return level.addFreshEntity(boss);
    }

    public static boolean summonBulwark(ServerLevel level, BlockPos center) {
        buildBulwarkArena(level, center);
        return summonBulwarkBoss(level, center);
    }

    public static boolean summonDeepbound(ServerLevel level, BlockPos center) {
        buildDeepboundArena(level, center);
        return summonDeepboundBoss(level, center);
    }

    static int bulwarkMinX() { return -33; }
    static int bulwarkMaxX() { return 33; }
    static int bulwarkDecorationCount() { return 17; }

    static void buildBulwarkColumn(ServerLevel level, BlockPos center, int x) {
        int clearRadius = 33;
        for (int z = -clearRadius; z <= clearRadius; z++) {
            if (x * x + z * z > (clearRadius + 1) * (clearRadius + 1)) continue;
            for (int y = 0; y <= 24; y++) {
                BlockPos pos = center.offset(x, y, z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        final int radius = 26;
        if (Math.abs(x) > radius) return;
        for (int z = -radius; z <= radius; z++) {
            double distance = Math.sqrt(x * x + z * z);
            if (distance > radius + 0.25D) continue;
            BlockPos floor = center.offset(x, -1, z);
            set(level, floor, endFloorState(x, z, distance));
            set(level, floor.below(), Blocks.END_STONE_BRICKS.defaultBlockState());
            set(level, floor.below(2), Blocks.END_STONE.defaultBlockState());
            if (ring(distance, 8.0D, 0.55D) || ring(distance, 16.0D, 0.55D)) {
                set(level, center.offset(x, 0, z), Blocks.PURPUR_SLAB.defaultBlockState());
            }
            boolean avenue = (Math.abs(x) <= 2 || Math.abs(z) <= 2) && distance >= 9.0D && distance <= 23.0D;
            if (avenue) set(level, center.offset(x, 0, z), ((Math.abs(x + z) & 3) == 0
                ? Blocks.CHISELED_QUARTZ_BLOCK : Blocks.SMOOTH_QUARTZ_SLAB).defaultBlockState());
        }
    }

    static void buildBulwarkDecoration(ServerLevel level, BlockPos center, int step) {
        if (step >= 0 && step < 8) {
            double angle = step * Math.PI / 4.0D;
            BlockPos tower = center.offset(round(Math.cos(angle) * 19.0D), 0, round(Math.sin(angle) * 19.0D));
            buildAethericTower(level, tower);
            return;
        }
        if (step >= 8 && step < 16) {
            int index = step - 8;
            double angle = (index + 0.5D) * Math.PI / 4.0D;
            BlockPos obelisk = center.offset(round(Math.cos(angle) * 23.0D), 0, round(Math.sin(angle) * 23.0D));
            buildEndObelisk(level, obelisk);
            return;
        }
        if (step == 16) {
            buildEndDais(level, center);
            BossArenaRegistryData.get(level.getServer()).register(BossArenaRegistryData.Kind.AETHERIC, center);
        }
    }

    private static BlockState endFloorState(int x, int z, double distance) {
        if (distance < 4.5D) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        if (ring(distance, 5.5D, 0.9D) || ring(distance, 12.0D, 0.8D) || ring(distance, 20.0D, 0.8D)) {
            return Blocks.PURPUR_BLOCK.defaultBlockState();
        }
        if (Math.abs(x) <= 1 || Math.abs(z) <= 1 || Math.abs(x - z) <= 1 || Math.abs(x + z) <= 1) {
            return Blocks.END_STONE_BRICKS.defaultBlockState();
        }
        int motif = Math.floorMod(x * 31 + z * 17 + x * z, 23);
        if (motif == 0) return Blocks.OBSIDIAN.defaultBlockState();
        if (motif <= 2) return Blocks.PURPUR_BLOCK.defaultBlockState();
        return Blocks.END_STONE_BRICKS.defaultBlockState();
    }

    private static void buildAethericTower(ServerLevel level, BlockPos base) {
        for (int y = -1; y <= 1; y++) {
            int radius = y < 1 ? 3 : 2;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                int edge = Math.max(Math.abs(x), Math.abs(z));
                if (edge > radius) continue;
                BlockState state = edge == radius ? Blocks.END_STONE_BRICKS.defaultBlockState()
                    : ((x + z + y) & 3) == 0 ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState()
                    : Blocks.PURPUR_BLOCK.defaultBlockState();
                set(level, base.offset(x, y, z), state);
            }
        }

        for (int y = 2; y <= 14; y++) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                boolean corner = Math.abs(x) == 1 && Math.abs(z) == 1;
                BlockState state = (y == 5 || y == 9 || y == 13)
                    ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState()
                    : corner ? Blocks.END_STONE_BRICKS.defaultBlockState()
                    : ((y & 1) == 0 ? Blocks.PURPUR_PILLAR : Blocks.PURPUR_BLOCK).defaultBlockState();
                set(level, base.offset(x, y, z), state);
            }
        }

        for (int bandY : new int[] {5, 9, 13}) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                int edge = Math.max(Math.abs(x), Math.abs(z));
                if (edge == 2 && (Math.abs(x) <= 1 || Math.abs(z) <= 1)) {
                    set(level, base.offset(x, bandY, z), Blocks.PURPUR_BLOCK.defaultBlockState());
                    set(level, base.offset(x, bandY - 1, z), Blocks.END_STONE_BRICK_WALL.defaultBlockState());
                }
            }
        }

        for (int[] dir : new int[][] {{1,0},{-1,0},{0,1},{0,-1}}) {
            for (int step = 2; step <= 4; step++) {
                int x = dir[0] * step;
                int z = dir[1] * step;
                int top = 5 - step;
                for (int y = 0; y <= top; y++) {
                    set(level, base.offset(x, y, z), y == top
                        ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState()
                        : Blocks.END_STONE_BRICKS.defaultBlockState());
                }
            }
        }

        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            set(level, base.offset(x, 15, z), Math.abs(x) == 1 || Math.abs(z) == 1
                ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.SEA_LANTERN.defaultBlockState());
        }
        for (int[] dir : new int[][] {{1,0},{-1,0},{0,1},{0,-1}}) {
            set(level, base.offset(dir[0] * 2, 15, dir[1] * 2), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
            set(level, base.offset(dir[0] * 2, 14, dir[1] * 2), Blocks.END_STONE_BRICK_WALL.defaultBlockState());
        }
        set(level, base.above(16), Blocks.PURPUR_PILLAR.defaultBlockState());
        set(level, base.above(17), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        set(level, base.above(18), Blocks.END_ROD.defaultBlockState());
    }

    private static void buildEndObelisk(ServerLevel level, BlockPos base) {
        for (int y = -1; y <= 0; y++) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                set(level, base.offset(x, y, z), Math.abs(x) == 1 || Math.abs(z) == 1
                    ? Blocks.END_STONE_BRICKS.defaultBlockState() : Blocks.PURPUR_BLOCK.defaultBlockState());
            }
        }
        for (int y = 1; y <= 6; y++) set(level, base.above(y), y == 3 ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState()
            : Blocks.PURPUR_PILLAR.defaultBlockState());
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (Math.abs(x) + Math.abs(z) <= 1) set(level, base.offset(x, 7, z), Blocks.PURPUR_BLOCK.defaultBlockState());
        }
        set(level, base.above(8), Blocks.SEA_LANTERN.defaultBlockState());
        set(level, base.above(9), Blocks.END_ROD.defaultBlockState());
        set(level, base.offset(1, 1, 0), Blocks.END_STONE_BRICK_WALL.defaultBlockState());
        set(level, base.offset(-1, 1, 0), Blocks.END_STONE_BRICK_WALL.defaultBlockState());
        set(level, base.offset(0, 1, 1), Blocks.END_STONE_BRICK_WALL.defaultBlockState());
        set(level, base.offset(0, 1, -1), Blocks.END_STONE_BRICK_WALL.defaultBlockState());
    }

    private static void buildEndDais(ServerLevel level, BlockPos center) {
        BlockState baseBlock = Blocks.END_STONE_BRICKS.defaultBlockState();
        BlockState trimBlock = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState wall = Blocks.END_STONE_BRICK_WALL.defaultBlockState();

        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 5) set(level, center.offset(x, -1, z), edge == 5 ? trimBlock : baseBlock);
            if (edge <= 4) set(level, center.offset(x, 0, z), edge == 4 ? chiseled : (edge == 3 ? crying : baseBlock));
            if (edge <= 2) set(level, center.offset(x, 1, z), edge == 2 ? trimBlock : chiseled);
        }
        set(level, center.above(2), VeloriseEMC.AETHERIC_ALTAR.get().defaultBlockState());

        for (int[] corner : new int[][] {{3,3},{3,-3},{-3,3},{-3,-3}}) {
            set(level, center.offset(corner[0], 1, corner[1]), chiseled);
            set(level, center.offset(corner[0], 2, corner[1]), wall);
            set(level, center.offset(corner[0], 3, corner[1]), Blocks.END_ROD.defaultBlockState());
        }
    }

    static int deepboundMinX() { return -36; }
    static int deepboundMaxX() { return 36; }
    static int deepboundDecorationCount() { return 17; }

    static void buildDeepboundColumn(ServerLevel level, BlockPos center, int x) {
        int clearRadius = 36;
        for (int z = -clearRadius; z <= clearRadius; z++) {
            if (x * x + z * z > (clearRadius + 1) * (clearRadius + 1)) continue;
            for (int y = -1; y <= 34; y++) {
                BlockPos pos = center.offset(x, y, z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        final int radius = 28;
        if (Math.abs(x) > radius) return;
        for (int z = -radius; z <= radius; z++) {
            double distance = Math.sqrt(x * x + z * z);
            if (distance > radius + 0.25D) continue;
            BlockPos floor = center.offset(x, -1, z);
            set(level, floor, deepFloorState(x, z, distance));
            set(level, floor.below(), distance < 22.0D
                ? Blocks.REINFORCED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
            set(level, floor.below(2), Blocks.DEEPSLATE.defaultBlockState());
            if (ring(distance, 9.0D, 0.65D) || ring(distance, 17.0D, 0.65D) || ring(distance, 24.0D, 0.65D)) {
                set(level, center.offset(x, 0, z), Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
            }
        }
    }

    static void buildDeepboundDecoration(ServerLevel level, BlockPos center, int step) {
        if (step >= 0 && step < 8) {
            buildDeepboundCauseway(level, center, step);
            return;
        }
        if (step >= 8 && step < 16) {
            int index = step - 8;
            double angle = index * Math.PI / 4.0D;
            BlockPos tower = center.offset(round(Math.cos(angle) * 20.0D), 0, round(Math.sin(angle) * 20.0D));
            buildDeepboundTower(level, tower, index);
            return;
        }
        if (step == 16) {
            buildDeepboundAltar(level, center);
            BossArenaRegistryData.get(level.getServer()).register(BossArenaRegistryData.Kind.DEEPBOUND, center);
        }
    }

    private static BlockState deepFloorState(int x, int z, double distance) {
        boolean isRoad = Math.abs(x) <= 1 || Math.abs(z) <= 1 || Math.abs(x - z) <= 1 || Math.abs(x + z) <= 1;
        if (isRoad) {
            return ((int)distance % 2 == 0) ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (ring(distance, 7.0D, 0.8D) || ring(distance, 15.0D, 0.8D) || ring(distance, 23.0D, 0.8D)) {
            return Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
        }
        if (ring(distance, 11.0D, 0.35D)) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        int motif = Math.floorMod(x * 19 + z * 29 + x * z, 31);
        if (motif < 4) return Blocks.SCULK.defaultBlockState();
        if (motif == 4) return Blocks.SCULK_CATALYST.defaultBlockState();
        if (motif == 5) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        if (motif < 12) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    }

    private static void buildDeepboundCauseway(ServerLevel level, BlockPos center, int index) {
        double angle = index * Math.PI / 4.0D;
        double directionX = Math.cos(angle);
        double directionZ = Math.sin(angle);
        int tangentX = round(-directionZ);
        int tangentZ = round(directionX);

        for (int step = 4; step <= 25; step++) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                int x = round(directionX * step) + tangentX * lateral;
                int z = round(directionZ * step) + tangentZ * lateral;
                BlockState state;
                if (lateral == 0 && step % 6 == 0) state = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
                else if (lateral == 0 && step % 3 == 0) state = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                else if (lateral != 0 && (step + index) % 5 == 0) state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                else state = lateral == 0 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                set(level, center.offset(x, -1, z), state);
            }

            int edgeDistance = ((step / 3 + index) & 1) == 0 ? 2 : 3;
            for (int side : new int[] {-1, 1}) {
                int x = round(directionX * step) + tangentX * edgeDistance * side;
                int z = round(directionZ * step) + tangentZ * edgeDistance * side;
                set(level, center.offset(x, -1, z), step % 4 == 0
                    ? Blocks.SCULK.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
                if (step % 3 == 0) {
                    set(level, center.offset(x, 0, z), Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
                }
            }

            if (step == 7 || step == 12 || step == 17 || step == 24) {
                for (int lateral = -3; lateral <= 3; lateral++) {
                    int x = round(directionX * step) + tangentX * lateral;
                    int z = round(directionZ * step) + tangentZ * lateral;
                    BlockState state = lateral == 0 ? Blocks.SCULK_CATALYST.defaultBlockState()
                        : Math.abs(lateral) == 3 ? Blocks.REINFORCED_DEEPSLATE.defaultBlockState()
                        : ((lateral & 1) == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                            : Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                    set(level, center.offset(x, -1, z), state);
                }
            }
        }

        for (int markerStep : new int[] {8, 15, 24}) {
            for (int side : new int[] {-1, 1}) {
                int x = round(directionX * markerStep) + tangentX * 3 * side;
                int z = round(directionZ * markerStep) + tangentZ * 3 * side;
                set(level, center.offset(x, -1, z), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
                set(level, center.offset(x, 0, z), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                set(level, center.offset(x, 1, z), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
                set(level, center.offset(x, 2, z), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }
    }

    private static void buildDeepboundLowerButtress(ServerLevel level, BlockPos base,
                                                    int dx, int dz, int phase) {
        int tangentX = -dz;
        int tangentZ = dx;
        for (int step = 2; step <= 4; step++) {
            int y = step - 1;
            int x = dx * step;
            int z = dz * step;
            set(level, base.offset(x, y, z), step == 4
                ? Blocks.REINFORCED_DEEPSLATE.defaultBlockState()
                : ((step + phase) % 2 == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.DEEPSLATE_BRICKS.defaultBlockState()));
            set(level, base.offset(x, y - 1, z), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());

            if (step == 2 || step == 3) {
                set(level, base.offset(x + tangentX, y, z + tangentZ), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                set(level, base.offset(x - tangentX, y, z - tangentZ), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
            }
        }
        set(level, base.offset(dx * 3, 4, dz * 3), Blocks.SCULK_CATALYST.defaultBlockState());
        set(level, base.offset(dx * 2, 5, dz * 2), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
    }

    private static void buildDeepTowerRing(ServerLevel level, BlockPos base, int radius, int y,
                                           BlockState main, BlockState accent, boolean raisedCourse) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
            boolean node = (x == 0 || z == 0 || Math.abs(x) == Math.abs(z));
            set(level, base.offset(x, y, z), node ? accent : main);
            if (raisedCourse && node && (Math.abs(x) + Math.abs(z)) % 2 == 0) {
                set(level, base.offset(x, y + 1, z), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState());
            }
        }
    }

    private static void buildDeepboundTower(ServerLevel level, BlockPos base, int index) {
        BlockState reinforced = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
        BlockState bricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        BlockState polished = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState catalyst = Blocks.SCULK_CATALYST.defaultBlockState();
        BlockState wall = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();

        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (Math.abs(x) + Math.abs(z) > 7) continue;
            int r = Math.max(Math.abs(x), Math.abs(z));
            BlockState state = r == 4 ? reinforced
                : r == 3 ? bricks
                : r == 2 ? ((((x + z + index) & 1) == 0) ? gilded : cracked)
                : r == 1 ? chiseled : crying;
            set(level, base.offset(x, -1, z), state);
        }
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (Math.abs(x) + Math.abs(z) > 5) continue;
            int r = Math.max(Math.abs(x), Math.abs(z));
            BlockState state = r == 3 ? gilded : r == 2 ? reinforced : (r == 1 ? bricks : crying);
            set(level, base.offset(x, 0, z), state);
        }

        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int directionIndex = 0; directionIndex < directions.length; directionIndex++) {
            int dx = directions[directionIndex][0];
            int dz = directions[directionIndex][1];
            int px = dx * 4;
            int pz = dz * 4;
            int pillarTop = (dx == 0 || dz == 0) ? 13 : 11;

            set(level, base.offset(px, -1, pz), reinforced);
            set(level, base.offset(px, 0, pz), reinforced);
            for (int y = 1; y <= pillarTop; y++) {
                BlockState state;
                if (y == 1 || y == pillarTop || y % 5 == 0) state = reinforced;
                else if (y % 4 == 0) state = catalyst;
                else if ((y + directionIndex) % 3 == 0) state = crying;
                else state = wall;
                set(level, base.offset(px, y, pz), state);
            }
            set(level, base.offset(px, pillarTop + 1, pz), chiseled);
            set(level, base.offset(px, pillarTop + 2, pz), lantern);

            buildDeepboundLowerButtress(level, base, dx, dz, directionIndex);
        }

        buildDeepTowerRing(level, base, 4, 2, bricks, crying, true);
        buildDeepTowerRing(level, base, 4, 6, reinforced, catalyst, true);
        buildDeepTowerRing(level, base, 3, 9, polished, crying, false);
        buildDeepTowerRing(level, base, 4, 11, bricks, catalyst, true);

        for (int y = 2; y <= 24; y++) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                boolean corner = Math.abs(x) == 1 && Math.abs(z) == 1;
                boolean centerColumn = x == 0 && z == 0;
                boolean majorBand = y == 6 || y == 11 || y == 16 || y == 21 || y == 24;
                BlockState state;
                if (centerColumn) {
                    state = y % 5 == 0 ? crying : (y % 2 == 0 ? chiseled : reinforced);
                } else if (corner) {
                    state = majorBand ? catalyst : (y % 4 == 0 ? cracked : bricks);
                } else {
                    state = majorBand ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : (y % 3 == 0 ? crying : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                }
                set(level, base.offset(x, y, z), state);
            }
        }

        for (int bandY : new int[] {6, 11, 16, 21, 24}) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                int r = Math.max(Math.abs(x), Math.abs(z));
                if (r != 2 || (Math.abs(x) == 2 && Math.abs(z) == 2)) continue;
                set(level, base.offset(x, bandY, z), crying);
                set(level, base.offset(x, bandY - 1, z), wall);
                if ((x == 0 || z == 0) && bandY != 24) {
                    set(level, base.offset(x, bandY + 1, z), chiseled);
                }
            }
            for (int[] cardinal : new int[][] {{1,0},{-1,0},{0,1},{0,-1}}) {
                set(level, base.offset(cardinal[0] * 2, bandY + 1, cardinal[1] * 2), catalyst);
            }
        }

        for (int[] diagonal : new int[][] {{1,1},{1,-1},{-1,1},{-1,-1}}) {
            int sx = diagonal[0] * 2;
            int sz = diagonal[1] * 2;
            for (int y = 19; y <= 26; y++) {
                set(level, base.offset(sx, y, sz), y == 22 || y == 26 ? catalyst : wall);
            }
            set(level, base.offset(sx, 27, sz), chiseled);
            set(level, base.offset(sx, 28, sz), lantern);
        }

        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            int r = Math.max(Math.abs(x), Math.abs(z));
            if (Math.abs(x) + Math.abs(z) > 5) continue;
            if (r == 3) set(level, base.offset(x, 25, z), wall);
            else if (r == 2) set(level, base.offset(x, 25, z), crying);
            else if (r <= 1) set(level, base.offset(x, 25, z), Blocks.SOUL_SOIL.defaultBlockState());
        }
        set(level, base.offset(0, 26, 0), Blocks.SOUL_FIRE.defaultBlockState());

        for (int[] corner : new int[][] {{3,3},{3,-3},{-3,3},{-3,-3}}) {
            int sx = Integer.signum(corner[0]);
            int sz = Integer.signum(corner[1]);
            set(level, base.offset(corner[0], 25, corner[1]), reinforced);
            set(level, base.offset(corner[0], 26, corner[1]), chiseled);
            set(level, base.offset(sx * 2, 27, sz * 2), bricks);
            set(level, base.offset(sx * 2, 28, sz * 2), chiseled);
            set(level, base.offset(sx, 29, sz), lantern);
        }
    }

    private static void buildDeepboundAltar(ServerLevel level, BlockPos center) {
        BlockState frame = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState inner = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState sculk = Blocks.SCULK.defaultBlockState();
        BlockState catalyst = Blocks.SCULK_CATALYST.defaultBlockState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState blackstoneChiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();

        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 5) set(level, center.offset(x, -2, z), edge == 5 ? frame : floor);
        }
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 4) set(level, center.offset(x, -1, z), edge == 4 ? frame : (edge == 3 ? inner : sculk));
        }
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            int edge = Math.max(Math.abs(x), Math.abs(z));
            if (edge <= 2) set(level, center.offset(x, 0, z), edge == 2 ? frame : sculk);
        }
        set(level, center.above(1), Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(net.minecraft.world.level.block.SculkShriekerBlock.CAN_SUMMON, true));

        for (int[] side : new int[][] {{3,0},{-3,0},{0,3},{0,-3}}) {
            set(level, center.offset(side[0], 0, side[1]), catalyst);
            set(level, center.offset(side[0], 1, side[1]), cryingObsidian);
            set(level, center.offset(side[0], 2, side[1]), Blocks.SOUL_LANTERN.defaultBlockState());
        }
        for (int[] corner : new int[][] {{2,2},{2,-2},{-2,2},{-2,-2}}) {
            set(level, center.offset(corner[0], 0, corner[1]), blackstoneChiseled);
            set(level, center.offset(corner[0], 1, corner[1]), catalyst);
        }
    }

    private static boolean ring(double distance, double target, double width) {
        return Math.abs(distance - target) <= width;
    }

    private static int round(double value) {
        return (int)Math.round(value);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state);
    }
}
