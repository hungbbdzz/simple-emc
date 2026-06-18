package com.velorise.simpleemc;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChaliceOfTransmutationItem extends Item {
    private static final long MAX_EMC = Long.MAX_VALUE;

    public ChaliceOfTransmutationItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getMode(stack) == 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (getMode(stack) == 0) {
            BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();
                FluidState centerFluid = level.getFluidState(pos);
                if (!centerFluid.isEmpty()) {
                    if (!level.isClientSide()) {
                        int radius = 4;
                        long waterCount = 0;
                        long lavaCount = 0;
                        long otherCount = 0;
                        
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dy = -radius; dy <= radius; dy++) {
                                for (int dz = -radius; dz <= radius; dz++) {
                                    BlockPos targetPos = pos.offset(dx, dy, dz);
                                    FluidState fluidState = level.getFluidState(targetPos);
                                    if (!fluidState.isEmpty()) {
                                        if (fluidState.is(Fluids.LAVA) || fluidState.is(Fluids.FLOWING_LAVA)) {
                                            lavaCount++;
                                        } else if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) {
                                            waterCount++;
                                        } else {
                                            otherCount++;
                                        }
                                    }
                                }
                            }
                        }
                        
                        long totalCount = waterCount + lavaCount + otherCount;
                        if (totalCount == 0) {
                            return InteractionResultHolder.pass(stack);
                        }
                        
                        long totalCost = 200 + (waterCount * 10) + (lavaCount * 50) + (otherCount * 20);
                        
                        long currentEMC = getStoredEMC(stack);
                        while (currentEMC < totalCost) {
                            long fuelVal = EMCRegistry.consumeFuelFromInventory(player);
                            if (fuelVal <= 0) {
                                break;
                            }
                            currentEMC += fuelVal;
                        }
                        
                        if (currentEMC < totalCost) {
                            player.displayClientMessage(Component.literal("§cNot enough alchemical energy! Need §e" + totalCost + " EMC §8(Have " + currentEMC + " EMC)"), true);
                            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
                            return InteractionResultHolder.fail(stack);
                        }
                        
                        setStoredEMC(stack, currentEMC - totalCost);
                        
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dy = -radius; dy <= radius; dy++) {
                                for (int dz = -radius; dz <= radius; dz++) {
                                    BlockPos targetPos = pos.offset(dx, dy, dz);
                                    FluidState fluidState = level.getFluidState(targetPos);
                                    if (!fluidState.isEmpty()) {
                                        level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                                    }
                                }
                            }
                        }
                        
                        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                            SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.2F, 1.0F);
                        if (lavaCount > 0) {
                            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
                            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE, 
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 1.0, 1.0, 1.0, 0.0);
                        }
                        ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, 
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 1.5, 1.5, 1.5, 0.1);
                        
                        player.displayClientMessage(Component.literal("§bVoided " + totalCount + " fluid blocks! (-§e" + totalCost + " EMC§f)"), true);
                        return InteractionResultHolder.success(stack);
                    } else {
                        return InteractionResultHolder.success(stack);
                    }
                }
            }
        }

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
        
        if (getMode(stack) == 1 && isHeld) {
            long currentEMC = getStoredEMC(stack);
            long cost = 4;
            
            if (currentEMC < cost) {
                long added = EMCRegistry.consumeFuelFromInventory(player);
                if (added > 0) {
                    currentEMC += added;
                    setStoredEMC(stack, currentEMC);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.5F);
                } else {
                    setMode(stack, 0);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                    player.displayClientMessage(Component.literal("§cNo alchemical fuel found! Growth mode deactivated."), true);
                    return;
                }
            }

            if (currentEMC >= cost) {
                setStoredEMC(stack, currentEMC - cost);
            }

            if (player.tickCount % 20 == 0) {
                int radius = 2;
                BlockPos playerPos = player.blockPosition();
                boolean grownAny = false;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            BlockPos targetPos = playerPos.offset(dx, dy, dz);
                            BlockState targetState = level.getBlockState(targetPos);
                            
                            if (targetState.getBlock() instanceof BonemealableBlock bonemealable) {
                                if (bonemealable.isValidBonemealTarget(level, targetPos, targetState, level.isClientSide())) {
                                    if (bonemealable.isBonemealSuccess(level, level.random, targetPos, targetState)) {
                                        bonemealable.performBonemeal((ServerLevel) level, level.random, targetPos, targetState);
                                        
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int mode = getMode(stack);
        long emc = getStoredEMC(stack);
        
        tooltipComponents.add(Component.literal("§7Mode: " + (mode == 0 ? "§bVoid Mode (Sponge clear)" : "§aGrowth Mode (Tick crops)")));
        tooltipComponents.add(Component.literal("§7Charge: §e" + String.format("%,d", emc) + " EMC"));
        tooltipComponents.add(Component.translatable("item.simpleemc.chalice_of_transmutation.tooltip1"));
        tooltipComponents.add(Component.translatable("item.simpleemc.chalice_of_transmutation.tooltip2"));
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
    }

    public static int getMode(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("mode") : 0;
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt("mode", mode);
    }

    public static long getStoredEMC(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getLong("emc") : 0L;
    }

    public static void setStoredEMC(ItemStack stack, long emc) {
        stack.getOrCreateTag().putLong("emc", emc);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
