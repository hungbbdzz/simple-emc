package com.velorise.emc;

import net.minecraft.server.level.ServerPlayer;

/** Server-side helpers for safely reading and mutating a player's EMC account. */
public final class EmcAccountUtil {
    private EmcAccountUtil() {}

    public static long get(ServerPlayer player) {
        return player.getData(VeloriseEMC.PLAYER_EMC.get()).getEmc();
    }

    public static boolean remove(ServerPlayer player, long amount) {
        if (amount <= 0L) return true;
        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        long previousEmc = data.getEmc();
        if (!data.removeEmc(amount)) return false;
        AdvancementHooks.onSpent(player, data, previousEmc);
        VeloriseEMC.syncPlayerData(player);
        return true;
    }

    public static void add(ServerPlayer player, long amount) {
        if (amount <= 0L) return;
        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        long current = data.getEmc();
        data.setEmc(current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
        AdvancementHooks.checkEmcMilestones(player, data);
        VeloriseEMC.syncPlayerData(player);
    }

    /** Transfers as much account EMC as possible into a placed machine buffer. */
    public static long transferToMachine(ServerPlayer player, ResonantMachine machine) {
        if (player == null || machine == null) return 0L;
        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        long room = Math.max(0L, machine.getMaxStoredEmc() - machine.getStoredEmc());
        long spendable = Math.max(0L, data.getEmc() - data.getSafetyThreshold());
        long requested = Math.min(room, spendable);
        if (requested <= 0L) return 0L;
        long accepted = machine.addEmc(requested);
        if (accepted <= 0L) return 0L;
        long previousEmc = data.getEmc();
        data.setEmc(previousEmc - accepted);
        AdvancementHooks.onSpent(player, data, previousEmc);
        player.setData(VeloriseEMC.PLAYER_EMC.get(), data);
        VeloriseEMC.syncPlayerData(player);
        return accepted;
    }

    public static void learn(ServerPlayer player, net.minecraft.world.item.Item item) {
        if (EMCRegistry.canLearn(item)) player.getData(VeloriseEMC.PLAYER_EMC.get()).learnItem(item);
        VeloriseEMC.syncPlayerData(player);
    }

    public static void addAndLearn(ServerPlayer player, long amount, java.util.Collection<net.minecraft.world.item.Item> items) {
        if (amount <= 0L) return;
        PlayerEMC data = player.getData(VeloriseEMC.PLAYER_EMC.get());
        long current = data.getEmc();
        data.setEmc(current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
        for (var item : items) if (EMCRegistry.canLearn(item)) data.learnItem(item);
        AdvancementHooks.checkEmcMilestones(player, data);
        VeloriseEMC.syncPlayerData(player);
    }
}
