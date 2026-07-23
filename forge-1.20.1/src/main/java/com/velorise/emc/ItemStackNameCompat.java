package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Forge 1.20.1 custom-name data bridge. */
public final class ItemStackNameCompat {
    private ItemStackNameCompat() {}

    public static boolean hasCustomName(ItemStack stack) {
        return stack.hasCustomHoverName();
    }

    public static Component getCustomName(ItemStack stack) {
        return stack.getHoverName();
    }

    public static void copyEntityCustomName(Entity entity, ItemStack stack) {
        if (entity.hasCustomName()) stack.setHoverName(entity.getCustomName());
    }
}
