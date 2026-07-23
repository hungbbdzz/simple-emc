package com.velorise.emc;

import com.velorise.emc.common.CustomUtilityProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Short-range blink. Core increases range only; the EMC price stays fixed. */
public class VoidTranslocatorItem extends Item implements CoreIIntegratedItem {
    public VoidTranslocatorItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack stack) { return CoreTierData.installedLevel(stack) > 0; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        int core = CoreTierData.installedLevel(stack);
        long cost = CustomUtilityProfile.translocatorCost();
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            double range = CustomUtilityProfile.translocatorRange(core)
                * ResonantSetBonus.voidTranslocatorMultiplier(player);
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            Vec3 end = start.add(look.scale(range));
            BlockHitResult hitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 destination = resolveDestination(level, look, end, hitResult);
            if (destination == null) return InteractionResultHolder.fail(stack);

            if (!BufferedItemEmcService.spend(serverPlayer, cost,
                () -> getStoredEMC(stack), value -> setStoredEMC(stack, value))) {
                EmcFailureMessages.show(serverPlayer, cost);
                return InteractionResultHolder.fail(stack);
            }

            ServerLevel serverLevel = serverPlayer.serverLevel();
            spawnPortalParticles(serverLevel, player.position().add(0, 1.0, 0));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
            player.teleportTo(destination.x, destination.y, destination.z);
            player.fallDistance = 0.0F;
            player.getCooldowns().addCooldown(this, CustomUtilityProfile.translocatorCooldownTicks());
            spawnPortalParticles(serverLevel, destination.add(0, 1.0, 0));
            level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.literal("§bTranslocated " + Math.round(range)
                + "-block maximum §7(-§e" + cost + " EMC§7)"), true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static Vec3 resolveDestination(Level level, Vec3 look, Vec3 end, BlockHitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();
            BlockPos above = hitPos.above();
            if (isSafePosition(level, above)) return Vec3.atBottomCenterOf(above);
            Direction face = hitResult.getDirection();
            BlockPos adjacent = hitPos.relative(face);
            if (isSafePosition(level, adjacent)) return Vec3.atBottomCenterOf(adjacent);
            Vec3 fallback = hitResult.getLocation().subtract(look.scale(1.2D));
            return isSafePosition(level, BlockPos.containing(fallback)) ? fallback : null;
        }
        return isSafePosition(level, BlockPos.containing(end)) ? end : null;
    }

    private static boolean isSafePosition(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)
            && !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above());
    }

    private static void spawnPortalParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 24, 0.4, 0.6, 0.4, 0.2);
    }

    

    public static long getStoredEMC(ItemStack stack) {
        return stack.hasTag() ? Math.max(0L, stack.getTag().getLong("emc")) : 0L;
    }

    public static void setStoredEMC(ItemStack stack, long emc) {
        stack.getOrCreateTag().putLong("emc", Math.max(0L, emc));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
