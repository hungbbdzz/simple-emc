package com.velorise.emc;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A regular-looking workbench item; Core state is intentionally not added to its name. */
public class PurificationGrindstoneItem extends BlockItem {
    public PurificationGrindstoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
