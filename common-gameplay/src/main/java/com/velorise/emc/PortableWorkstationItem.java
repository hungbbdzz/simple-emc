package com.velorise.emc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Portable workstation item. Every variant is a fixed utility made with Core I.
 * Portable workstations never carry a level and never scan nearby inventories.
 */
public final class PortableWorkstationItem extends BlockItem {
    public enum Kind {
        CRAFTING,
        STONECUTTER,
        LOOM,
        CARTOGRAPHY
    }

    private final Kind kind;

    public PortableWorkstationItem(Block block, Properties properties, Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PortableWorkstationUi.open(serverPlayer, kind);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static boolean isPortableWorkstation(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PortableWorkstationItem;
    }
}
