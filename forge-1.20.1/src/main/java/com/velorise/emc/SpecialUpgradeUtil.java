package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Persistent marker for non-damageable Vanilla items upgraded through Wireless Core. */
public final class SpecialUpgradeUtil {
    public static final String TAG_TYPE = "VeloriseEMCSpecialUpgrade";
    public static final String BOUND_COMPASS = "bound_compass";
    public static final String SOULBOUND_RECOVERY_COMPASS = "soulbound_recovery_compass";
    public static final String FAR_SIGHT_SPYGLASS = "far_sight_spyglass";
    private static final String TAG_SELECTION = "VeloriseEMCSelection";

    private SpecialUpgradeUtil() {}

    public static boolean is(ItemStack stack, String type) {
        return !stack.isEmpty() && stack.hasTag() && type.equals(stack.getTag().getString(TAG_TYPE));
    }

    public static void set(ItemStack stack, String type) {
        if (!stack.isEmpty()) stack.getOrCreateTag().putString(TAG_TYPE, type);
    }

    public static int getSelection(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return 0;
        return Math.floorMod(stack.getTag().getInt(TAG_SELECTION), 6);
    }

    public static void setSelection(ItemStack stack, int selection) {
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putInt(TAG_SELECTION, Math.max(0, Math.min(5, selection)));
        }
    }



    public static ItemStack createFixedVariant(ItemStack vanillaStack, String type, String nameKey) {
        ItemStack result = vanillaStack.copy();
        result.setCount(1);
        set(result, type);
        CoreTierData.set(result, com.velorise.emc.common.WirelessCoreTier.I);
        result.setHoverName(Component.translatable(nameKey));
        return result;
    }

    public static ItemStack createVariant(ItemStack vanillaStack, String type, String nameKey) {
        ItemStack result = vanillaStack.copy();
        result.setCount(1);
        set(result, type);
        CoreTierData.set(result, com.velorise.emc.common.WirelessCoreTier.IV);
        result.setHoverName(Component.translatable(nameKey));
        return result;
    }
}
