package com.velorise.emc;

import net.minecraft.world.item.ItemStack;

public interface AlchemicalChargeable {
    default int getMinCharge(ItemStack stack) { return 0; }

    int getMaxCharge(ItemStack stack);

    default int getCharge(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("charge") : 0;
    }

    default void setCharge(ItemStack stack, int charge) {
        stack.getOrCreateTag().putInt("charge", charge);
    }
}
