package com.hunga.simpleemc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AttractionCatalystItem extends Item {
    private static final long MAX_EMC = Long.MAX_VALUE;

    public AttractionCatalystItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide()) {
            // Toggle active state directly on right click
            boolean active = !isActive(stack);
            setActive(stack, active);
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, active ? 1.2F : 0.8F);
            player.displayClientMessage(Component.literal("Attraction Catalyst: " + (active ? "§aACTIVE" : "§cINACTIVE")), true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        if (isActive(stack)) {
            long currentEMC = getStoredEMC(stack);
            long cost = 2; // 2 EMC per game tick of active operation
            
            if (currentEMC < cost) {
                // Try to auto-recharge
                long added = EMCRegistry.consumeFuelFromInventory(player);
                if (added > 0) {
                    currentEMC += added;
                    setStoredEMC(stack, currentEMC);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.5F);
                } else {
                    // Deactivate due to no fuel
                    setActive(stack, false);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                    player.displayClientMessage(Component.literal("§cNo alchemical fuel found! Attraction Catalyst deactivated."), true);
                    return;
                }
            }

            if (currentEMC >= cost) {
                setStoredEMC(stack, currentEMC - cost);

                // Pull nearby item entities
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0));
                for (ItemEntity itemEntity : items) {
                    if (!itemEntity.isAlive() || itemEntity.hasPickUpDelay()) {
                        continue;
                    }

                    // Pull the item towards the player
                    Vec3 dir = player.position().add(0, 0.5, 0).subtract(itemEntity.position());
                    double distSq = dir.lengthSqr();
                    
                    if (distSq > 0.1) {
                        Vec3 motion = dir.normalize().scale(0.25);
                        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(motion));
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        boolean active = isActive(stack);
        long emc = getStoredEMC(stack);
        
        tooltipComponents.add(Component.literal("§7Status: " + (active ? "§aActive" : "§cInactive")));
        tooltipComponents.add(Component.literal("§7Charge: §e" + String.format("%,d", emc) + " EMC"));
        tooltipComponents.add(Component.translatable("item.simpleemc.attraction_catalyst.tooltip1"));
        tooltipComponents.add(Component.translatable("item.simpleemc.attraction_catalyst.tooltip2"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // Helper NBT get/set methods
    public static boolean isActive(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getBoolean("active");
        }
        return false;
    }

    public static void setActive(ItemStack stack, boolean active) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putBoolean("active", active);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static long getStoredEMC(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getLong("emc");
        }
        return 0L;
    }

    public static void setStoredEMC(ItemStack stack, long emc) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putLong("emc", emc);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
