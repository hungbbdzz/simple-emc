package com.velorise.emc;

import net.minecraft.world.level.block.Block;

/**
 * The Hourglass recipe already consumes Wireless Core I, so an untagged stack
 * is semantically Tier I rather than a separate Core-0 variant.
 */
public final class AlchemicalHourglassItem extends GlintBlockItem implements CoreIIntegratedItem {
    public AlchemicalHourglassItem(Block block, Properties properties) {
        super(block, properties);
    }
}
