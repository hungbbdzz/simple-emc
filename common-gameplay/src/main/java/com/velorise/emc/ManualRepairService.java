package com.velorise.emc;

import com.velorise.emc.common.CoreEquipmentProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative manual EMC repair. No durability is mutated from the player tick loop. */
public final class ManualRepairService {
    private ManualRepairService() {}

    public static void repair(ServerPlayer player, boolean allEquipped) {
        if (player.isUsingItem()) {
            player.displayClientMessage(Component.literal("§eFinish using the item before repairing it."), true);
            return;
        }

        List<ItemStack> targets = new ArrayList<>();
        addUnique(targets, player.getMainHandItem());
        if (allEquipped) {
            addUnique(targets, player.getOffhandItem());
            for (ItemStack stack : player.getInventory().armor) addUnique(targets, stack);
            ImbuedCompatibility.collectExternalEquipped(player, stack -> addUnique(targets, stack));
        }

        int eligible = 0;
        int repaired = 0;
        long spent = 0L;
        for (ItemStack stack : targets) {
            if (!ImbuedEquipmentUtil.isImbued(stack) || !stack.isDamaged()) continue;
            eligible++;
            long unitCost = CoreEquipmentProfile.repairEmcPerDurability(CoreTierData.get(stack).level());
            int points = affordableRepairPoints(player, stack.getDamageValue(), unitCost);
            if (points <= 0) continue;
            long cost = unitCost * (long) points;
            if (!EmcTransactionService.spend(player, cost)) continue;
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - points));
            repaired += points;
            spent += cost;
        }

        if (repaired > 0) {
            player.displayClientMessage(Component.literal(
                "§aRepaired §e" + repaired + "§a durability for §e" + spent + " EMC"), true);
        } else if (eligible == 0) {
            player.displayClientMessage(Component.literal(
                allEquipped ? "§7No damaged Imbued equipment is equipped." : "§7Hold a damaged Imbued item in the main hand."), true);
        } else {
            player.displayClientMessage(Component.literal("§cNot enough EMC to repair equipment."), true);
        }
    }

    private static int affordableRepairPoints(ServerPlayer player, int damage, long unitCost) {
        int high = Math.max(0, damage);
        if (unitCost <= 0L) return high;
        int low = 0;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            long cost = unitCost > Long.MAX_VALUE / mid ? Long.MAX_VALUE : unitCost * (long) mid;
            if (EmcTransactionService.canSpend(player, cost)) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    private static void addUnique(List<ItemStack> stacks, ItemStack candidate) {
        if (candidate == null || candidate.isEmpty()) return;
        for (ItemStack existing : stacks) if (existing == candidate) return;
        stacks.add(candidate);
    }
}
