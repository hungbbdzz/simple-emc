package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ResonantSmokerBlock extends AbstractResonantFurnaceBlock {
    public ResonantSmokerBlock(Properties properties) { super(properties); }
    @Override protected AbstractResonantFurnaceBlockEntity createMachine(BlockPos pos, BlockState state) { return new ResonantSmokerBlockEntity(pos, state); }
    @Override protected BlockEntityType<? extends AbstractResonantFurnaceBlockEntity> machineType() { return VeloriseEMC.RESONANT_SMOKER_BE.get(); }
    @Override protected Item machineItem() { return VeloriseEMC.RESONANT_SMOKER_ITEM.get(); }
}
