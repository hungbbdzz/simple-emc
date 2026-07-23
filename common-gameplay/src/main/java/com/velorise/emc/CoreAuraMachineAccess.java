package com.velorise.emc;

import com.velorise.emc.common.CoreAuraMachineProfile;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public interface CoreAuraMachineAccess extends Container, ResonantMachine {
    CoreAuraMachineProfile.Kind getMachineKind();
    int getCoreTierLevel();
    int getSelectedEffect();
    void setSelectedEffect(int selection);
    boolean isRunning();
    boolean stillValid(Player player);
    default void onMenuOpened(Player player) { startOpen(player); }
    default void onMenuClosed(Player player) { stopOpen(player); }
    default int lockedPlayerSlot() { return -1; }
}
