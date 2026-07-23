package com.velorise.emc;

import com.velorise.emc.common.EmcSourceMode;
import com.velorise.emc.common.EmcTransactionMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative, atomic EMC spending service for personal items. */
public final class EmcTransactionService {
    private EmcTransactionService() {}

    public static boolean canSpend(ServerPlayer player, long cost) {
        if (cost <= 0L || player.getAbilities().instabuild) return true;
        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        EmcSourceMode mode = data.getSourceMode();
        long inventoryAvailable = inventoryAvailable(player, data);
        long fund = data.getEmc();
        long reserve = data.getSafetyThreshold();
        if (mode.usesInventoryFuel()) return inventoryAvailable >= cost;
        return EmcTransactionMath.canSpend(fund, cost, reserve);
    }

    public static boolean spend(ServerPlayer player, long cost) {
        if (cost <= 0L || player.getAbilities().instabuild) return true;
        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        EmcSourceMode mode = data.getSourceMode();
        long inventoryAvailable = inventoryAvailable(player, data);
        long fund = data.getEmc();
        long reserve = data.getSafetyThreshold();

        boolean success;
        if (mode.usesInventoryFuel()) {
            success = inventoryAvailable >= cost;
            if (success) consumeInventoryFuel(player, data, cost);
        } else {
            success = EmcTransactionMath.canSpend(fund, cost, reserve);
            if (success) data.setEmc(fund - cost);
        }

        if (success) {
            AdvancementHooks.onSpent(player, data, fund);
            player.setData(VeloriseEMC.PLAYER_EMC.get(), data);
            VeloriseEMC.syncPlayerData(player);
        }
        return success;
    }

    public static long inventoryAvailable(ServerPlayer player) {
        return inventoryAvailable(player, player.getData(VeloriseEMC.PLAYER_EMC.get()));
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
