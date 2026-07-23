package com.velorise.emc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;

final class PortableStonecutterBlock extends StonecutterBlock {
    static final MapCodec<StonecutterBlock> CODEC = simpleCodec(PortableStonecutterBlock::new);
    PortableStonecutterBlock(Properties properties) { super(properties); }
    @Override public MapCodec<StonecutterBlock> codec() { return CODEC; }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.STONECUTTER);
    }
}

final class PortableLoomBlock extends LoomBlock {
    static final MapCodec<LoomBlock> CODEC = simpleCodec(PortableLoomBlock::new);
    PortableLoomBlock(Properties properties) { super(properties); }
    @Override public MapCodec<LoomBlock> codec() { return CODEC; }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.LOOM);
    }
}

final class PortableCartographyTableBlock extends CartographyTableBlock {
    static final MapCodec<CartographyTableBlock> CODEC = simpleCodec(PortableCartographyTableBlock::new);
    PortableCartographyTableBlock(Properties properties) { super(properties); }
    @Override public MapCodec<CartographyTableBlock> codec() { return CODEC; }
    @Override public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return PortableWorkstationUi.provider(PortableWorkstationItem.Kind.CARTOGRAPHY);
    }
}
