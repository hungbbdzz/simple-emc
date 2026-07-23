package com.velorise.emc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** NeoForge 1.21.1 custom-name data-component bridge. */
public final class ItemStackNameCompat {
    private ItemStackNameCompat() {}

    public static boolean hasCustomName(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    public static Component getCustomName(ItemStack stack) {
        return stack.getHoverName();
    }

    public static void copyEntityCustomName(Entity entity, ItemStack stack) {
        if (entity.hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, entity.getCustomName());
    }
}
