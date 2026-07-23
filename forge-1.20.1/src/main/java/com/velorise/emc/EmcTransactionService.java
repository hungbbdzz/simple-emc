package com.velorise.emc;

import com.velorise.emc.capability.PlayerEMCCapability;
import com.velorise.emc.common.EmcSourceMode;
import com.velorise.emc.common.EmcTransactionMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Server-authoritative EMC spending service for personal items.
 *
 * <p>The operation is atomic: inventory fuel is only consumed after every required
 * source has been checked. Partial payments are never lost on failure.</p>
 */
public final class EmcTransactionService {
    private EmcTransactionService() {}

    public static boolean canSpend(ServerPlayer player, long cost) {
        if (cost <= 0L || player.getAbilities().instabuild) return true;
        final boolean[] result = {false};
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data ->
            result[0] = canSpend(player, data, cost));
        return result[0];
    }

    public static boolean spend(ServerPlayer player, long cost) {
        if (cost <= 0L || player.getAbilities().instabuild) return true;
        final boolean[] result = {false};
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            long previousEmc = data.getEmc();
            result[0] = spend(player, data, cost);
            if (result[0]) AdvancementHooks.onSpent(player, data, previousEmc);
        });
        if (result[0]) VeloriseEMC.syncPlayerData(player);
        return result[0];
    }

    private static boolean canSpend(ServerPlayer player, PlayerEMC data, long cost) {
        EmcSourceMode mode = data.getSourceMode();
        long inventoryAvailable = inventoryAvailable(player, data);
        long fund = data.getEmc();
        long reserve = data.getSafetyThreshold();
        if (mode.usesInventoryFuel()) return inventoryAvailable >= cost;
        return EmcTransactionMath.canSpend(fund, cost, reserve);
    }

    private static boolean spend(ServerPlayer player, PlayerEMC data, long cost) {
        EmcSourceMode mode = data.getSourceMode();
        long inventoryAvailable = inventoryAvailable(player, data);
        long fund = data.getEmc();
        long reserve = data.getSafetyThreshold();

        if (mode.usesInventoryFuel()) {
            if (inventoryAvailable < cost) return false;
            consumeInventoryFuel(player, data, cost);
            return true;
        }

        if (!EmcTransactionMath.canSpend(fund, cost, reserve)) return false;
        data.setEmc(fund - cost);
        return true;
    }

    public static long inventoryAvailable(ServerPlayer player) {
        final long[] value = {0L};
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> value[0] = inventoryAvailable(player, data));
        return value[0];
    }

    private static long inventoryAvailable(ServerPlayer player, PlayerEMC data) {
        long total = data.getInventoryFuelBuffer();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            long each = EMCRegistry.getAlchemicalFuelEMC(stack);
            if (each <= 0L || stack.isEmpty()) continue;
            long amount = each > Long.MAX_VALUE / stack.getCount() ? Long.MAX_VALUE : each * stack.getCount();
            total = total > Long.MAX_VALUE - amount ? Long.MAX_VALUE : total + amount;
        }
        return total;
    }

    private static void consumeInventoryFuel(ServerPlayer player, PlayerEMC data, long cost) {
        long buffer = data.getInventoryFuelBuffer();
        if (buffer >= cost) {
            data.setInventoryFuelBuffer(buffer - cost);
            return;
        }

        long remaining = cost - buffer;
        data.setInventoryFuelBuffer(0L);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0L; slot++) {
            ItemStack stack = inventory.getItem(slot);
            long each = EMCRegistry.getAlchemicalFuelEMC(stack);
            if (each <= 0L || stack.isEmpty()) continue;
            while (!stack.isEmpty() && remaining > 0L) {
                stack.shrink(1);
                if (each >= remaining) {
                    data.setInventoryFuelBuffer(each - remaining);
                    remaining = 0L;
                } else {
                    remaining -= each;
                }
            }
        }
    }
}
