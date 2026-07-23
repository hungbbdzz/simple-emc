package com.velorise.emc;

import com.velorise.emc.common.ResonantMachineProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Item form of the Core-powered furnace family. */
public class ResonantBlockItem extends BlockItem {
    public ResonantBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override public Component getName(ItemStack stack) { return TieredMachineName.of(super.getName(stack), stack); }

        
}
