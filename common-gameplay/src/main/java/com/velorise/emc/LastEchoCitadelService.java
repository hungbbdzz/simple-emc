package com.velorise.emc;

import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Deterministic, player-proximity-triggered citadel builder. Never replaces non-air blocks. */
public final class LastEchoCitadelService {
    private static final int GRID_RADIUS = 64;
    private static final int GRID_WIDTH = GRID_RADIUS * 2 + 1;
    private static final int MAX_BUILD_COLUMNS_PER_TICK = 32;
    private static final long BUILD_BUDGET_NANOS = 1_250_000L;

    private LastEchoCitadelService() {}

    /** True only for the End arena volume while its boss fight is active, plus the permanent central altar. */
    public static boolean isArenaProtected(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.END)) return false;
        LastEchoCitadelData data = LastEchoCitadelData.get(level.getServer());
        BlockPos center = data.center();
        if (center == null || !data.built()) return false;
        int fountainDx = Math.abs(pos.getX() - center.getX());
        int fountainDz = Math.abs(pos.getZ() - center.getZ());
        int fountainDy = pos.getY() - center.getY();
        // The altar (Y + 1..3) must survive between encounters so another Eye can always
        // reactivate it. This compact volume is permanent and needs no Bedrock.
        if (fountainDx <= 2 && fountainDz <= 2
            && fountainDy >= 1 && fountainDy <= 3) return true;
        if (!data.bossActive() || data.defeated()) return false;
        int dy = pos.getY() - center.getY();
        if (dy < -LastEchoProfile.ARENA_PROTECTION_BELOW
            || dy > LastEchoProfile.ARENA_PROTECTION_ABOVE) return false;
        long dx = (long)pos.getX() - center.getX();
        long dz = (long)pos.getZ() - center.getZ();
        long radius = LastEchoProfile.ARENA_PROTECTION_RADIUS;
        return dx * dx + dz * dz <= radius * radius;
    }

    public static BlockPos ensureLocated(MinecraftServer server) {
        if (WorldProgressionData.get(server).snapshot().worldTier() < LastEchoProfile.REQUIRED_WORLD_TIER) return null;
        LastEchoCitadelData data = LastEchoCitadelData.get(server);
        if (data.center() == null) {
            RandomSource random = RandomSource.create(server.overworld().getSeed() ^ 0x4C4153544543484FL);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = LastEchoProfile.MIN_DISTANCE + random.nextInt(LastEchoProfile.MAX_DISTANCE - LastEchoProfile.MIN_DISTANCE + 1);
            data.locate(new BlockPos(Mth.floor(Math.cos(angle) * distance), LastEchoProfile.CITADEL_Y, Mth.floor(Math.sin(angle) * distance)));
        }
        return data.center();
    }

    public static void tick(MinecraftServer server) {
        BlockPos center = ensureLocated(server);
        if (center == null) return;
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        LastEchoCitadelData data = LastEchoCitadelData.get(server);
        if (!data.built()) {
            if (!playerWithin(end, center, LastEchoProfile.BUILD_TRIGGER_RADIUS)) return;
            if (data.buildCursor() == 0 && !highSkyClear(end, center)) {
                // A modded sky structure already owns this space. Rotate to another point in the
                // same outer-End annulus instead of cutting into or replacing any of its blocks.
                double angle = Math.atan2(center.getZ(), center.getX()) + 2.399963229728653D;
                double radius = Math.sqrt((double)center.getX() * center.getX() + (double)center.getZ() * center.getZ());
                data.locate(new BlockPos(Mth.floor(Math.cos(angle) * radius), LastEchoProfile.CITADEL_Y,
                    Mth.floor(Math.sin(angle) * radius)));
                return;
            }
            buildBatch(end, data, center);
            return;
        }
        // Once built, routine boss/fountain checks do not need to run every tick.
        if ((server.getTickCount() & 3) != 0) return;
        if (data.defeated()) {
            // Preserve the Exit Portal and Dragon Egg after victory while keeping the central altar intact.
            if (!end.getBlockState(center.above(3)).is(VeloriseEMC.LAST_ECHO_ALTAR.get())) {
                end.setBlock(center.above(3), VeloriseEMC.LAST_ECHO_ALTAR.get().defaultBlockState(), 3);
            }
            return;
        }
        ensureRespawnFountain(end, center);
        // Only reconcile bossActive when the citadel chunk is actually loaded in memory!
        // Otherwise, if a player dies and respawns in the Overworld (unloading the outer End),
        // getEntitiesOfClass returns empty and would falsely clear bossActive.
        if (!end.hasChunkAt(center)) return;
        AABB arenaBox = new AABB(center).inflate(LastEchoProfile.ACTIVATION_RADIUS, 72.0D, LastEchoProfile.ACTIVATION_RADIUS);
        boolean bossPresent = !end.getEntitiesOfClass(EchoboundDragonEntity.class, arenaBox, e -> e.isAlive()).isEmpty();
        if (data.bossActive() && !bossPresent && server.getTickCount() % 200 == 0) data.setBossActive(false);
        if (!data.bossActive() && bossPresent) data.setBossActive(true);
    }

    private static boolean playerWithin(ServerLevel level, BlockPos center, double radius) {
        double max = radius * radius;
        for (ServerPlayer player : level.players()) if (!player.isSpectator() && player.distanceToSqr(center.getX(), center.getY(), center.getZ()) <= max) return true;
        return false;
    }

    private static boolean highSkyClear(ServerLevel level, BlockPos center) {
        for (int dx = -48; dx <= 48; dx += 16) for (int dz = -48; dz <= 48; dz += 16) {
            for (int dy = -10; dy <= 50; dy += 15) {
                if (!level.getBlockState(center.offset(dx, dy, dz)).isAir()) return false;
            }
        }
        return true;
    }

    private static void buildBatch(ServerLevel level, LastEchoCitadelData data, BlockPos center) {
        int cursor = data.buildCursor();
        int total = GRID_WIDTH * GRID_WIDTH;
        int hardStop = Math.min(total, cursor + Math.min(MAX_BUILD_COLUMNS_PER_TICK, LastEchoProfile.COLUMNS_PER_TICK));
        long deadline = System.nanoTime() + BUILD_BUDGET_NANOS;
        int index = cursor;
        while (index < hardStop && System.nanoTime() < deadline) {
            int dx = index % GRID_WIDTH - GRID_RADIUS;
            int dz = index / GRID_WIDTH - GRID_RADIUS;
            buildColumn(level, center, dx, dz);
            index++;
        }
        if (index >= total) {
            buildRespawnFountain(level, center);
            data.markBuilt();
        } else data.setBuildCursor(index);
    }

    private static void buildColumn(ServerLevel level, BlockPos center, int dx, int dz) {
        double distance = Math.sqrt(dx * dx + dz * dz);
        double coarse = Math.sin(dx * 0.31D) * 2.4D + Math.cos(dz * 0.27D) * 2.0D
            + Math.sin((dx + dz) * 0.17D) * 1.6D;
        double detail = coordinateNoise(dx, dz);
        double noise = coarse + detail * 3.25D;
        double islandRadius = 58.0D + noise;
        if (distance <= islandRadius) {
            int edgeRelief = distance < 49.0D ? 0 : Mth.floor(detail * 2.2D + (islandRadius - distance) * 0.14D);
            int top = center.getY() + edgeRelief;
            int depth = Math.max(3, Mth.floor((islandRadius - distance) * 0.30D
                + Math.abs(coordinateNoise(dx * 3 + 7, dz * 3 - 11)) * 6.0D));
            for (int y = top - depth; y <= top; y++) {
                BlockState state = y == top && distance < 49.0D ? arenaFloor(dx, dz, distance)
                    : y >= top - 2 && distance > 45.0D ? Blocks.END_STONE_BRICKS.defaultBlockState()
                    : Blocks.END_STONE.defaultBlockState();
                placeAirOnly(level, center.offset(dx, y - center.getY(), dz), state);
            }
        }

        // Eight radial causeways lead toward the altar and break up the otherwise circular floor.
        boolean cardinalRoad = (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && distance >= 4.0D && distance <= 47.0D;
        boolean diagonalRoad = (Math.abs(dx - dz) <= 1 || Math.abs(dx + dz) <= 1) && distance >= 6.0D && distance <= 43.0D;
        if (cardinalRoad || diagonalRoad) {
            placeAirOnly(level, center.offset(dx, 1, dz), Math.floorMod(dx + dz, 5) == 0
                ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        }

        buildCitadelPillarColumn(level, center, dx, dz);
    }

    /** Builds four major cardinal towers plus four medium diagonal towers in an 8-point symmetric ring at R=35 with gothic flying buttress branches and dragon claw hand tops. */
    private static void buildCitadelPillarColumn(ServerLevel level, BlockPos center, int dx, int dz) {
        boolean eastWest = Math.abs(Math.abs(dx) - 35) <= 5 && Math.abs(dz) <= 5;
        boolean northSouth = Math.abs(Math.abs(dz) - 35) <= 5 && Math.abs(dx) <= 5;
        if (eastWest || northSouth) {
            int cx = eastWest ? Integer.signum(dx) * 35 : 0;
            int cz = northSouth ? Integer.signum(dz) * 35 : 0;
            int lx = Math.abs(dx - cx), lz = Math.abs(dz - cz);
            int radial = Math.max(lx, lz);
            int manhattan = lx + lz;

            // --- 1. Base Plinth & Flared Ground Foundation (Y = 1..3) ---
            if (radial <= 6) {
                placeAirOnly(level, center.offset(dx, 1, dz),
                    radial == 6 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
            }
            if (radial <= 5) {
                placeAirOnly(level, center.offset(dx, 2, dz),
                    radial == 5 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
            if (radial <= 4) {
                placeAirOnly(level, center.offset(dx, 3, dz),
                    radial == 4 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            }

            // --- 1b. Lower Interlocking Belts tying the outer legs back into the core ---
            if (radial >= 3 && radial <= 5) {
                for (int y : new int[] {6, 11}) {
                    if (radial == 5 && (lx == 0 || lz == 0 || lx == lz)) {
                        placeAirOnly(level, center.offset(dx, y, dz),
                            y == 11 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                                : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    } else if (radial == 4 && (lx <= 1 || lz <= 1 || lx == lz)) {
                        placeAirOnly(level, center.offset(dx, y, dz),
                            y == 11 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                    } else if (radial == 3 && manhattan <= 4) {
                        placeAirOnly(level, center.offset(dx, y, dz), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
                    }
                }
            }

            // --- 2. Double-Tiered Outer Flying Buttresses & Bottom Connecting Rings (Y = 3..26) ---
            if ((radial == 3 || radial == 4) && (lx == 0 || lz == 0 || lx == lz)) {
                placeAirOnly(level, center.offset(dx, 3, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 4, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 5, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }

            if (radial == 5 && (lx == 0 || lz == 0 || lx == lz)) {
                for (int y = 3; y <= 14; y++) {
                    BlockState state = (y == 7 || y == 14) ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
                    placeAirOnly(level, center.offset(dx, y, dz), state);
                }
            }
            if (radial == 4 && (lx == 0 || lz == 0)) {
                for (int y = 13; y <= 26; y++) {
                    BlockState state = (y == 20 || y == 26) ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
                    placeAirOnly(level, center.offset(dx, y, dz), state);
                }
            }

            // --- 3. Main Central Spire Shaft with Gothic Ribbed Belts (Y = 3..27 at radial <= 2) ---
            if (radial <= 2 && manhattan <= 3) {
                for (int y = 3; y <= 27; y++) {
                    BlockState state;
                    if (y == 7 || y == 14 || y == 21 || y == 27) state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                    else if ((lx == 2 || lz == 2) && y % 4 == 0) state = Blocks.GILDED_BLACKSTONE.defaultBlockState();
                    else if (y % 5 == 0) state = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                    else state = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
                    placeAirOnly(level, center.offset(dx, y, dz), state);
                }
            }
            // Vertical Wall Trim Column Ledges
            if ((lx == 2 && lz <= 1) || (lz == 2 && lx <= 1)) {
                for (int y : new int[] {7, 14, 21, 27}) {
                    placeAirOnly(level, center.offset(dx, y, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    placeAirOnly(level, center.offset(dx, y - 1, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                }
            }

            // --- 3b. Suspended Brace Bands across the middle body to avoid isolated shafts ---
            if (radial >= 3 && radial <= 5) {
                for (int y : new int[] {17, 24}) {
                    if (radial == 5 && (lx == 0 || lz == 0)) {
                        placeAirOnly(level, center.offset(dx, y, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    } else if (radial == 4 && (lx == 0 || lz == 0 || lx == lz)) {
                        placeAirOnly(level, center.offset(dx, y, dz),
                            y == 24 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                                : Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    } else if (radial == 3 && manhattan <= 4) {
                        placeAirOnly(level, center.offset(dx, y, dz), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                    }
                }
            }

            // --- 4. Palm Platform & Central Bedrock Crystal Pedestal (Y = 28..30) ---
            if (radial <= 2 && manhattan <= 3) {
                placeAirOnly(level, center.offset(dx, 28, dz),
                    radial == 2 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.CRYING_OBSIDIAN.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 29, dz),
                    radial == 2 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : Blocks.GILDED_BLACKSTONE.defaultBlockState());
            }
            if (lx == 0 && lz == 0) {
                // Central unbreakable Bedrock crystal pedestal (Y = 30)
                placeAirOnly(level, center.offset(dx, 30, dz), Blocks.BEDROCK.defaultBlockState());
            } else if (radial <= 1) {
                placeAirOnly(level, center.offset(dx, 30, dz), Blocks.GILDED_BLACKSTONE.defaultBlockState());
            }

            // --- 5. DRAGON CLAW HAND HOLDING THE CRYSTAL (Y = 31..33) ---
            if (lx == 2 && lz == 2) {
                placeAirOnly(level, center.offset(dx, 31, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 32, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 33, dz), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            }
            if (radial == 2 && (lx == 0 || lz == 0)) {
                placeAirOnly(level, center.offset(dx, 30, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 31, dz), Blocks.GILDED_BLACKSTONE.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 32, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }
            if (radial == 3 && (lx == 0 || lz == 0 || lx == lz)) {
                placeAirOnly(level, center.offset(dx, 29, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 30, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
            return;
        }

        // 4 Medium Side Pillars placed diagonally at (+/-25, +/-25)
        boolean diagonal = Math.abs(Math.abs(dx) - 25) <= 3 && Math.abs(Math.abs(dz) - 25) <= 3;
        if (diagonal) {
            int cx = Integer.signum(dx) * 25;
            int cz = Integer.signum(dz) * 25;
            int lx = Math.abs(dx - cx), lz = Math.abs(dz - cz), r = Math.max(lx, lz);
            int manhattan = lx + lz;

            if (r == 3 && (lx == 0 || lz == 0 || lx == lz)) {
                placeAirOnly(level, center.offset(dx, 1, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                for (int y = 2; y <= 6; y++) {
                    placeAirOnly(level, center.offset(dx, y, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                }
            }
            if (r == 2 && (lx == 0 || lz == 0 || lx == lz)) {
                placeAirOnly(level, center.offset(dx, 2, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 3, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 4, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
            }

            if (r >= 1 && r <= 3) {
                for (int y : new int[] {5, 9}) {
                    if (r == 3 && (lx == 0 || lz == 0 || lx == lz)) {
                        placeAirOnly(level, center.offset(dx, y, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    } else if (r == 2 && (lx <= 1 || lz <= 1 || lx == lz)) {
                        placeAirOnly(level, center.offset(dx, y, dz),
                            y == 9 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                    } else if (r == 1 && manhattan <= 2) {
                        placeAirOnly(level, center.offset(dx, y, dz), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
                    }
                }
            }

            if (r <= 2) {
                if (r == 2) {
                    placeAirOnly(level, center.offset(dx, 1, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    if (manhattan <= 3) {
                        placeAirOnly(level, center.offset(dx, 2, dz), Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
                        if ((lx == 2 && lz == 0) || (lz == 2 && lx == 0)) {
                            for (int y : new int[] {4, 8, 12}) {
                                placeAirOnly(level, center.offset(dx, y, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                            }
                        }
                    }
                } else {
                    for (int y = 1; y <= 15; y++) {
                        BlockState state;
                        if (y == 1) state = Blocks.OBSIDIAN.defaultBlockState();
                        else if (y == 4 || y == 8 || y == 12) state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                        else if (y == 6 || y == 10) state = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                        else if (y % 3 == 0) state = Blocks.GILDED_BLACKSTONE.defaultBlockState();
                        else state = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
                        placeAirOnly(level, center.offset(dx, y, dz), state);
                    }

                    if (r == 1) {
                        if (lx == 1 && lz == 1) {
                            for (int y : new int[] {3, 7, 11}) {
                                placeAirOnly(level, center.offset(dx, y, dz), Blocks.GILDED_BLACKSTONE.defaultBlockState());
                            }
                        } else {
                            for (int y : new int[] {3, 5, 7, 9, 11, 13}) {
                                placeAirOnly(level, center.offset(dx, y, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                            }
                        }
                        placeAirOnly(level, center.offset(dx, 16, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    }

                    if (lx == 0 && lz == 0) {
                        placeAirOnly(level, center.offset(dx, 16, dz), Blocks.GILDED_BLACKSTONE.defaultBlockState());
                        placeAirOnly(level, center.offset(dx, 17, dz), Blocks.BEDROCK.defaultBlockState());
                    }

                    if (lx == 1 && lz == 1) {
                        placeAirOnly(level, center.offset(dx, 17, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                        placeAirOnly(level, center.offset(dx, 18, dz), Blocks.SOUL_LANTERN.defaultBlockState());
                    }
                }
            }
            return;
        }

        for (int i = 0; i < 16; i++) {
            double angle = (i + 0.5D) * (Math.PI / 8.0D);
            int radius = (i & 1) == 0 ? 23 : 29;
            int px = Mth.floor(Math.cos(angle) * radius + 0.5D);
            int pz = Mth.floor(Math.sin(angle) * radius + 0.5D);
            int lx = Math.abs(dx - px), lz = Math.abs(dz - pz), r = Math.max(lx, lz);
            if (r > 1) continue;

            placeAirOnly(level, center.offset(dx, 1, dz), r == 1
                ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
            if (r == 1) {
                placeAirOnly(level, center.offset(dx, 2, dz), (lx == 1 && lz == 1)
                    ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 6, dz), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 8, dz), (lx == 1 && lz == 1)
                    ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            } else {
                for (int y = 2; y <= 9; y++) {
                    BlockState state;
                    if (y == 4 || y == 7) state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                    else if (y == 5 || y == 8) state = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                    else state = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
                    placeAirOnly(level, center.offset(dx, y, dz), state);
                }
                placeAirOnly(level, center.offset(dx, 10, dz), Blocks.GILDED_BLACKSTONE.defaultBlockState());
                placeAirOnly(level, center.offset(dx, 11, dz), Blocks.SOUL_LANTERN.defaultBlockState());
            }
            return;
        }
    }

    private static BlockState arenaFloor(int dx, int dz, double distance) {
        if (ring(distance, 8.0D, 0.75D) || ring(distance, 18.0D, 0.75D)
            || ring(distance, 31.0D, 0.75D) || ring(distance, 43.0D, 0.75D)) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (ring(distance, 13.0D, 0.5D) || ring(distance, 24.0D, 0.5D) || ring(distance, 37.0D, 0.5D)) {
            return Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || Math.abs(dx - dz) <= 1 || Math.abs(dx + dz) <= 1) {
            return Math.floorMod(dx + dz, 6) == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                : Blocks.OBSIDIAN.defaultBlockState();
        }
        long hash = coordinateHash(dx, dz);
        int patch = Math.floorMod((int)(hash ^ (hash >>> 32)), 113);
        if (patch < 6) return Blocks.SCULK.defaultBlockState();
        if (patch < 12) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        if (patch < 16) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        if (patch < 25) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        return Blocks.OBSIDIAN.defaultBlockState();
    }

    private static boolean ring(double distance, double target, double width) {
        return Math.abs(distance - target) <= width;
    }

    private static double coordinateNoise(int x, int z) {
        long hash = coordinateHash(x, z);
        return ((hash >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private static long coordinateHash(int x, int z) {
        long value = (long)x * 0x9E3779B97F4A7C15L ^ (long)z * 0xC2B2AE3D27D4EB4FL ^ 0x4543484F424F554EL;
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27; value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static void placeAirOnly(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir()) level.setBlock(pos, state, 2);
    }

    private static void ensureRespawnFountain(ServerLevel level, BlockPos center) {
        // The central altar summit is placed at center.above(3) by buildRespawnFountain.
        if (!level.getBlockState(center.above(3)).is(VeloriseEMC.LAST_ECHO_ALTAR.get())) {
            buildRespawnFountain(level, center);
        }
    }

    /** Intricate 11x11 multi-tiered stepped Citadel Ziggurat Pyramid built of dark obsidian & blackstone blocks with the central altar at the summit. */
    private static void buildRespawnFountain(ServerLevel level, BlockPos center) {
        BlockState baseBlock = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        BlockState deepslate = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
        BlockState endBricks = Blocks.END_STONE_BRICKS.defaultBlockState();

        // Clear space above altar (y = 3..10) to make sure NO block sits above the altar
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            for (int y = 3; y <= 10; y++) {
                level.setBlock(center.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // Tier 1: 9x9 Base Platform (y = 1, dx/dz = -4..4)
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int edge = Math.max(Math.abs(dx), Math.abs(dz));
            BlockPos pos = center.offset(dx, 1, dz);
            boolean corner = Math.abs(dx) == 4 && Math.abs(dz) == 4;
            boolean ray = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
            if (corner) level.setBlock(pos, crying, 3);
            else if (edge == 4) level.setBlock(pos, gilded, 3);
            else if (edge == 3) level.setBlock(pos, ray ? endBricks : chiseled, 3);
            else level.setBlock(pos, ray ? baseBlock : deepslate, 3);
        }

        // Tier 2: 5x5 Upper Platform (y = 2, dx/dz = -2..2)
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int edge = Math.max(Math.abs(dx), Math.abs(dz));
            BlockPos pos = center.offset(dx, 2, dz);
            boolean ray = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
            if (edge == 2) level.setBlock(pos, ray ? gilded : chiseled, 3);
            else level.setBlock(pos, ray ? baseBlock : endBricks, 3);
        }

        // Tier 3: Central Altar Block at Summit (y = 3, at 0,3,0) - NO block above!
        level.setBlock(center.offset(0, 3, 0), VeloriseEMC.LAST_ECHO_ALTAR.get().defaultBlockState(), 3);
    }

    /** Handles an Eye used anywhere on the central pyramid altar. */
    public static boolean useRespawnFountain(ServerLevel level, BlockPos clicked,
                                              ServerPlayer player, ItemStack eye) {
        LastEchoCitadelData data = LastEchoCitadelData.get(level.getServer());
        BlockPos center = data.center();
        if (center == null || !data.built() || !Level.END.equals(level.dimension())) return false;
        int dx = Math.abs(clicked.getX() - center.getX());
        int dz = Math.abs(clicked.getZ() - center.getZ());
        int dy = clicked.getY() - center.getY();
        if (dx > 5 || dz > 5 || dy < 0 || dy > 5) return false;
        if (!level.getBlockState(clicked).is(VeloriseEMC.LAST_ECHO_ALTAR.get())
            && !(dx <= 2 && dz <= 2 && dy >= 1 && dy <= 4)) return false;

        if (data.bossActive() || ArenaSummonService.isPending(level.dimension(), center, ArenaSummonService.BossKind.LAST_ECHO)) {
            player.displayClientMessage(Component.translatable(
                "message.velorise_emc.last_echo.respawn_blocked"), true);
            return true;
        }

        buildRespawnFountain(level, center);
        data.resetAt(center);
        data.markBuilt();
        if (!ArenaSummonService.start(level, center, ArenaSummonService.BossKind.LAST_ECHO)) return true;
        if (!player.getAbilities().instabuild) eye.shrink(1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
            center.getX() + 0.5D, center.getY() + 2.0D, center.getZ() + 0.5D,
            60, 2.0D, 1.0D, 2.0D, 0.08D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
            center.getX() + 0.5D, center.getY() + 3.0D, center.getZ() + 0.5D,
            80, 2.5D, 2.0D, 2.5D, 0.10D);
        player.displayClientMessage(Component.translatable(
            "message.velorise_emc.last_echo.respawned"), true);
        return true;
    }

    /** Explicit operator action: places the complete arena now instead of using the normal tick budget. */
    public static int placeNow(ServerLevel level, BlockPos center) {
        LastEchoCitadelData data = LastEchoCitadelData.get(level.getServer());
        data.resetAt(center);
        int changed = 0;
        for (int dz = -GRID_RADIUS; dz <= GRID_RADIUS; dz++) for (int dx = -GRID_RADIUS; dx <= GRID_RADIUS; dx++) {
            buildColumn(level, center, dx, dz);
            changed++;
        }
        buildRespawnFountain(level, center);
        data.markBuilt();
        data.markDefeated();
        return changed;
    }

    public static boolean summonExisting(ServerLevel level, BlockPos center) {
        LastEchoCitadelData data = LastEchoCitadelData.get(level.getServer());
        if (!level.getEntitiesOfClass(EchoboundDragonEntity.class, new AABB(center).inflate(96.0D, 64.0D, 96.0D), e -> e.isAlive()).isEmpty()) return false;
        spawnBoss(level, data, center);
        return true;
    }

    public static EchoboundDragonEntity summonAt(ServerLevel level, BlockPos center) {
        LastEchoCitadelData data = LastEchoCitadelData.get(level.getServer());
        BlockPos resolved = data.built() && data.center() != null && data.center().distSqr(center) <= 72.0D * 72.0D
            ? data.center()
            : PromotionBossArenaService.snapPlacementFloor(level, center);
        data.resetAt(resolved);
        buildRespawnFountain(level, resolved);
        data.markBuilt();
        summonExisting(level, resolved);
        return level.getEntitiesOfClass(EchoboundDragonEntity.class, new AABB(resolved).inflate(96.0D, 64.0D, 96.0D), e -> e.isAlive())
            .stream().findFirst().orElse(null);
    }

    private static void spawnBoss(ServerLevel level, LastEchoCitadelData data, BlockPos center) {
        EchoboundDragonEntity boss = VeloriseEMC.ECHOBOUND_DRAGON.get().create(level);
        if (boss == null) return;
        boss.bindArena(center);
        boss.moveTo(center.getX() + 0.5D, center.getY() + 34.0D, center.getZ() + 0.5D, 0.0F, 0.0F);
        boss.addTag("velorise_emc.last_echo_boss");
        level.addFreshEntity(boss);
        data.setBossActive(true);
    }
}
