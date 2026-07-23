package com.velorise.emc;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

public class WaypointAnchorBlock extends Block {
    public static final MapCodec<WaypointAnchorBlock> CODEC = simpleCodec(WaypointAnchorBlock::new);

    public WaypointAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends WaypointAnchorBlock> codec() {
        return CODEC;
    }
}
