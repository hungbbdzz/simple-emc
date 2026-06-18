package com.velorise.simpleemc.compat.jade;

import com.velorise.simpleemc.EMCRegistry;
import com.velorise.simpleemc.AlchemicalHourglassBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class SimpleEMCJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 1. Generic EMC tooltip for items/blocks
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                ItemStack stack = accessor.getPickedResult();
                if (!stack.isEmpty()) {
                    // Skip displaying base item EMC if it is the placed Alchemical Hourglass block
                    if (accessor.getBlock() instanceof AlchemicalHourglassBlock) {
                        return;
                    }
                    long emc = EMCRegistry.getEMC(stack.getItem());
                    if (emc > 0) {
                        tooltip.add(Component.literal("§eEMC: " + String.format("%,d", emc)));
                    }
                }
            }

            @Override
            public ResourceLocation getUid() {
                return new ResourceLocation("simpleemc", "emc");
            }
        }, Block.class);

        // 2. Specific Alchemical Hourglass status tooltip (Reads synced NBT from Jade serverData accessor)
        registration.registerBlockComponent(new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                if (accessor.getBlock() instanceof AlchemicalHourglassBlock) {
                    CompoundTag tag = accessor.getServerData();
                    if (tag.contains("emc")) {
                        long storedEMC = tag.getLong("emc");
                        boolean isActive = storedEMC > 0;
                        
                        // Display status: Active/Inactive
                        if (isActive) {
                            tooltip.add(Component.literal("§7Status: §aActive"));
                        } else {
                            tooltip.add(Component.literal("§7Status: §cInactive"));
                        }
                        
                        // Display stored EMC
                        tooltip.add(Component.literal("§7Stored EMC: §e" + String.format("%,d", storedEMC) + " EMC"));
                    }
                }
            }

            @Override
            public ResourceLocation getUid() {
                return new ResourceLocation("simpleemc", "hourglass");
            }
        }, AlchemicalHourglassBlock.class);
    }
}
