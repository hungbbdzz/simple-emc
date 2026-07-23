package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Occasional low-frequency flavor text for failed EMC purchases. */
public final class EmcFailureMessages {
    private static final String[] FLAVOR = {
        "Insufficient magical funding.",
        "Your EMC account declined the transaction.",
        "Power requires purchasing power."
    };

    private EmcFailureMessages() {}

    public static void show(ServerPlayer player, long cost) {
        String message = player.getRandom().nextInt(4) == 0
            ? FLAVOR[player.getRandom().nextInt(FLAVOR.length)]
            : "Not enough EMC.";
        player.displayClientMessage(Component.literal("§c" + message + " §7Need §e" + cost + " EMC"), true);
        player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
        AdvancementHooks.onFailedSpend(player, EmcAccountUtil.get(player));
    }
}
