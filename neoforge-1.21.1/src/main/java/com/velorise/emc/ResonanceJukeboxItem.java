package com.velorise.emc;

import com.velorise.emc.common.ResonanceJukeboxProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ResonanceJukeboxItem extends BlockItem {
    public ResonanceJukeboxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override public Component getName(ItemStack stack) { return TieredMachineName.of(super.getName(stack), stack); }

        
}
