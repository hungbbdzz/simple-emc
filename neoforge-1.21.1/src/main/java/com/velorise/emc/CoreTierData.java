package com.velorise.emc;

import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Core identity for concrete Core items and installed Core markers on other stacks. */
public final class CoreTierData {
    public static final String TAG_TIER = "VeloriseEMCCoreTier";
    private CoreTierData() {}

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isCoreItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof WirelessCoreItem;
    }
    public static boolean isCoreItem(Item item) { return item instanceof WirelessCoreItem; }

    /**
     * Returns whether a stack has an effective Core tier. Items crafted with a
     * built-in Core I never expose a Core-0 state, even before an explicit data
     * component is written by the first upgrade.
     */
    public static boolean hasTier(ItemStack stack) {
        return isCoreItem(stack) || (stack != null && !stack.isEmpty()
            && (stack.getItem() instanceof CoreIIntegratedItem || tag(stack).contains(TAG_TIER)));
    }
    public static WirelessCoreTier get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WirelessCoreTier.I;
        if (stack.getItem() instanceof WirelessCoreItem core) return core.tier();
        return WirelessCoreTier.byLevel(tag(stack).getInt(TAG_TIER));
    }
    public static void set(ItemStack stack, WirelessCoreTier tier) {
        if (stack == null || stack.isEmpty() || isCoreItem(stack)) return;
        CompoundTag tag = tag(stack);
        tag.putInt(TAG_TIER, (tier == null ? WirelessCoreTier.I : tier).level());
        save(stack, tag);
    }
    /** Returns 0 when no Core is installed, otherwise the concrete Core level 1-4. */
    public static int installedLevel(ItemStack stack) {
        if (hasTier(stack)) return get(stack).level();
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof CoreIIntegratedItem ? 1 : 0;
    }

    public static int maximumMode(ItemStack stack) { return get(stack).maximumMode(); }

    public static Item coreItem(WirelessCoreTier tier) {
        return switch (tier == null ? WirelessCoreTier.I : tier) {
            case I -> VeloriseEMC.WIRELESS_CORE.get();
            case II -> VeloriseEMC.WIRELESS_CORE_T2.get();
            case III -> VeloriseEMC.WIRELESS_CORE_T3.get();
            case IV -> VeloriseEMC.WIRELESS_CORE_T4.get();
        };
    }
    public static ItemStack createCoreStack(WirelessCoreTier tier) { return new ItemStack(coreItem(tier)); }
}
