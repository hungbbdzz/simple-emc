package com.velorise.emc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

public final class MirrorWaypointUtil {
    private static final String ROOT = "VeloriseEMCWaypoint";
    private static final String USE_ANCHOR = "VeloriseEMCUseAnchor";

    public record Link(ResourceLocation dimension, BlockPos pos) {}

    private MirrorWaypointUtil() {}

    public static void link(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag link = new CompoundTag();
        link.putString("Dimension", level.dimension().location().toString());
        link.putLong("Pos", pos.asLong());
        tag.put(ROOT, link);
    }

    public static Link getLink(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(ROOT)) return null;
        CompoundTag tag = stack.getTag().getCompound(ROOT);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        return dimension == null ? null : new Link(dimension, BlockPos.of(tag.getLong("Pos")));
    }

    public static void setUsingAnchor(ItemStack stack, boolean value) {
        stack.getOrCreateTag().putBoolean(USE_ANCHOR, value);
    }

    public static boolean isUsingAnchor(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(USE_ANCHOR);
    }
}
