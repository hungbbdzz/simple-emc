package com.velorise.emc.compat.jade;

import com.velorise.emc.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VeloriseUniversalJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("velorise_emc", "universal");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof AbstractResonantFurnaceBlockEntity furnace) {
            tag.putInt("CoreLevel", furnace.getCoreTierLevel());
            ItemStack input = furnace.getItem(0);
            ItemStack fuel = furnace.getItem(1);
            ItemStack output = furnace.getItem(2);
            if (!input.isEmpty()) tag.put("Input", input.saveOptional(accessor.getLevel().registryAccess()));
            if (!fuel.isEmpty()) tag.put("Fuel", fuel.saveOptional(accessor.getLevel().registryAccess()));
            if (!output.isEmpty()) tag.put("Output", output.saveOptional(accessor.getLevel().registryAccess()));
            tag.putInt("CookTime", furnace.getCookingProgress());
            tag.putInt("CookTimeTotal", furnace.getCookingTotalTime());
            tag.putBoolean("IsLit", furnace.isVisuallyLit());
        } else if (be instanceof AlchemicalHourglassBlockEntity hourglass) {
            tag.putInt("CoreLevel", hourglass.getInstalledCoreLevel());
            tag.putLong("EMC", hourglass.getEMC());
        } else if (be instanceof ResonanceJukeboxBlockEntity jukebox) {
            tag.putInt("CoreLevel", jukebox.getCoreTierLevel());
        } else if (be instanceof CoreAuraMachineBlockEntity machine) {
            tag.putInt("CoreLevel", machine.getCoreTierLevel());
            tag.putLong("EMC", machine.getStoredEmc());
        } else if (be instanceof AuraNodeBlockEntity spire) {
            tag.putInt("CoreLevel", spire.getTier());
        }

        if (!tag.contains("CoreLevel") && accessor.getBlock() instanceof AlchemicalAnvilBlock) {
            String blockId = accessor.getBlock().getDescriptionId();
            if (blockId.contains("iv")) tag.putInt("CoreLevel", 4);
            else if (blockId.contains("iii")) tag.putInt("CoreLevel", 3);
            else if (blockId.contains("ii")) tag.putInt("CoreLevel", 2);
            else tag.putInt("CoreLevel", 1);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();

        if (tag.contains("CoreLevel")) {
            int coreLevel = tag.getInt("CoreLevel");
            String coreName = switch (coreLevel) {
                case 2 -> "Infernal Core II";
                case 3 -> "End Core III";
                case 4 -> "Echoing Core IV";
                default -> "Overworld Core I";
            };
            tooltip.add(Component.literal("§dCore: " + coreName));
        }

        if (tag.contains("EMC")) {
            long emc = tag.getLong("EMC");
            tooltip.add(Component.literal("§eEMC: " + String.format("%,d", emc)));
        }

        if (tag.contains("Input") || tag.contains("Fuel") || tag.contains("Output")) {
            ItemStack input = tag.contains("Input") ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), tag.getCompound("Input")) : ItemStack.EMPTY;
            ItemStack fuel = tag.contains("Fuel") ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), tag.getCompound("Fuel")) : ItemStack.EMPTY;
            ItemStack output = tag.contains("Output") ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), tag.getCompound("Output")) : ItemStack.EMPTY;

            if (!input.isEmpty() || !fuel.isEmpty() || !output.isEmpty()) {
                if (!input.isEmpty()) tooltip.add(snownee.jade.api.ui.IElementHelper.get().item(input));
                if (!fuel.isEmpty()) tooltip.append(snownee.jade.api.ui.IElementHelper.get().item(fuel));
                tooltip.append(snownee.jade.api.ui.IElementHelper.get().text(Component.literal(" → ")));
                if (!output.isEmpty()) tooltip.append(snownee.jade.api.ui.IElementHelper.get().item(output));
            }
        }
    }
}
