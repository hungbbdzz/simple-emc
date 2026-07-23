package com.velorise.emc;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
public interface DeepboundStorageAccess extends Container {
    WirelessCoreTier getCoreTier();
    int getAccessibleSlotCount();
    int clampPage(int page);
    boolean stillValid(Player player);
    default void onMenuOpened(Player player) { startOpen(player); }
    default void onMenuClosed(Player player) { stopOpen(player); }
    default int lockedPlayerSlot() { return -1; }
}
