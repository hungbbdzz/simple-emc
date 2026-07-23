package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PassiveEmcTableBlockEntity extends BlockEntity {
    public PassiveEmcTableBlockEntity(BlockPos pos, BlockState state) {
        super(VeloriseEMC.PASSIVE_EMC_TABLE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PassiveEmcTableBlockEntity blockEntity) {
        // Passive EMC generation was removed. The block entity remains only for clean block compatibility.
    }

}
