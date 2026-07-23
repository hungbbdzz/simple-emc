package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class MirrorWaypointUtil {
    private static final String ROOT = "VeloriseEMCWaypoint";
    private static final String USE_ANCHOR = "VeloriseEMCUseAnchor";

    public record Link(ResourceLocation dimension, BlockPos pos) {}

    private MirrorWaypointUtil() {}

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void link(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag root = tag(stack);
        CompoundTag link = new CompoundTag();
        link.putString("Dimension", level.dimension().location().toString());
        link.putLong("Pos", pos.asLong());
        root.put(ROOT, link);
        write(stack, root);
    }

    public static Link getLink(ItemStack stack) {
        CompoundTag root = tag(stack);
        if (!root.contains(ROOT)) return null;
        CompoundTag link = root.getCompound(ROOT);
        ResourceLocation dimension = ResourceLocation.tryParse(link.getString("Dimension"));
        return dimension == null ? null : new Link(dimension, BlockPos.of(link.getLong("Pos")));
    }

    public static void setUsingAnchor(ItemStack stack, boolean value) {
        CompoundTag root = tag(stack);
        root.putBoolean(USE_ANCHOR, value);
        write(stack, root);
    }

    public static boolean isUsingAnchor(ItemStack stack) {
        return tag(stack).getBoolean(USE_ANCHOR);
    }
}
