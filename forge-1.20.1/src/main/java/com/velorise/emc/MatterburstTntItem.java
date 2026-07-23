package com.velorise.emc;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Matterburst TNT is born with Core I and upgrades sequentially through Core IV. */
public final class MatterburstTntItem extends BlockItem implements CoreIIntegratedItem {
    public MatterburstTntItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(
            MatterburstTntBlock.CORE_LEVEL,
            Math.max(1, CoreTierData.installedLevel(context.getItemInHand()))
        );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
