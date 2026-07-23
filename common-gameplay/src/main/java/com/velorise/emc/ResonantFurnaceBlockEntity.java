package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class ResonantFurnaceBlockEntity extends AbstractResonantFurnaceBlockEntity {
    public ResonantFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(VeloriseEMC.RESONANT_FURNACE_BE.get(), pos, state, RecipeType.SMELTING, Kind.FURNACE);
    }
}
