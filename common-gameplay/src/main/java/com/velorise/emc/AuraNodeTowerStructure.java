package com.velorise.emc;

import com.velorise.emc.common.AuraNodeProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Shared procedural structure builder for the four world-generated Resonance Spires.
 *
 * <p>The builder intentionally uses only Vanilla blocks that exist on both supported
 * Minecraft targets. Each tier has a larger footprint, a taller frame, more support
 * pylons and a distinct material palette. The Resonance Spire remains accessible at the
 * center of the lower chamber while the energy beam and crown make the structure
 * readable from a distance.</p>
 */
public final class AuraNodeTowerStructure {

    private AuraNodeTowerStructure() {}

    public record PlacementResult(
        boolean success,
        int tier,
        BlockPos foundationCenter,
        BlockPos nodePos,
        int width,
        int height,
        int changedBlocks,
        String message
    ) {}

    public static int radiusForTier(int requestedTier) {
        return switch (AuraNodeProfile.clampTier(requestedTier)) {
            case 2 -> 4;
            case 3 -> 5;
            case 4 -> 7;
            default -> 3;
        };
    }

    public static int widthForTier(int requestedTier) {
        return radiusForTier(requestedTier) * 2 + 3;
    }

    public static int heightForTier(int requestedTier) {
        return switch (AuraNodeProfile.clampTier(requestedTier)) {
            case 2 -> 14;
            case 3 -> 19;
            case 4 -> 26;
            default -> 9;
        };
    }

    /** Small towers expose the Spire at ground level; only tiers III-IV use an upper sanctum. */
    public static int nodeOffsetY(int requestedTier) {
        int tier = AuraNodeProfile.clampTier(requestedTier);
        return tier <= 2 ? 2 : heightForTier(tier) - 4;
    }

    public static PlacementResult place(ServerLevel level, BlockPos foundationCenter, int requestedTier, boolean clearSpace) {
        int tier = AuraNodeProfile.clampTier(requestedTier);
        TowerSpec spec = spec(tier);
        int minY = foundationCenter.getY() - 2;
        int maxY = foundationCenter.getY() + spec.height + 1;
        if (minY < level.getMinBuildHeight() || maxY >= level.getMaxBuildHeight()) {
            return new PlacementResult(false, tier, foundationCenter, foundationCenter.above(nodeOffsetY(tier)),
                spec.radius * 2 + 3, spec.height, 0, "Tower would exceed the world build height.");
        }

        int changed = 0;
        if (clearSpace) changed += clearVolume(level, foundationCenter, spec);
        changed += buildFoundation(level, foundationCenter, spec);
        changed += buildCentralDais(level, foundationCenter, spec);
        changed += buildPylons(level, foundationCenter, spec);
        changed += buildFrameRings(level, foundationCenter, spec);
        changed += buildConnectorBands(level, foundationCenter, spec);
        changed += buildEnergySpine(level, foundationCenter, spec);
        changed += buildUpperBranches(level, foundationCenter, spec);
        // Build the authored crown after procedural branches so random limbs cannot
        // overwrite its inward-facing ribs.
        changed += buildCrown(level, foundationCenter, spec);
        changed += buildTierDetails(level, foundationCenter, spec);
        if (tier >= 3) changed += buildUpperSanctum(level, foundationCenter, spec);
        changed += buildEntranceAndStaircase(level, foundationCenter, spec);

        BlockPos nodePos = foundationCenter.above(nodeOffsetY(tier));
        BlockState nodeState = VeloriseEMC.RESONANCE_SPIRE.get().defaultBlockState()
            .setValue(AuraNodeBlock.TIER, tier)
            .setValue(AuraNodeBlock.ACTIVE, false);
        changed += set(level, nodePos, nodeState, Block.UPDATE_ALL);
        spawnTowerGuards(level, foundationCenter, spec);

        return new PlacementResult(true, tier, foundationCenter, nodePos,
            spec.radius * 2 + 3, spec.height, changed, "Resonance Spire placed.");
    }

    private static int clearVolume(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        for (int y = 1; y <= spec.height + 1; y++) {
            int clearanceRadius = spec.radius;
            for (int dx = -clearanceRadius; dx <= clearanceRadius; dx++) {
                for (int dz = -clearanceRadius; dz <= clearanceRadius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) > clearanceRadius) continue;
                    BlockPos pos = center.offset(dx, y, dz);
                    BlockState current = level.getBlockState(pos);
                    if (!current.isAir() && !current.is(VeloriseEMC.RESONANCE_SPIRE.get())) {
                        changed += set(level, pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return changed;
    }

    /** Four persistent vanilla mobs guard the cardinal approaches without occupying the stairwell. */
    private static void spawnTowerGuards(ServerLevel level, BlockPos center, TowerSpec spec) {
        String guardTag = "velorise_emc.tower_guard";
        AABB nearby = new AABB(center).inflate(spec.radius + 6.0D, spec.height + 4.0D, spec.radius + 6.0D);
        if (!level.getEntitiesOfClass(Mob.class, nearby, mob -> mob.getTags().contains(guardTag)).isEmpty()) return;

        if (spec.tier == 3) {
            spawnTierThreeShulkers(level, center, spec, guardTag);
            return;
        }

        int guardDistance = Math.max(2, spec.radius - 1);
        for (int index = 0; index < CARDINALS.length; index++) {
            int[] direction = CARDINALS[index];
            EntityType<? extends Mob> type = guardType(spec.tier, index);
            Mob guard = type.create(level);
            if (guard == null) continue;
            BlockPos spawn = center.offset(direction[0] * guardDistance, 1, direction[1] * guardDistance);
            guard.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
            guard.addTag(guardTag);
            guard.addTag("velorise_emc.tower_guard_tier_" + spec.tier);
            guard.setPersistenceRequired();
            if (spec.tier == 1) {
                equipTierOneGuard(guard, type);
            } else if (type == EntityType.PILLAGER) {
                guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            } else if (type == EntityType.WITHER_SKELETON) {
                guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            }
            level.addFreshEntity(guard);
        }
    }

    private static EntityType<? extends Mob> guardType(int tier, int index) {
        return switch (tier) {
            case 1 -> (index & 1) == 0 ? EntityType.ZOMBIE : EntityType.SKELETON;
            case 2 -> (index & 1) == 0 ? EntityType.BLAZE : EntityType.WITHER_SKELETON;
            case 4 -> EntityType.ENDERMAN;
            default -> EntityType.HUSK;
        };
    }

    /** Fixed early-game guard kit: visible protection, with predictable weapons. */
    private static void equipTierOneGuard(Mob guard, EntityType<? extends Mob> type) {
        guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        guard.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        guard.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        guard.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        guard.setItemSlot(EquipmentSlot.MAINHAND,
            new ItemStack(type == EntityType.SKELETON ? Items.BOW : Items.IRON_SWORD));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            guard.setDropChance(slot, 0.0F);
        }
    }

    /** Tier III is an End stronghold: four Shulkers below, four watching from the upper exterior. */
    private static void spawnTierThreeShulkers(ServerLevel level, BlockPos center, TowerSpec spec, String guardTag) {
        int lowerDistance = Math.max(2, spec.radius - 1);
        for (int[] direction : CARDINALS) {
            spawnTowerGuard(level, center.offset(direction[0] * lowerDistance, 1, direction[1] * lowerDistance),
                EntityType.SHULKER, spec.tier, guardTag);
        }

        int p = spec.pillarRadius;
        int topY = spec.height - 6;
        int[][] upperPositions = {{p + 1, 2}, {-p - 1, 2}, {2, p + 1}, {2, -p - 1}};
        for (int[] position : upperPositions) {
            spawnTowerGuard(level, center.offset(position[0], topY, position[1]),
                EntityType.SHULKER, spec.tier, guardTag);
        }
    }

    private static void spawnTowerGuard(ServerLevel level, BlockPos spawn, EntityType<? extends Mob> type,
                                        int tier, String guardTag) {
        Mob guard = type.create(level);
        if (guard == null) return;
        guard.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
            level.random.nextFloat() * 360.0F, 0.0F);
        guard.addTag(guardTag);
        guard.addTag("velorise_emc.tower_guard_tier_" + tier);
        guard.setPersistenceRequired();
        level.addFreshEntity(guard);
    }

    private static int buildFoundation(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        int radius = spec.radius;

        for (int depth = 2; depth >= 1; depth--) {
            int layerRadius = Math.max(1, radius - (2 - depth));
            BlockState state = depth == 2 ? spec.secondary : spec.primary;
            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    if (insideFootprint(dx, dz, layerRadius)) {
                        changed += set(level, center.offset(dx, -depth, dz), state, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!insideFootprint(dx, dz, radius)) continue;
                int edge = Math.max(Math.abs(dx), Math.abs(dz));
                BlockState state;
                if (edge == radius) {
                    state = spec.primary;
                } else if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) {
                    state = spec.accent;
                } else if (((dx + dz) & 1) == 0) {
                    state = spec.secondary;
                } else {
                    state = spec.primary;
                }
                changed += set(level, center.offset(dx, 0, dz), state, Block.UPDATE_CLIENTS);
            }
        }

        int gateHalfWidth = spec.tier >= 3 ? 2 : 1;
        for (int offset = -gateHalfWidth; offset <= gateHalfWidth; offset++) {
            changed += set(level, center.offset(offset, 0, -radius), spec.accent, Block.UPDATE_CLIENTS);
            changed += set(level, center.offset(offset, 0, radius), spec.accent, Block.UPDATE_CLIENTS);
            changed += set(level, center.offset(-radius, 0, offset), spec.accent, Block.UPDATE_CLIENTS);
            changed += set(level, center.offset(radius, 0, offset), spec.accent, Block.UPDATE_CLIENTS);
        }
        return changed;
    }

    private static int buildCentralDais(ServerLevel level, BlockPos center, TowerSpec spec) {
        // The two introductory towers need a walkable, uncluttered ground floor.
        // Keep only the centre support required by their low Spire position.
        if (spec.tier <= 2) {
            return set(level, center.above(), spec.accent, Block.UPDATE_CLIENTS);
        }
        int changed = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockState state = (Math.abs(dx) == 1 && Math.abs(dz) == 1) ? spec.primary : spec.accent;
                changed += set(level, center.offset(dx, 1, dz), state, Block.UPDATE_CLIENTS);
            }
        }

        if (spec.tier >= 3) {
            for (int[] direction : CARDINALS) {
                changed += set(level, center.offset(direction[0] * 2, 1, direction[1] * 2), spec.light, Block.UPDATE_CLIENTS);
            }
        }
        return changed;
    }

    private static int buildPylons(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        int p = spec.pillarRadius;
        int[][] positions = switch (spec.tier) {
            case 1 -> new int[][]{{-p, -p}, {-p, p}, {p, -p}, {p, p}};
            case 2 -> new int[][]{{-p, -p}, {-p, p}, {p, -p}, {p, p}, {-p, -1}, {-p, 1}, {p, -1}, {p, 1}};
            case 3 -> new int[][]{{-p, -p}, {-p, p}, {p, -p}, {p, p}, {-p, -2}, {-p, 2}, {p, -2}, {p, 2}, {-2, -p}, {2, -p}, {-2, p}, {2, p}};
            default -> new int[][]{{-p, -p}, {-p, p}, {p, -p}, {p, p}, {-p, -3}, {-p, 0}, {-p, 3}, {p, -3}, {p, 0}, {p, 3}, {-3, -p}, {0, -p}, {3, -p}, {-3, p}, {0, p}, {3, p}};
        };

        for (int index = 0; index < positions.length; index++) {
            int dx = positions[index][0];
            int dz = positions[index][1];
            int pillarHeight = spec.tier <= 2 ? spec.height - 3
                : spec.height - 3 - ((index & 1) == 0 ? 0 : Math.min(3, spec.tier));
            for (int y = 1; y <= pillarHeight; y++) {
                BlockState state;
                if (y == 1 || y == pillarHeight || y % 5 == 0) {
                    state = spec.accent;
                } else if (y % 3 == 0) {
                    state = spec.secondary;
                } else {
                    state = spec.primary;
                }
                changed += set(level, center.offset(dx, y, dz), state, Block.UPDATE_CLIENTS);
            }
            changed += set(level, center.offset(dx, pillarHeight + 1, dz), spec.light, Block.UPDATE_CLIENTS);
            changed += set(level, center.offset(dx, pillarHeight + 2, dz), spec.accent, Block.UPDATE_CLIENTS);
        }
        return changed;
    }

    private static int buildFrameRings(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        // More frequent structural belts keep the long tower sections visually connected.
        int[] heights = switch (spec.tier) {
            case 1 -> new int[]{4, spec.height - 3};
            case 2 -> new int[]{5, 8, 11};
            case 3 -> new int[]{5, 8, 11, 14, spec.height - 3};
            default -> new int[]{5, 8, 11, 14, 17, 20, spec.height - 3};
        };

        for (int index = 0; index < heights.length; index++) {
            int y = heights[index];
            int radius = Math.max(2, spec.pillarRadius - ((index & 1) == 0 ? 0 : 1));
            BlockState state = index == heights.length - 1 ? spec.accent
                : ((index & 1) == 0 ? spec.primary : spec.secondary);
            changed += ornateRing(level, center, radius, y, state, spec.light, spec.tier >= 3);

            // A second, slightly recessed course makes each belt read as a load-bearing band
            // instead of a single floating line of blocks.
            if (spec.tier >= 3 && y + 1 < spec.height - 1) {
                int innerRadius = Math.max(2, radius - 1);
                changed += ornateRing(level, center, innerRadius, y + 1,
                    spec.secondary, spec.light, false);
            }

            int braceLength = Math.max(2, radius - 1);
            for (int[] direction : CARDINALS) {
                for (int step = braceLength; step <= radius; step++) {
                    changed += set(level, center.offset(direction[0] * step, y,
                        direction[1] * step), step == radius ? spec.accent : spec.secondary,
                        Block.UPDATE_CLIENTS);
                }
            }
        }
        return changed;
    }

    /**
     * Locks the cut-corner rings into the outer pylons. Without these corner clasps the
     * tower reads as several disconnected cylinders, especially on the tall Tier IV form.
     */
    private static int buildConnectorBands(ServerLevel level, BlockPos center, TowerSpec spec) {
        if (spec.tier < 3) return 0;
        int changed = 0;
        int p = spec.pillarRadius;
        int[] heights = spec.tier == 3
            ? new int[]{6, 12, 16}
            : new int[]{6, 10, 14, 18, 22};

        for (int y : heights) {
            if (y >= spec.height - 1) continue;
            for (int[] diagonal : DIAGONALS) {
                int sx = diagonal[0];
                int sz = diagonal[1];

                // Thick L-shaped clasp from each corner pylon toward both sides of the ring.
                changed += set(level, center.offset(sx * p, y, sz * p), spec.accent, Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(sx * (p - 1), y, sz * p), spec.primary, Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(sx * p, y, sz * (p - 1)), spec.primary, Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(sx * (p - 1), y, sz * (p - 1)), spec.secondary, Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(sx * (p - 2), y, sz * (p - 1)), spec.secondary, Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(sx * (p - 1), y, sz * (p - 2)), spec.secondary, Block.UPDATE_CLIENTS);

                // Short raised cap gives the connection a wrapped, mechanical silhouette.
                changed += set(level, center.offset(sx * p, y + 1, sz * p), spec.primary, Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(sx * (p - 1), y + 1, sz * (p - 1)), spec.accent, Block.UPDATE_CLIENTS);
            }
        }
        return changed;
    }

    private static int buildEnergySpine(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        for (int y = 3; y <= spec.height - 5; y++) {
            // Tier III-IV no longer contain an installable core, so their old
            // magenta/cyan stained-glass energy column is obsolete. Preserve an
            // open central shaft while retaining lights in the surrounding cage.
            BlockState state = spec.tier >= 3
                ? Blocks.AIR.defaultBlockState()
                : (y % 5 == 0) ? spec.light : spec.beam;
            changed += set(level, center.above(y), state, Block.UPDATE_CLIENTS);
        }

        if (spec.tier >= 2) {
            int cageHeight = spec.height - 5;
            for (int y = 5; y <= cageHeight; y += 3) {
                for (int[] direction : CARDINALS) {
                    changed += set(level, center.offset(direction[0], y, direction[1]), spec.frame, Block.UPDATE_CLIENTS);
                }
            }
        }
        return changed;
    }

    private static int buildCrown(ServerLevel level, BlockPos center, TowerSpec spec) {
        if (spec.tier == 4) {
            return buildEncasingCrown(level, center, spec);
        }

        int changed = 0;
        int crownY = spec.height - 2;
        int armLength = 1 + spec.tier;

        changed += set(level, center.above(crownY), spec.light, Block.UPDATE_CLIENTS);
        for (int[] direction : CARDINALS) {
            for (int step = 1; step <= armLength; step++) {
                BlockState state = step == armLength ? spec.light : spec.accent;
                changed += set(level, center.offset(direction[0] * step, crownY,
                    direction[1] * step), state, Block.UPDATE_CLIENTS);
            }
            changed += set(level, center.offset(direction[0] * armLength, crownY + 1,
                direction[1] * armLength), spec.accent, Block.UPDATE_CLIENTS);
            if (spec.tier >= 3) {
                changed += set(level, center.offset(direction[0] * armLength, crownY + 2,
                    direction[1] * armLength), spec.light, Block.UPDATE_CLIENTS);
            }
        }
        changed += set(level, center.above(crownY + 1), spec.accent, Block.UPDATE_CLIENTS);
        if (spec.tier >= 3) {
            changed += set(level, center.above(crownY + 2), spec.light, Block.UPDATE_CLIENTS);
        }
        return changed;
    }

    /**
     * Four heavy ribs rise from the upper ring and curl inward around the Spire. The middle
     * remains open, matching the hand-built crown while keeping the altar and stair exit usable.
     */
    private static int buildEncasingCrown(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        int baseY = spec.height - 5;
        int p = Math.max(3, spec.pillarRadius - 1);
        int topY = spec.height + 1;

        changed += ornateRing(level, center, p, baseY, spec.primary, spec.light, true);
        changed += ornateRing(level, center, p - 1, baseY + 1, spec.secondary, spec.light, false);

        // Keep the exact center free above the Resonance Spire. Light is placed in four
        // surrounding sockets instead of replacing the functional block with a cap.
        for (int[] direction : CARDINALS) {
            changed += set(level, center.offset(direction[0] * 2, baseY + 1,
                direction[1] * 2), spec.light, Block.UPDATE_CLIENTS);
        }

        for (int[] diagonal : DIAGONALS) {
            int sx = diagonal[0];
            int sz = diagonal[1];
            int tangentX = -sz;
            int tangentZ = sx;

            // Broad root and two-block-wide upright rib.
            for (int y = baseY + 1; y <= baseY + 4; y++) {
                changed += set(level, center.offset(sx * p, y, sz * p), spec.primary, Block.UPDATE_CLIENTS);
                if (y <= baseY + 3) {
                    changed += set(level, center.offset(sx * p + tangentX, y,
                        sz * p + tangentZ), spec.secondary, Block.UPDATE_CLIENTS);
                }
            }

            // Stepped inward hook: p -> p-1 -> p-2 -> p-3. This creates the enclosing
            // claw shape visible from all four sides without sealing the center.
            changed += crownPair(level, center, spec, sx, sz, tangentX, tangentZ,
                p - 1, baseY + 4, spec.primary);
            changed += crownPair(level, center, spec, sx, sz, tangentX, tangentZ,
                p - 1, baseY + 5, spec.primary);
            changed += crownPair(level, center, spec, sx, sz, tangentX, tangentZ,
                p - 2, baseY + 6, spec.secondary);
            changed += set(level, center.offset(sx * Math.max(1, p - 3), topY,
                sz * Math.max(1, p - 3)), spec.accent, Block.UPDATE_CLIENTS);
        }

        return changed;
    }

    private static int crownPair(ServerLevel level, BlockPos center, TowerSpec spec,
                                 int sx, int sz, int tangentX, int tangentZ,
                                 int distance, int y, BlockState state) {
        int changed = 0;
        changed += set(level, center.offset(sx * distance, y, sz * distance), state, Block.UPDATE_CLIENTS);
        changed += set(level, center.offset(sx * distance + tangentX, y,
            sz * distance + tangentZ), state, Block.UPDATE_CLIENTS);
        return changed;
    }

    /**
     * Tree-like upper limbs break up the rectangular silhouette without filling the
     * tower interior. Higher tiers gain diagonal limbs, longer reach and brighter forks.
     */
    private static int buildUpperBranches(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        if (spec.tier <= 2) {
            // Tier I-II are deliberately authored rather than procedurally varied:
            // every cardinal arm is mirrored, leaving the tower's first floor open.
            int start = Math.max(1, spec.pillarRadius - 1);
            int end = spec.radius + (spec.tier == 1 ? 0 : 1);
            int baseY = spec.height - 6;
            for (int[] direction : CARDINALS) {
                changed += buildFixedBranch(level, center, spec, direction[0], direction[1], start, end, baseY);
            }
            if (spec.tier == 2) {
                for (int[] direction : DIAGONALS) {
                    changed += buildFixedBranch(level, center, spec, direction[0], direction[1], start + 1, end - 1, baseY - 1);
                }
            }
            return changed;
        }

        RandomSource random = RandomSource.create(level.getSeed() ^ center.asLong()
            ^ (0x9E3779B97F4A7C15L * spec.tier));
        int start = Math.max(2, spec.pillarRadius - 1);
        int branchIndex = 0;
        // All eight compass directions grow an outward chorus-like limb. The
        // low layer is restrained; upper layers become progressively denser.
        for (int[][] directions : new int[][][]{CARDINALS, DIAGONALS}) {
            for (int[] direction : directions) {
                int lowerY = 4 + random.nextInt(4);
                changed += buildBranch(level, center, spec, direction[0], direction[1], start,
                    spec.radius + random.nextInt(2), lowerY, branchIndex++, random);

                int upperY = spec.height - 9 + random.nextInt(4);
                changed += buildBranch(level, center, spec, direction[0], direction[1], start,
                    spec.radius + 2 + random.nextInt(3), upperY, branchIndex++, random);
            }
        }
        // Tier IV gets one extra middle canopy in every direction, still outside
        // the stairwell, so it reads as an overgrown sculk/chorus tree.
        if (spec.tier == 4) {
            for (int[][] directions : new int[][][]{CARDINALS, DIAGONALS}) {
                for (int[] direction : directions) {
                    changed += buildBranch(level, center, spec, direction[0], direction[1], start,
                        spec.radius + 1 + random.nextInt(3), 9 + random.nextInt(7), branchIndex++, random);
                }
            }
        }
        return changed;
    }

    /** A compact, mirrored limb for the fixed Tier I-II silhouettes. */
    private static int buildFixedBranch(ServerLevel level, BlockPos center, TowerSpec spec,
                                        int directionX, int directionZ, int start, int end, int baseY) {
        int changed = 0;
        for (int step = start; step <= end; step++) {
            int y = baseY + (step - start) / 2;
            BlockState state = step == end ? spec.light : ((step - start) % 2 == 0 ? spec.accent : spec.primary);
            changed += setBranch(level, center, spec, directionX * step, y, directionZ * step, state);
        }
        return changed;
    }

    private static int buildBranch(ServerLevel level, BlockPos center, TowerSpec spec,
                                   int directionX, int directionZ, int start, int end,
                                   int baseY, int branchIndex, RandomSource random) {
        int changed = 0;
        int tipX = 0;
        int tipY = baseY;
        int tipZ = 0;
        int perpendicularX = -Integer.signum(directionZ);
        int perpendicularZ = Integer.signum(directionX);
        int lateral = 0;
        for (int step = start; step <= end; step++) {
            if (step > start && random.nextFloat() < 0.32F) {
                lateral = Math.max(-2, Math.min(2, lateral + (random.nextBoolean() ? 1 : -1)));
            }
            int rise = Math.max(0, step - start + random.nextInt(3)) / 2;
            int x = directionX * step + perpendicularX * lateral;
            int z = directionZ * step + perpendicularZ * lateral;
            int y = baseY + rise;
            BlockState state = step == end ? spec.light
                : ((step + branchIndex) % 3 == 0 ? spec.accent : spec.primary);
            changed += setBranch(level, center, spec, x, y, z, state);
            if (step <= start + 1) {
                changed += setBranch(level, center, spec, x, y - 1, z, spec.secondary);
            }
            tipX = x;
            tipY = y;
            tipZ = z;
        }

        // A short perpendicular fork makes every limb read as a branch instead of a beam.
        int forkX = perpendicularX;
        int forkZ = perpendicularZ;
        int forkRise = 1 + random.nextInt(2);
        changed += setBranch(level, center, spec, tipX + forkX, tipY + forkRise, tipZ + forkZ, spec.accent);
        changed += setBranch(level, center, spec, tipX - forkX, tipY + 1, tipZ - forkZ, spec.light);
        if (spec.tier == 4 && random.nextBoolean()) {
            changed += setBranch(level, center, spec,
                tipX + directionX, tipY + forkRise + 1, tipZ + directionZ, spec.light);
        }
        if (spec.tier >= 3) {
            changed += growChorusCrown(level, center, spec, tipX, tipY, tipZ,
                directionX, directionZ, perpendicularX, perpendicularZ, random);
        }
        return changed;
    }

    /** Tall, forked continuation that makes Tier III-IV limbs read like chorus trees. */
    private static int growChorusCrown(ServerLevel level, BlockPos center, TowerSpec spec,
                                       int baseX, int baseY, int baseZ,
                                       int directionX, int directionZ,
                                       int perpendicularX, int perpendicularZ,
                                       RandomSource random) {
        int changed = 0;
        int reach = spec.tier == 4 ? 7 + random.nextInt(6) : 5 + random.nextInt(4);
        int x = baseX;
        int z = baseZ;
        for (int rise = 1; rise <= reach; rise++) {
            if (rise > 2 && rise % 3 == 0 && random.nextBoolean()) {
                x += directionX + (random.nextBoolean() ? perpendicularX : -perpendicularX);
                z += directionZ + (random.nextBoolean() ? perpendicularZ : -perpendicularZ);
            }
            int y = baseY + rise;
            BlockState state = rise == reach ? spec.light
                : (rise % 3 == 0 ? spec.accent : spec.primary);
            changed += setBranch(level, center, spec, x, y, z, state);
            if (rise >= 3 && rise % 3 == 0) {
                changed += setBranch(level, center, spec, x + perpendicularX, y + 1, z + perpendicularZ, spec.accent);
                changed += setBranch(level, center, spec, x - perpendicularX, y + 1, z - perpendicularZ, spec.light);
            }
        }
        return changed;
    }

    private static int setBranch(ServerLevel level, BlockPos center, TowerSpec spec,
                                 int dx, int y, int dz, BlockState state) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return 0;
        BlockPos pos = center.offset(dx, y, dz);
        if (Math.max(Math.abs(dx), Math.abs(dz)) > spec.radius && !level.getBlockState(pos).isAir()) return 0;
        return set(level, pos, state, Block.UPDATE_CLIENTS);
    }

    private static int buildTierDetails(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        int r = spec.radius;

        if (spec.tier >= 2) {
            int buttressHeight = 2 + spec.tier;
            for (int[] direction : DIAGONALS) {
                for (int step = 0; step < spec.tier; step++) {
                    int x = direction[0] * (r - step);
                    int z = direction[1] * (r - step);
                    for (int y = 1; y <= buttressHeight - step; y++) {
                        changed += set(level, center.offset(x, y, z), y == buttressHeight - step ? spec.accent : spec.secondary, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        if (spec.tier >= 3) {
            int balconyY = spec.tier == 3 ? 10 : 12;
            int balconyRadius = spec.tier == 3 ? 3 : 4;
            changed += ornateRing(level, center, balconyRadius, balconyY, spec.secondary, spec.light, false);
            for (int[] direction : CARDINALS) {
                changed += set(level, center.offset(direction[0] * balconyRadius, balconyY + 1, direction[1] * balconyRadius), spec.light, Block.UPDATE_CLIENTS);
            }
        }

        if (spec.tier == 4) {
            for (int[] direction : CARDINALS) {
                for (int step = 3; step <= 6; step++) {
                    int y = 2 + (step - 3) * 2;
                    changed += set(level, center.offset(direction[0] * step, y, direction[1] * step), spec.accent, Block.UPDATE_CLIENTS);
                    changed += set(level, center.offset(direction[0] * step, y + 1, direction[1] * step), spec.primary, Block.UPDATE_CLIENTS);
                }
            }
            for (int dx : new int[]{-5, 5}) {
                for (int dz : new int[]{-5, 5}) {
                    changed += set(level, center.offset(dx, 3, dz), spec.light, Block.UPDATE_CLIENTS);
                    changed += set(level, center.offset(dx, 4, dz), spec.accent, Block.UPDATE_CLIENTS);
                }
            }
            // Wardens are summoned by real vanilla shriekers when players make
            // noise, rather than being pre-spawned as unavoidable guards.
            int[][] shriekers = {{5, 5}, {5, -5}, {-5, 5}, {-5, -5}};
            for (int[] point : shriekers) {
                changed += set(level, center.offset(point[0], 1, point[1]),
                    Blocks.SCULK_SHRIEKER.defaultBlockState()
                        .setValue(SculkShriekerBlock.CAN_SUMMON, true), Block.UPDATE_CLIENTS);
                int sensorX = point[0] - Integer.signum(point[0]);
                int sensorZ = point[1] - Integer.signum(point[1]);
                changed += set(level, center.offset(sensorX, 1, sensorZ),
                    Blocks.SCULK_SENSOR.defaultBlockState(), Block.UPDATE_CLIENTS);
                changed += set(level, center.offset(point[0], 0, point[1]),
                    Blocks.SCULK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return changed;
    }

    /** Open reward/function platform reached by the helical stair, containing the EMC Spire itself. */
    private static int buildUpperSanctum(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        int floorY = nodeOffsetY(spec.tier) - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // The central cell used to be the solid core pedestal. The
                // Spire is now self-powered, so keep this vertical shaft open
                // instead of putting a floor block beneath it.
                if ((Math.abs(dx) == 2 && Math.abs(dz) == 2) || (dx == 0 && dz == 0)) continue;
                BlockState floor = (Math.abs(dx) + Math.abs(dz)) % 3 == 0 ? spec.accent : spec.secondary;
                changed += set(level, center.offset(dx, floorY, dz), floor, Block.UPDATE_CLIENTS);
            }
        }
        for (int[] direction : DIAGONALS) {
            changed += set(level, center.offset(direction[0] * 2, floorY + 1, direction[1]), spec.light, Block.UPDATE_CLIENTS);
            changed += set(level, center.offset(direction[0], floorY + 1, direction[1] * 2), spec.light, Block.UPDATE_CLIENTS);
        }
        return changed;
    }

    /** Ground entrance for tiers I-II; protected helical stair corridor for tiers III-IV. */
    private static int buildEntranceAndStaircase(ServerLevel level, BlockPos center, TowerSpec spec) {
        int changed = 0;
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            changed += buildCardinalEntrance(level, center, spec, direction);
        }

        if (spec.tier <= 2) {
            // Small towers expose the functional Spire above the central dais and
            // therefore need only the four ground-level approaches.
            return changed;
        }

        // Three rising steps per side. Every corner is a full flat landing at the
        // same base Y as the preceding stair, then the next side resumes climbing.
        int[][] stairPath = {
            {-1, -2}, {0, -2}, {1, -2},
            {2, -1}, {2, 0}, {2, 1},
            {1, 2}, {0, 2}, {-1, 2},
            {-2, 1}, {-2, 0}, {-2, -1}
        };
        int[][] cornerLandings = {
            {2, -2}, {2, 2}, {-2, 2}, {-2, -2}
        };

        // Reserve the complete route before placing any stair. Decorations are generated
        // procedurally, so a final corridor pass is the reliable way to prevent rings,
        // braces, balconies or tier-IV arms from occupying head space at a turn.
        int stairTopY = nodeOffsetY(spec.tier) - 1;
        for (int y = 1; y <= stairTopY; y++) {
            int index = Math.floorMod(y - 1, stairPath.length);
            BlockPos stairPos = center.offset(stairPath[index][0], y, stairPath[index][1]);
            changed += clearHeadroom(level, stairPos);
            if (index % 3 == 2) {
                int[] landing = cornerLandings[index / 3];
                BlockPos landingPos = center.offset(landing[0], y, landing[1]);
                changed += clearHeadroom(level, landingPos);
            }
        }

        for (int y = 1; y <= stairTopY; y++) {
            int index = Math.floorMod(y - 1, stairPath.length);
            int[] target = index % 3 == 2
                ? cornerLandings[index / 3]
                : stairPath[index + 1];
            int dx = target[0] - stairPath[index][0];
            int dz = target[1] - stairPath[index][1];
            Direction facing = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST
                : dz > 0 ? Direction.SOUTH : Direction.NORTH;
            BlockState stair = spec.stairs.setValue(StairBlock.FACING, facing);
            BlockPos stairPos = center.offset(stairPath[index][0], y, stairPath[index][1]);
            BlockPos supportPos = stairPos.below();
            if (level.getBlockState(supportPos).isAir()) {
                changed += set(level, supportPos, spec.primary, Block.UPDATE_CLIENTS);
            }
            changed += set(level, stairPos, stair, Block.UPDATE_CLIENTS);
            if (index % 3 == 2) {
                int[] landing = cornerLandings[index / 3];
                BlockPos landingPos = center.offset(landing[0], y, landing[1]);
                if (level.getBlockState(landingPos.below()).isAir()) {
                    changed += set(level, landingPos.below(), spec.primary, Block.UPDATE_CLIENTS);
                }
                changed += set(level, landingPos, spec.secondary, Block.UPDATE_CLIENTS);
            }
            if (y % 5 == 0) {
                int outerX = Integer.signum(stairPath[index][0]) * 3;
                int outerZ = Integer.signum(stairPath[index][1]) * 3;
                changed += set(level, center.offset(outerX, y, outerZ), spec.light, Block.UPDATE_CLIENTS);
            }
        }

        // Last, perform a complete three-block headroom sweep after every floor,
        // ring, light and catalyst has been generated. This removes the Tier III
        // end-stone-brick corner snag and any top-platform decoration that could
        // intrude into a turn after the stair itself has been placed.
        for (int y = 1; y <= stairTopY; y++) {
            int index = Math.floorMod(y - 1, stairPath.length);
            changed += clearHeadroom(level, center.offset(stairPath[index][0], y, stairPath[index][1]));
            if (index % 3 == 2) {
                int[] landing = cornerLandings[index / 3];
                changed += clearHeadroom(level, center.offset(landing[0], y, landing[1]));
            }
        }

        // The upper sanctum deliberately places diagonal lights one block above
        // its floor. At the final stair these lights coincide with the exit cell
        // on tier III and with both inward turns on tier IV. Clear the complete
        // transition after all stairs/landings have been placed so the last step
        // always opens onto the platform with two blocks of headroom.
        int finalIndex = Math.floorMod(stairTopY - 1, stairPath.length);
        int[] finalExit = finalIndex % 3 == 2
            ? cornerLandings[finalIndex / 3]
            : stairPath[finalIndex + 1];
        changed += clearHeadroom(level, center.offset(finalExit[0], stairTopY, finalExit[1]));
        if (finalExit[0] != 0) {
            changed += clearHeadroom(level, center.offset(
                finalExit[0] - Integer.signum(finalExit[0]), stairTopY, finalExit[1]));
        }
        if (finalExit[1] != 0) {
            changed += clearHeadroom(level, center.offset(
                finalExit[0], stairTopY, finalExit[1] - Integer.signum(finalExit[1])));
        }
        return changed;
    }

    private static int clearHeadroom(ServerLevel level, BlockPos floorPos) {
        int changed = 0;
        changed += set(level, floorPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        changed += set(level, floorPos.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        changed += set(level, floorPos.above(3), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        return changed;
    }

    /** Three-wide exterior step, doorway and approach in one cardinal direction. */
    private static int buildCardinalEntrance(ServerLevel level, BlockPos center, TowerSpec spec,
                                             Direction outward) {
        int changed = 0;
        int directionX = outward.getStepX();
        int directionZ = outward.getStepZ();
        int tangentX = -directionZ;
        int tangentZ = directionX;
        Direction inward = outward.getOpposite();
        BlockState entryStair = spec.stairs.setValue(StairBlock.FACING, inward);

        for (int lateral = -1; lateral <= 1; lateral++) {
            int sideX = tangentX * lateral;
            int sideZ = tangentZ * lateral;
            changed += set(level, center.offset(directionX * (spec.radius + 1) + sideX, 0,
                directionZ * (spec.radius + 1) + sideZ), entryStair, Block.UPDATE_CLIENTS);
            for (int y = 1; y <= 3; y++) {
                changed += set(level, center.offset(directionX * spec.radius + sideX, y,
                    directionZ * spec.radius + sideZ), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        for (int lateral : new int[]{-2, 2}) {
            int sideX = tangentX * lateral;
            int sideZ = tangentZ * lateral;
            for (int y = 1; y <= 4; y++) {
                changed += set(level, center.offset(directionX * spec.radius + sideX, y,
                    directionZ * spec.radius + sideZ), y == 4 ? spec.light : spec.primary, Block.UPDATE_CLIENTS);
            }
        }
        for (int lateral = -1; lateral <= 1; lateral++) {
            changed += set(level, center.offset(directionX * spec.radius + tangentX * lateral, 4,
                directionZ * spec.radius + tangentZ * lateral), spec.accent, Block.UPDATE_CLIENTS);
        }

        for (int distance = spec.radius; distance >= 3; distance--) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                int x = directionX * distance + tangentX * lateral;
                int z = directionZ * distance + tangentZ * lateral;
                changed += set(level, center.offset(x, 0, z), lateral == 0 ? spec.secondary : spec.primary,
                    Block.UPDATE_CLIENTS);
                for (int y = 1; y <= 3; y++) {
                    changed += set(level, center.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return changed;
    }

    private static int ornateRing(ServerLevel level, BlockPos center, int radius, int y,
                                  BlockState state, BlockState light, boolean cornerLights) {
        int changed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius || !insideFootprint(dx, dz, radius)) continue;
                changed += set(level, center.offset(dx, y, dz), state, Block.UPDATE_CLIENTS);
            }
        }
        if (cornerLights) {
            int corner = Math.max(1, radius - Math.max(1, radius / 3));
            for (int[] direction : DIAGONALS) {
                changed += set(level, center.offset(direction[0] * corner, y, direction[1] * radius), light, Block.UPDATE_CLIENTS);
            }
        }
        return changed;
    }

    private static boolean insideFootprint(int dx, int dz, int radius) {
        int cut = Math.max(1, radius / 3);
        return Math.max(Math.abs(dx), Math.abs(dz)) <= radius
            && Math.abs(dx) + Math.abs(dz) <= radius * 2 - cut;
    }

    private static int set(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        if (level.getBlockState(pos).equals(state)) return 0;
        level.setBlock(pos, state, flags);
        return 1;
    }

    private static TowerSpec spec(int tier) {
        return switch (tier) {
            case 2 -> new TowerSpec(
                tier, 4, 14, 3,
                Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                Blocks.SHROOMLIGHT.defaultBlockState(),
                Blocks.ORANGE_STAINED_GLASS.defaultBlockState(),
                Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState()
            );
            case 3 -> new TowerSpec(
                tier, 5, 19, 4,
                Blocks.END_STONE_BRICKS.defaultBlockState(),
                Blocks.PURPUR_BLOCK.defaultBlockState(),
                Blocks.PURPUR_PILLAR.defaultBlockState(),
                Blocks.OBSIDIAN.defaultBlockState(),
                Blocks.SEA_LANTERN.defaultBlockState(),
                Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(),
                Blocks.PURPUR_STAIRS.defaultBlockState()
            );
            case 4 -> new TowerSpec(
                tier, 7, 26, 6,
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                Blocks.REINFORCED_DEEPSLATE.defaultBlockState(),
                Blocks.SCULK.defaultBlockState(),
                Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                Blocks.SCULK_CATALYST.defaultBlockState(),
                Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
            );
            default -> new TowerSpec(
                1, 3, 9, 2,
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.GLOWSTONE.defaultBlockState(),
                Blocks.LIME_STAINED_GLASS.defaultBlockState(),
                Blocks.STONE_BRICK_STAIRS.defaultBlockState()
            );
        };
    }

    private static final int[][] CARDINALS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private record TowerSpec(
        int tier,
        int radius,
        int height,
        int pillarRadius,
        BlockState primary,
        BlockState secondary,
        BlockState accent,
        BlockState frame,
        BlockState light,
        BlockState beam,
        BlockState stairs
    ) {}
}
