package com.velorise.emc;

import com.velorise.emc.common.CustomUtilityProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Toggleable item vacuum. Core expands area; operating price is fixed. */
public class AttractionCatalystItem extends Item implements CoreIIntegratedItem {
    public AttractionCatalystItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack stack) { return isActive(stack); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
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
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !isActive(stack)) return;
        int core = CoreTierData.installedLevel(stack);
        int radius = CustomUtilityProfile.attractionRadius(core);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
            player.getBoundingBox().inflate(radius, radius, radius),
            item -> item.isAlive() && !item.hasPickUpDelay());
        if (items.isEmpty()) return;

        long cost = CustomUtilityProfile.attractionCostPerTick();
        if (!BufferedItemEmcService.spend(player, cost,
            () -> getStoredEMC(stack), value -> setStoredEMC(stack, value))) {
            setActive(stack, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.displayClientMessage(Component.literal("§cAttraction Catalyst disabled: insufficient EMC"), true);
            return;
        }

        for (ItemEntity itemEntity : items) {
            Vec3 direction = player.position().add(0.0D, 0.5D, 0.0D).subtract(itemEntity.position());
            if (direction.lengthSqr() <= 0.1D) continue;
            itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(
                direction.normalize().scale(CustomUtilityProfile.attractionPullStrength())));
        }
    }

        

    public static boolean isActive(ItemStack stack) { return stack.hasTag() && stack.getTag().getBoolean("active"); }
    public static void setActive(ItemStack stack, boolean active) { stack.getOrCreateTag().putBoolean("active", active); }
    public static long getStoredEMC(ItemStack stack) { return stack.hasTag() ? Math.max(0L, stack.getTag().getLong("emc")) : 0L; }
    public static void setStoredEMC(ItemStack stack, long emc) { stack.getOrCreateTag().putLong("emc", Math.max(0L, emc)); }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
