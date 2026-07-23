package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

/** Server-side interaction helpers used by all local-buffer machines. */
public final class ResonantMachineInteraction {
    private ResonantMachineInteraction() {}

    public static boolean tryLoadFuel(Level level, BlockPos pos, Player player, ItemStack held, ResonantMachine machine) {
        long value = EMCRegistry.getAlchemicalFuelEMC(held);
        if (value <= 0L) return false;

        long room = Math.max(0L, machine.getMaxStoredEmc() - machine.getStoredEmc());
        if (room <= 0L) {
            player.displayClientMessage(Component.literal("§eEMC buffer is full"), true);
            return true;
        }

        int requested = player.isShiftKeyDown() ? held.getCount() : 1;
        long maxItemsByRoom = room / value;
        if (maxItemsByRoom <= 0L) {
            player.displayClientMessage(Component.literal("§eNot enough buffer space for this fuel"), true);
            return true;
        }
        int items = (int) Math.min((long) requested, maxItemsByRoom);
        long accepted = machine.addEmc(saturatingMultiply(value, items));
        if (accepted <= 0L) return true;

        if (!player.getAbilities().instabuild) held.shrink(items);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.45F, 1.35F);
        showStatus(player, machine, "Loaded " + accepted + " EMC");
        return true;
    }

    public static void showStatus(Player player, ResonantMachine machine, String prefix) {
        String state = machine.isMachineEnabled() ? "ON" : "OFF";
        player.displayClientMessage(Component.literal("§b" + prefix + " §7| §fEMC "
            + machine.getStoredEmc() + "/" + machine.getMaxStoredEmc()
            + " §7| §f" + state), true);
    }

    private static long saturatingMultiply(long a, long b) {
        if (a <= 0L || b <= 0L) return 0L;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }
}
