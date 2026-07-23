package com.velorise.emc;

import net.minecraft.server.level.ServerPlayer;

/** Owns only the mayfly flag it granted; Creative/Spectator or another provider is left untouched. */
public final class EchoboundArmorFlight {
    private static final String GRANTED = "velorise_emc:EchoboundFlightGranted";

    private EchoboundArmorFlight() {}

    public static void update(ServerPlayer player) {
        boolean fullSet = ResonantSetBonus.hasEchobound(player);
        boolean creativeFlight = player.isCreative() || player.isSpectator();
        boolean granted = player.getPersistentData().getBoolean(GRANTED);

        if (fullSet && !creativeFlight && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.getPersistentData().putBoolean(GRANTED, true);
            player.onUpdateAbilities();
        } else if (granted && (!fullSet || creativeFlight)) {
            player.getPersistentData().remove(GRANTED);
            if (!creativeFlight) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }
}
