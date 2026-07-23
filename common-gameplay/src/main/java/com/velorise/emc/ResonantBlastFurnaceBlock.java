package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ResonantBlastFurnaceBlock extends AbstractResonantFurnaceBlock {
    public ResonantBlastFurnaceBlock(Properties properties) { super(properties); }
    @Override protected AbstractResonantFurnaceBlockEntity createMachine(BlockPos pos, BlockState state) { return new ResonantBlastFurnaceBlockEntity(pos, state); }
    @Override protected BlockEntityType<? extends AbstractResonantFurnaceBlockEntity> machineType() { return VeloriseEMC.RESONANT_BLAST_FURNACE_BE.get(); }
    @Override protected Item machineItem() { return VeloriseEMC.RESONANT_BLAST_FURNACE_ITEM.get(); }
}
