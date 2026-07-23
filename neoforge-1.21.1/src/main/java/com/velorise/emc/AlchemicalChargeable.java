package com.velorise.emc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public interface AlchemicalChargeable {
    default int getMinCharge(ItemStack stack) { return 0; }

    int getMaxCharge(ItemStack stack);

    default int getCharge(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getInt("charge");
        }
        return 0;
    }

    default void setCharge(ItemStack stack, int charge) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt("charge", charge);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
