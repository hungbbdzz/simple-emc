package com.velorise.emc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

/** Version bridge for held-item break animation and sound. */
public final class ItemBreakCompat {
    private ItemBreakCompat() {}

    public static void broadcast(ServerPlayer player, InteractionHand hand, Item brokenItem) {
        player.broadcastBreakEvent(hand);
    }
}
