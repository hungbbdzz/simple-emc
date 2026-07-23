package com.velorise.emc;

import com.velorise.emc.capability.PlayerEMCCapability;
import net.minecraft.server.level.ServerPlayer;

/** Server-side helpers for safely reading and mutating a player's EMC account. */
public final class EmcAccountUtil {
    private EmcAccountUtil() {}

    public static long get(ServerPlayer player) {
        return player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP)
            .map(PlayerEMC::getEmc)
            .orElse(0L);
    }

    public static boolean remove(ServerPlayer player, long amount) {
        if (amount <= 0L) return true;
        final boolean[] removed = {false};
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            long previousEmc = data.getEmc();
            removed[0] = data.removeEmc(amount);
            if (removed[0]) AdvancementHooks.onSpent(player, data, previousEmc);
        });
        if (removed[0]) VeloriseEMC.syncPlayerData(player);
        return removed[0];
    }

    public static void add(ServerPlayer player, long amount) {
        if (amount <= 0L) return;
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            long current = data.getEmc();
            long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
            data.setEmc(next);
            AdvancementHooks.checkEmcMilestones(player, data);
        });
        VeloriseEMC.syncPlayerData(player);
    }

    /** Transfers as much account EMC as possible into a placed machine buffer. */
    public static long transferToMachine(ServerPlayer player, ResonantMachine machine) {
        if (player == null || machine == null) return 0L;
        final long[] transferred = {0L};
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            long room = Math.max(0L, machine.getMaxStoredEmc() - machine.getStoredEmc());
            long spendable = Math.max(0L, data.getEmc() - data.getSafetyThreshold());
            long requested = Math.min(room, spendable);
            if (requested <= 0L) return;
            long accepted = machine.addEmc(requested);
            if (accepted <= 0L) return;
            long previousEmc = data.getEmc();
            data.setEmc(previousEmc - accepted);
            AdvancementHooks.onSpent(player, data, previousEmc);
            transferred[0] = accepted;
        });
        if (transferred[0] > 0L) VeloriseEMC.syncPlayerData(player);
        return transferred[0];
    }

    public static void learn(ServerPlayer player, net.minecraft.world.item.Item item) {
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> { if (EMCRegistry.canLearn(item)) data.learnItem(item); });
        VeloriseEMC.syncPlayerData(player);
    }

    public static void addAndLearn(ServerPlayer player, long amount, java.util.Collection<net.minecraft.world.item.Item> items) {
        if (amount <= 0L) return;
        player.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
            long current = data.getEmc();
            data.setEmc(current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
            for (var item : items) if (EMCRegistry.canLearn(item)) data.learnItem(item);
            AdvancementHooks.checkEmcMilestones(player, data);
        });
        VeloriseEMC.syncPlayerData(player);
    }
}
