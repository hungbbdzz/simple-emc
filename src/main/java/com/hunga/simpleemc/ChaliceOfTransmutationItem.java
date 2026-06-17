package com.hunga.simpleemc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class ChaliceOfTransmutationItem extends Item {
    private static final long MAX_EMC = Long.MAX_VALUE;

    public ChaliceOfTransmutationItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Shine when in Growth Mode to indicate it is active
        return getMode(stack) == 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Void Mode Liquid Collection
        if (getMode(stack) == 0) {
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();
                FluidState fluidState = level.getFluidState(pos);
                
                if (fluidState.isSource()) {
                    if (!level.isClientSide()) {
                        long currentEMC = getStoredEMC(stack);
                        if (currentEMC >= MAX_EMC) {
                            player.displayClientMessage(Component.literal("Chalice is full of EMC! Cannot void liquids."), true);
                            return InteractionResultHolder.fail(stack);
                        }

                        long gained = 0;
                        if (fluidState.is(Fluids.WATER)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                                SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                            gained = 2;
                        } else if (fluidState.is(Fluids.LAVA)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
                            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE, 
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.0);
                            gained = 64;
                        }

                        if (gained > 0) {
                            long newEMC = Math.min(MAX_EMC, currentEMC + gained);
                            setStoredEMC(stack, newEMC);
                            player.displayClientMessage(Component.literal("Voided liquid! (+§e" + gained + " EMC§f, Total: §e" + newEMC + " EMC§f)"), true);
                            return InteractionResultHolder.success(stack);
                        }
                    } else {
                        return InteractionResultHolder.success(stack);
                    }
                }
            }
        }

        // Toggle mode on click if not voiding liquid
        if (!level.isClientSide()) {
            int mode = getMode(stack) == 0 ? 1 : 0;
            setMode(stack, mode);
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, mode == 1 ? 1.2F : 0.8F);
            player.displayClientMessage(Component.literal("Chalice Mode: " + (mode == 0 ? "§bVoid Mode" : "§aGrowth Mode")), true);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        boolean isHeld = (player.getMainHandItem() == stack || player.getOffhandItem() == stack);
        
        // Growth Mode logic: Ticks crops within 5x5x3 area centered on player, runs once a second
        if (getMode(stack) == 1 && isHeld) {
            long currentEMC = getStoredEMC(stack);
            long cost = 4; // 4 EMC per game tick of active operation in Growth Mode
            
            if (currentEMC < cost) {
                // Try to auto-recharge
                long added = EMCRegistry.consumeFuelFromInventory(player);
                if (added > 0) {
                    currentEMC += added;
                    setStoredEMC(stack, currentEMC);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.5F);
                } else {
                    // Turn off growth mode
                    setMode(stack, 0); // Revert to Void Mode (inactive)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                    player.displayClientMessage(Component.literal("§cNo alchemical fuel found! Growth mode deactivated."), true);
                    return;
                }
            }

            if (currentEMC >= cost) {
                setStoredEMC(stack, currentEMC - cost);
            }

            // Crop growth ticking once a second (20 ticks)
            if (player.tickCount % 20 == 0) {
                int radius = 2; // 5x5 area
                BlockPos playerPos = player.blockPosition();
                boolean grownAny = false;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            BlockPos targetPos = playerPos.offset(dx, dy, dz);
                            BlockState targetState = level.getBlockState(targetPos);
                            
                            if (targetState.getBlock() instanceof BonemealableBlock bonemealable) {
                                if (bonemealable.isValidBonemealTarget(level, targetPos, targetState)) {
                                    if (bonemealable.isBonemealSuccess(level, level.random, targetPos, targetState)) {
                                        bonemealable.performBonemeal((ServerLevel) level, level.random, targetPos, targetState);
                                        
                                        // Spawn happy villager green particles
                                        ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                                            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 
                                            4, 0.25, 0.25, 0.25, 0.05);
                                        grownAny = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (grownAny) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.5F, 1.0F);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int mode = getMode(stack);
        long emc = getStoredEMC(stack);
        
        tooltipComponents.add(Component.literal("§7Mode: " + (mode == 0 ? "§bVoid Mode (Clear liquids)" : "§aGrowth Mode (Tick crops)")));
        tooltipComponents.add(Component.literal("§7Charge: §e" + String.format("%,d", emc) + " EMC"));
        tooltipComponents.add(Component.translatable("item.simpleemc.chalice_of_transmutation.tooltip1"));
        tooltipComponents.add(Component.translatable("item.simpleemc.chalice_of_transmutation.tooltip2"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // Helper NBT get/set methods
    public static int getMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getInt("mode");
        }
        return 0; // Default: Void Mode
    }

    public static void setMode(ItemStack stack, int mode) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt("mode", mode);
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
