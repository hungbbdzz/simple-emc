package com.velorise.emc;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Generic upgraded item that always keeps the enchantment-style item glint. */
public class GlintItem extends Item {
    public GlintItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
