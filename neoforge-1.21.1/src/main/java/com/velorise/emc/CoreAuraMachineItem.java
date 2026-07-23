package com.velorise.emc;

import com.velorise.emc.common.CoreAuraMachineProfile;
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

public final class CoreAuraMachineItem extends BlockItem {
    private final CoreAuraMachineProfile.Kind kind;
    public CoreAuraMachineItem(Block block, Properties properties, CoreAuraMachineProfile.Kind kind) {
        super(block, properties.stacksTo(1));
        this.kind = kind;
    }
    @Override public boolean isFoil(ItemStack stack) { return true; }
        
}
