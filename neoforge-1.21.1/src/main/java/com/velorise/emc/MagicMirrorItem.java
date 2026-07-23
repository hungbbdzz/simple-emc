package com.velorise.emc;

import com.velorise.emc.common.CustomUtilityProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Core-scaled return utility. Core increases same-dimension range and reduces EMC per block. */
public class MagicMirrorItem extends Item implements CoreIIntegratedItem {
    private record Target(ServerLevel level, Vec3 pos, float angle, boolean bedObstructed, boolean anchor) {}

    public MagicMirrorItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(VeloriseEMC.WAYPOINT_ANCHOR.get())) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
            MirrorWaypointUtil.link(context.getItemInHand(), context.getLevel(), context.getClickedPos());
            context.getPlayer().displayClientMessage(Component.literal("§aMagic Mirror linked to this Waypoint Anchor"), true);
            context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    private Target resolveTarget(ServerPlayer player, ItemStack stack) {
        if (MirrorWaypointUtil.isUsingAnchor(stack)) {
            MirrorWaypointUtil.Link link = MirrorWaypointUtil.getLink(stack);
            if (link == null) return null;
            ServerLevel targetLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, link.dimension()));
            if (targetLevel == null || !targetLevel.getBlockState(link.pos()).is(VeloriseEMC.WAYPOINT_ANCHOR.get())) return null;
            BlockPos feet = link.pos().above();
            BlockPos head = feet.above();
            if (!targetLevel.getBlockState(feet).getCollisionShape(targetLevel, feet).isEmpty()
                || !targetLevel.getBlockState(head).getCollisionShape(targetLevel, head).isEmpty()) return null;
            return new Target(targetLevel, Vec3.atBottomCenterOf(feet), player.getYRot(), false, true);
        }

        boolean hadRespawn = player.getRespawnPosition() != null;
        DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        return new Target(
            transition.newLevel(), transition.pos(), transition.yRot(),
            hadRespawn && transition.missingRespawnBlock(), false
        );
    }

    private static double emcPerBlock(ItemStack stack) {
        return CustomUtilityProfile.magicMirrorEmcPerBlock(CoreTierData.installedLevel(stack));
    }

    private static long teleportCost(ServerPlayer player, ItemStack stack, Target target) {
        return TeleportCostUtil.cost(player, target.level, target.pos, emcPerBlock(stack));
    }

    private static boolean isWithinRange(ServerPlayer player, ItemStack stack, Target target) {
        if (player.serverLevel() != target.level) return true;
        double range = CustomUtilityProfile.magicMirrorRange(CoreTierData.installedLevel(stack));
        return player.position().distanceToSqr(target.pos) <= range * range;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        boolean anchorMode = player.isShiftKeyDown() && MirrorWaypointUtil.getLink(stack) != null;
        MirrorWaypointUtil.setUsingAnchor(stack, anchorMode);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Target target = resolveTarget(serverPlayer, stack);
            if (target == null) {
                serverPlayer.displayClientMessage(Component.literal("§cMirror target is missing or obstructed"), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!isWithinRange(serverPlayer, stack, target)) {
                long range = Math.round(CustomUtilityProfile.magicMirrorRange(CoreTierData.installedLevel(stack)));
                serverPlayer.displayClientMessage(Component.literal("§cMirror target exceeds the §e" + range + "§c block range"), true);
                return InteractionResultHolder.fail(stack);
            }
            long cost = teleportCost(serverPlayer, stack, target);
            if (!EmcTransactionService.canSpend(serverPlayer, cost)) {
                EmcFailureMessages.show(serverPlayer, cost);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return CustomUtilityProfile.magicMirrorUseDurationTicks(); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (count % 3 == 0) player.swing(player.getUsedItemHand(), true);
            if (count % 5 == 0) ((ServerLevel)player.level()).sendParticles(
                ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.5, 0.3, 0.15
            );
            if (count % 20 == 0) player.playNotifySound(VeloriseEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 0.4F, 1.5F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            Target target = resolveTarget(player, stack);
            if (target == null) {
                player.displayClientMessage(Component.literal("§cMirror target is missing or obstructed"), true);
                return stack;
            }
            if (!isWithinRange(player, stack, target)) {
                long range = Math.round(CustomUtilityProfile.magicMirrorRange(CoreTierData.installedLevel(stack)));
                player.displayClientMessage(Component.literal("§cMirror target exceeds the §e" + range + "§c block range"), true);
                return stack;
            }
            long cost = teleportCost(player, stack, target);
            if (!EmcTransactionService.spend(player, cost)) return stack;

            ServerLevel departure = player.serverLevel();
            departure.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
            departure.playSound(null, player.blockPosition(), VeloriseEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.teleportTo(target.level, target.pos.x, target.pos.y, target.pos.z, target.angle, 0.0F);
            target.level.sendParticles(ParticleTypes.PORTAL, target.pos.x, target.pos.y + 1.0, target.pos.z, 30, 0.5, 0.5, 0.5, 0.5);
            target.level.playSound(null, target.pos.x, target.pos.y, target.pos.z, VeloriseEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (target.bedObstructed) player.displayClientMessage(Component.literal("§cBed missing or obstructed; moved to world spawn"), false);
            player.displayClientMessage(Component.literal(target.anchor
                ? "§aTeleported to Waypoint Anchor (-§e" + cost + " EMC§a)"
                : "§aTeleported home (-§e" + cost + " EMC§a)"), true);
            player.getCooldowns().addCooldown(this, CustomUtilityProfile.magicMirrorCooldownTicks());
        }
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return CoreTierData.installedLevel(stack) > 0; }

        
}
