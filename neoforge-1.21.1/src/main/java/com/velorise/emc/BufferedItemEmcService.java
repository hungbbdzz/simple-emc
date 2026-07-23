package com.velorise.emc;

import com.velorise.emc.common.EmcTransactionMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Atomic EMC payment for personal items that own an internal EMC buffer.
 *
 * <p>Fund mode transfers only the exact shortage. Inventory mode consumes whole
 * alchemical fuel items and leaves the converted excess inside the powered item.</p>
 */
public final class BufferedItemEmcService {
    private BufferedItemEmcService() {}

    public static boolean spend(ServerPlayer player, long cost, LongSupplier storedGetter, LongConsumer storedSetter) {
        if (cost <= 0L || player.getAbilities().instabuild) return true;

        long stored = Math.max(0L, storedGetter.getAsLong());
        if (stored >= cost) {
            storedSetter.accept(stored - cost);
            return true;
        }

        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        if (data.getSourceMode().usesInventoryFuel()) {
            FuelPlan plan = planInventoryFuel(player.getInventory(), stored, cost);
            if (plan == null || !validatePlan(player.getInventory(), plan)) return false;
            commitPlan(player.getInventory(), plan);
            storedSetter.accept(saturatingAdd(stored, plan.convertedEmc()) - cost);
            return true;
        }

        long shortage = cost - stored;
        long fund = data.getEmc();
        if (!EmcTransactionMath.canSpend(fund, shortage, data.getSafetyThreshold())) return false;
        data.setEmc(fund - shortage);
        AdvancementHooks.onSpent(player, data, fund);
        player.setData(VeloriseEMC.PLAYER_EMC.get(), data);
        storedSetter.accept(0L);
        VeloriseEMC.syncPlayerData(player);
        return true;
    }

    private static FuelPlan planInventoryFuel(Inventory inventory, long stored, long cost) {
        long available = stored;
        List<FuelWithdrawal> withdrawals = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize() && available < cost; slot++) {
            ItemStack stack = inventory.getItem(slot);
            long each = EMCRegistry.getAlchemicalFuelEMC(stack);
            if (stack.isEmpty() || each <= 0L) continue;

            long needed = cost - available;
            long unitsNeeded = ((needed - 1L) / each) + 1L;
            int count = (int)Math.min((long)stack.getCount(), unitsNeeded);
            if (count <= 0) continue;
            withdrawals.add(new FuelWithdrawal(slot, count, each));
            available = saturatingAdd(available, saturatingMultiply(each, count));
        }
        if (available < cost) return null;
        return new FuelPlan(withdrawals, available - stored);
    }

    private static boolean validatePlan(Inventory inventory, FuelPlan plan) {
        for (FuelWithdrawal withdrawal : plan.withdrawals()) {
            ItemStack stack = inventory.getItem(withdrawal.slot());
            if (stack.getCount() < withdrawal.count()
                || EMCRegistry.getAlchemicalFuelEMC(stack) != withdrawal.unitEmc()) return false;
        }
        return true;
    }

    private static void commitPlan(Inventory inventory, FuelPlan plan) {
        for (FuelWithdrawal withdrawal : plan.withdrawals()) {
            inventory.getItem(withdrawal.slot()).shrink(withdrawal.count());
        }
        inventory.setChanged();
    }

    private static long saturatingAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    private static long saturatingMultiply(long value, int count) {
        return value > Long.MAX_VALUE / count ? Long.MAX_VALUE : value * count;
    }

    private record FuelWithdrawal(int slot, int count, long unitEmc) {}
    private record FuelPlan(List<FuelWithdrawal> withdrawals, long convertedEmc) {}
}
