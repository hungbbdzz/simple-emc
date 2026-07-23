package com.velorise.emc;

import com.velorise.emc.common.CondensedCoreGeneratorProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pays nearby players once per second for every condensed-core generator in range. */
public final class CondensedCoreGeneratorService {
    private CondensedCoreGeneratorService() {}

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0 || !eligible(player)) return;

        BlockPos center = player.blockPosition();
        int radius = CondensedCoreGeneratorProfile.maximumRadius();
        long generated = 0L;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            Block block = player.serverLevel().getBlockState(pos).getBlock();
            int tier = tierOf(block);
            if (tier > 0) {
                generated += shareFor(player, pos, tier);
            }
        }

        EmcAccountUtil.add(player, generated);
    }

    /** A generator has one fixed output; nearby players split it instead of duplicating it. */
    private static long shareFor(ServerPlayer player, BlockPos generatorPos, int tier) {
        int radius = CondensedCoreGeneratorProfile.radius(tier);
        List<ServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer candidate : player.serverLevel().players()) {
            if (eligible(candidate) && inSquareRange(candidate.blockPosition(), generatorPos, radius)) {
                recipients.add(candidate);
            }
        }
        if (recipients.isEmpty()) return 0L;
        recipients.sort(Comparator.comparing(ServerPlayer::getUUID));

        int index = -1;
        for (int i = 0; i < recipients.size(); i++) {
            if (recipients.get(i).getUUID().equals(player.getUUID())) { index = i; break; }
        }
        if (index < 0) return 0L;

        long output = CondensedCoreGeneratorProfile.emcPerSecond(tier);
        long base = output / recipients.size();
        long remainder = output % recipients.size();
        return base + (index < remainder ? 1L : 0L);
    }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static boolean inSquareRange(BlockPos playerPos, BlockPos generatorPos, int radius) {
        return Math.abs(playerPos.getX() - generatorPos.getX()) <= radius
            && Math.abs(playerPos.getY() - generatorPos.getY()) <= radius
            && Math.abs(playerPos.getZ() - generatorPos.getZ()) <= radius;
    }

    private static int tierOf(Block block) {
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_I.get()) return 1;
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_II.get()) return 2;
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_III.get()) return 3;
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_IV.get()) return 4;
        return 0;
    }
}
