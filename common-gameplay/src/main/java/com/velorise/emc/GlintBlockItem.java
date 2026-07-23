package com.velorise.emc;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Generic upgraded block item that always keeps the enchantment-style item glint. */
public class GlintBlockItem extends BlockItem {
    public GlintBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
