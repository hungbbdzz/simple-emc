package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class ResonantBlastFurnaceBlockEntity extends AbstractResonantFurnaceBlockEntity {
    public ResonantBlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(VeloriseEMC.RESONANT_BLAST_FURNACE_BE.get(), pos, state, RecipeType.BLASTING, Kind.BLAST_FURNACE);
    }
}
