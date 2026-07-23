package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Portable crafting table. It deliberately has no Core level or nearby-inventory scan. */
public final class ResonantCraftingTableBlock extends CraftingTableBlock {
    public ResonantCraftingTableBlock(Properties properties) { super(properties); }
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.CRAFTING);
    }
}
