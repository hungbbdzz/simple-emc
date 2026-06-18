package com.velorise.simpleemc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;

import java.util.List;

public class MagicMirrorItem extends Item {

    public MagicMirrorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private Vec3 getOverworldEquivalentPos(Level level, Vec3 pos) {
        if (level.dimension() == Level.NETHER) {
            return new Vec3(pos.x * 8.0, pos.y, pos.z * 8.0);
        }
        return pos;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean hasSpawnPoint = serverPlayer.getRespawnPosition() != null;
            boolean respawnForced = serverPlayer.isRespawnForced();
            DimensionTransition transition = serverPlayer.findRespawnPositionAndUseSpawnBlock(respawnForced, DimensionTransition.DO_NOTHING);

            ServerLevel targetLevel = transition.newLevel();
            Vec3 targetPos = transition.pos();
            boolean crossDimension = serverPlayer.level().dimension() != targetLevel.dimension();

            long cost;
            if (crossDimension) {
                Vec3 currentMapped = getOverworldEquivalentPos(serverPlayer.level(), serverPlayer.position());
                Vec3 targetMapped = getOverworldEquivalentPos(targetLevel, targetPos);
                double distance = currentMapped.distanceTo(targetMapped);
                cost = 10000 + (long) Math.ceil(distance * 10);
            } else {
                double distance = serverPlayer.position().distanceTo(targetPos);
                cost = (long) Math.ceil(distance * 10);
            }

            PlayerEMC emcData = serverPlayer.getData(SimpleEMC.PLAYER_EMC.get());
            long currentEMC = emcData.getEmc();

            if (currentEMC < cost) {
                serverPlayer.displayClientMessage(Component.literal("§cNot enough EMC! Needs §e" + cost + " EMC §c(Have: §e" + currentEMC + " EMC§c)"), true);
                serverPlayer.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60; // 3 seconds (60 ticks)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE; // Keep it in normal holding position
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
            // Shake/swing the arm every 3 ticks to create a trembling/wobbling effect
            if (count % 3 == 0) {
                serverPlayer.swing(serverPlayer.getUsedItemHand(), true);
            }
            
            // Particle visual feedback while charging
            if (count % 5 == 0) {
                ServerLevel departureLevel = (ServerLevel) serverPlayer.level();
                departureLevel.sendParticles(ParticleTypes.PORTAL, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 10, 0.3, 0.5, 0.3, 0.15);
            }
            // Repeat subtle sound effects during holding
            if (count % 20 == 0) {
                serverPlayer.playNotifySound(SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 0.4F, 1.5F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
            boolean hasSpawnPoint = serverPlayer.getRespawnPosition() != null;
            boolean respawnForced = serverPlayer.isRespawnForced();
            DimensionTransition transition = serverPlayer.findRespawnPositionAndUseSpawnBlock(respawnForced, DimensionTransition.DO_NOTHING);

            ServerLevel targetLevel = transition.newLevel();
            Vec3 targetPos = transition.pos();
            float targetAngle = transition.yRot();
            boolean crossDimension = serverPlayer.level().dimension() != targetLevel.dimension();
            boolean bedObstructed = hasSpawnPoint && transition.missingRespawnBlock();

            long cost;
            if (crossDimension) {
                Vec3 currentMapped = getOverworldEquivalentPos(serverPlayer.level(), serverPlayer.position());
                Vec3 targetMapped = getOverworldEquivalentPos(targetLevel, targetPos);
                double distance = currentMapped.distanceTo(targetMapped);
                cost = 10000 + (long) Math.ceil(distance * 10);
            } else {
                double distance = serverPlayer.position().distanceTo(targetPos);
                cost = (long) Math.ceil(distance * 10);
            }

            PlayerEMC emcData = serverPlayer.getData(SimpleEMC.PLAYER_EMC.get());
            long currentEMC = emcData.getEmc();

            if (currentEMC < cost) {
                serverPlayer.displayClientMessage(Component.literal("§cNot enough EMC! Needs §e" + cost + " EMC §c(Have: §e" + currentEMC + " EMC§c)"), true);
                serverPlayer.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                return stack;
            }

            // Deduct EMC
            emcData.setEmc(currentEMC - cost);
            SimpleEMC.syncPlayerData(serverPlayer);

            // Departure particles & sounds
            ServerLevel departureLevel = (ServerLevel) serverPlayer.level();
            departureLevel.sendParticles(ParticleTypes.PORTAL, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
            departureLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            // Teleport the player
            serverPlayer.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, targetAngle, 0.0F);

            // Arrival particles & sounds
            targetLevel.sendParticles(ParticleTypes.PORTAL, targetPos.x, targetPos.y + 1.0, targetPos.z, 30, 0.5, 0.5, 0.5, 0.5);
            targetLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z, SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            // Display messages
            if (bedObstructed) {
                serverPlayer.displayClientMessage(Component.literal("§cYour bed was obstructed or missing! Teleported to Spawn."), false);
            }
            if (crossDimension) {
                serverPlayer.displayClientMessage(Component.literal("§aTeleported across dimensions! (-§e" + cost + " EMC§a)"), true);
            } else {
                serverPlayer.displayClientMessage(Component.literal("§aTeleported back to spawn! (-§e" + cost + " EMC§a for §e" + (int) (cost / 10) + "m§a)"), true);
            }

            // Play successful mirror sound
            serverPlayer.playNotifySound(SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            
            // Add 15s (300 ticks) cooldown
            serverPlayer.getCooldowns().addCooldown(this, 300);
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.simpleemc.magic_mirror.tooltip1"));
        tooltipComponents.add(Component.translatable("item.simpleemc.magic_mirror.tooltip2"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
