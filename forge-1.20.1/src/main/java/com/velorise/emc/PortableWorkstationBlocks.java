package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;

final class PortableStonecutterBlock extends StonecutterBlock {
    PortableStonecutterBlock(Properties properties) { super(properties); }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.STONECUTTER);
    }
}

final class PortableLoomBlock extends LoomBlock {
    PortableLoomBlock(Properties properties) { super(properties); }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.LOOM);
    }
}

final class PortableCartographyTableBlock extends CartographyTableBlock {
    PortableCartographyTableBlock(Properties properties) { super(properties); }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.CARTOGRAPHY);
    }
}
