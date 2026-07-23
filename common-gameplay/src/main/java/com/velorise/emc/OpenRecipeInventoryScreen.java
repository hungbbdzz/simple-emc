package com.velorise.emc;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;

/** Vanilla inventory that opens its crafting recipe book after Minecraft has initialized it. */
public final class OpenRecipeInventoryScreen extends InventoryScreen {
    public OpenRecipeInventoryScreen(Player player) {
        super(player);
    }

    @Override
    protected void init() {
        super.init();
        // InventoryScreen may redirect creative players to the creative inventory.
        // Only touch the recipe component when this screen completed initialization.
        if (this.minecraft != null && this.minecraft.screen == this
            && !getRecipeBookComponent().isVisible()) {
            getRecipeBookComponent().toggleVisibility();
        }
    }
}
