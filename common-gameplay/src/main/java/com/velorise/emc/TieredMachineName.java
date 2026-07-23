package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Consistent item names for machines whose installed Core is stored on the stack. */
public final class TieredMachineName {
    private TieredMachineName() {}

    public static Component of(Component baseName, ItemStack stack) {
        String name = baseName.getString();
        if (name.endsWith(" I") || name.endsWith(" II") || name.endsWith(" III") || name.endsWith(" IV")) {
            return baseName;
        }
        int tier = CoreTierData.get(stack).level();
        if (tier <= 1) return baseName;
        String roman = switch (tier) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "I"; };
        return baseName.copy().append(" " + roman);
    }
}
