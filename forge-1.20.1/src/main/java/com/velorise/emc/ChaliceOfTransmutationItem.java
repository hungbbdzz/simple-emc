package com.velorise.emc;

import com.velorise.emc.common.CustomUtilityProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
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

import java.util.ArrayList;
import java.util.List;

/** Context utility: use on fluids to void them, or on growable blocks to stimulate the area. */
public class ChaliceOfTransmutationItem extends Item implements CoreIIntegratedItem {
    private static final String TAG_EMC = "emc";

    public ChaliceOfTransmutationItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack stack) { return CoreTierData.installedLevel(stack) > 0; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return activate(context.getLevel(), player, context.getItemInHand(), context.getClickedPos());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        InteractionResult result = activate(level, player, stack, hit.getBlockPos());
        return result.consumesAction()
            ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
            : InteractionResultHolder.pass(stack);
    }

    private InteractionResult activate(Level level, Player player, ItemStack stack, BlockPos origin) {
        if (!level.getFluidState(origin).isEmpty()) return voidFluids(level, player, stack, origin);
        return growPlants(level, player, stack, origin);
    }

    private InteractionResult voidFluids(Level level, Player player, ItemStack stack, BlockPos origin) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int radius = CustomUtilityProfile.chaliceRadius(CoreTierData.installedLevel(stack));
        int vertical = CustomUtilityProfile.chaliceVerticalRadius();
        List<BlockPos> targets = new ArrayList<>();
        long water = 0L, lava = 0L, other = 0L;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -vertical; dy <= vertical; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos target = origin.offset(dx, dy, dz);
                    FluidState fluid = level.getFluidState(target);
                    if (fluid.isEmpty()) continue;
                    targets.add(target);
                    if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) lava++;
                    else if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) water++;
                    else other++;
                }
            }
        }
        if (targets.isEmpty()) return InteractionResult.PASS;
        long cost = CustomUtilityProfile.chaliceVoidCost(water, lava, other);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (!BufferedItemEmcService.spend(serverPlayer, cost,
            () -> getStoredEMC(stack), value -> setStoredEMC(stack, value))) {
            EmcFailureMessages.show(serverPlayer, cost);
            return InteractionResult.FAIL;
        }
        for (BlockPos target : targets) level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, origin, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.2F, 1.0F);
        ServerLevel serverLevel = (ServerLevel) level;
        if (lava > 0L) {
            level.playSound(null, origin, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, origin.getX() + 0.5D, origin.getY() + 0.5D,
                origin.getZ() + 0.5D, 20, 1.0D, 1.0D, 1.0D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.SPLASH, origin.getX() + 0.5D, origin.getY() + 0.5D,
            origin.getZ() + 0.5D, 30, 1.5D, 1.5D, 1.5D, 0.1D);
        player.displayClientMessage(Component.literal("§bVoided " + targets.size() + " fluid blocks §7(-" + cost + " EMC)"), true);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult growPlants(Level level, Player player, ItemStack stack, BlockPos origin) {
        int radius = CustomUtilityProfile.chaliceRadius(CoreTierData.installedLevel(stack));
        int vertical = CustomUtilityProfile.chaliceVerticalRadius();
        if (level.isClientSide) {
            BlockState state = level.getBlockState(origin);
            return state.getBlock() instanceof BonemealableBlock ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -vertical; dy <= vertical; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos target = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(target);
                    if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) continue;
                    if (!bonemealable.isValidBonemealTarget(level, target, state, level.isClientSide)) continue;
                    if (!bonemealable.isBonemealSuccess(level, level.random, target, state)) continue;
                    targets.add(target);
                }
            }
        }
        if (targets.isEmpty()) return InteractionResult.PASS;
        long cost = CustomUtilityProfile.chaliceGrowthCost(targets.size());
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (!BufferedItemEmcService.spend(serverPlayer, cost,
            () -> getStoredEMC(stack), value -> setStoredEMC(stack, value))) {
            EmcFailureMessages.show(serverPlayer, cost);
            return InteractionResult.FAIL;
        }

        for (BlockPos target : targets) {
            BlockState state = level.getBlockState(target);
            if (state.getBlock() instanceof BonemealableBlock bonemealable) {
                bonemealable.performBonemeal((ServerLevel) level, level.random, target, state);
                ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                    4, 0.25D, 0.25D, 0.25D, 0.05D);
            }
        }
        level.playSound(null, origin, SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.7F, 1.0F);
        player.displayClientMessage(Component.literal("§aStimulated " + targets.size() + " plants §7(-" + cost + " EMC)"), true);
        return InteractionResult.SUCCESS;
    }

        

    public static long getStoredEMC(ItemStack stack) {
        return stack.hasTag() ? Math.max(0L, stack.getTag().getLong(TAG_EMC)) : 0L;
    }
    public static void setStoredEMC(ItemStack stack, long emc) {
        stack.getOrCreateTag().putLong(TAG_EMC, Math.max(0L, emc));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
