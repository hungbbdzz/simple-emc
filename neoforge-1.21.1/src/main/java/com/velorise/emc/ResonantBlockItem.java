package com.velorise.emc;

import com.velorise.emc.common.ResonantMachineProfile;
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

/** Item form of the Core-powered furnace family. */
public class ResonantBlockItem extends BlockItem {
    public ResonantBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override public Component getName(ItemStack stack) { return TieredMachineName.of(super.getName(stack), stack); }

        
}
