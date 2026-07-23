package com.velorise.emc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Persistent marker for non-damageable Vanilla items upgraded through Wireless Core. */
public final class SpecialUpgradeUtil {
    public static final String TAG_TYPE = "VeloriseEMCSpecialUpgrade";
    public static final String BOUND_COMPASS = "bound_compass";
    public static final String SOULBOUND_RECOVERY_COMPASS = "soulbound_recovery_compass";
    public static final String FAR_SIGHT_SPYGLASS = "far_sight_spyglass";
    private static final String TAG_SELECTION = "VeloriseEMCSelection";

    private SpecialUpgradeUtil() {}

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean is(ItemStack stack, String type) {
        return !stack.isEmpty() && type.equals(tag(stack).getString(TAG_TYPE));
    }

    public static void set(ItemStack stack, String type) {
        if (stack.isEmpty()) return;
        CompoundTag tag = tag(stack);
        tag.putString(TAG_TYPE, type);
        save(stack, tag);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    public static int getSelection(ItemStack stack) {
        return Math.floorMod(tag(stack).getInt(TAG_SELECTION), 6);
    }

    public static void setSelection(ItemStack stack, int selection) {
        if (stack.isEmpty()) return;
        CompoundTag tag = tag(stack);
        tag.putInt(TAG_SELECTION, Math.max(0, Math.min(5, selection)));
        save(stack, tag);
    }



    public static ItemStack createFixedVariant(ItemStack vanillaStack, String type, String nameKey) {
        ItemStack result = vanillaStack.copy();
        result.setCount(1);
        set(result, type);
        CoreTierData.set(result, com.velorise.emc.common.WirelessCoreTier.I);
        result.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey));
        return result;
    }

    public static ItemStack createVariant(ItemStack vanillaStack, String type, String nameKey) {
        ItemStack result = vanillaStack.copy();
        result.setCount(1);
        set(result, type);
        CoreTierData.set(result, com.velorise.emc.common.WirelessCoreTier.IV);
        result.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey));
        return result;
    }
}
