package com.velorise.simpleemc;

import com.velorise.simpleemc.capability.PlayerEMCCapability;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

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

    private static class RespawnInfo {
        final ServerLevel level;
        final Vec3 pos;
        final float angle;
        final boolean bedObstructed;

        RespawnInfo(ServerLevel level, Vec3 pos, float angle, boolean bedObstructed) {
            this.level = level;
            this.pos = pos;
            this.angle = angle;
            this.bedObstructed = bedObstructed;
        }
    }

    private RespawnInfo findRespawnInfo(ServerPlayer serverPlayer) {
        BlockPos respawnPosition = serverPlayer.getRespawnPosition();
        ServerLevel targetLevel = serverPlayer.server.getLevel(serverPlayer.getRespawnDimension());
        if (targetLevel == null) {
            targetLevel = serverPlayer.server.overworld();
        }
        boolean respawnForced = serverPlayer.isRespawnForced();
        Vec3 targetPos;
        float targetAngle = 0.0f;
        boolean bedObstructed = false;

        if (respawnPosition != null) {
            Optional<Vec3> respawnPosOpt = Player.findRespawnPositionAndUseSpawnBlock(targetLevel, respawnPosition, serverPlayer.getRespawnAngle(), respawnForced, true);
            if (respawnPosOpt.isPresent()) {
                targetPos = respawnPosOpt.get();
                targetAngle = serverPlayer.getRespawnAngle();
            } else {
                bedObstructed = true;
                BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                targetPos = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            }
        } else {
            targetLevel = serverPlayer.server.overworld();
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            targetPos = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        }

        return new RespawnInfo(targetLevel, targetPos, targetAngle, bedObstructed);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            RespawnInfo respawnInfo = findRespawnInfo(serverPlayer);
            boolean crossDimension = serverPlayer.level().dimension() != respawnInfo.level.dimension();

            long cost;
            if (crossDimension) {
                Vec3 currentMapped = getOverworldEquivalentPos(serverPlayer.level(), serverPlayer.position());
                Vec3 targetMapped = getOverworldEquivalentPos(respawnInfo.level, respawnInfo.pos);
                double distance = currentMapped.distanceTo(targetMapped);
                cost = 10000 + (long) Math.ceil(distance * 10);
            } else {
                double distance = serverPlayer.position().distanceTo(respawnInfo.pos);
                cost = (long) Math.ceil(distance * 10);
            }

            Optional<PlayerEMC> emcCap = serverPlayer.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).resolve();
            if (emcCap.isPresent()) {
                long currentEMC = emcCap.get().getEmc();
                if (currentEMC < cost) {
                    serverPlayer.displayClientMessage(Component.literal("§cNot enough EMC! Needs §e" + cost + " EMC §c(Have: §e" + currentEMC + " EMC§c)"), true);
                    serverPlayer.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                    return InteractionResultHolder.fail(stack);
                }
            } else {
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 60; // 3 seconds (60 ticks)
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
            if (count % 3 == 0) {
                serverPlayer.swing(serverPlayer.getUsedItemHand(), true);
            }
            
            if (count % 5 == 0) {
                ServerLevel departureLevel = (ServerLevel) serverPlayer.level();
                departureLevel.sendParticles(ParticleTypes.PORTAL, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 10, 0.3, 0.5, 0.3, 0.15);
            }
            if (count % 20 == 0) {
                serverPlayer.playNotifySound(SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 0.4F, 1.5F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer serverPlayer) {
            RespawnInfo respawnInfo = findRespawnInfo(serverPlayer);
            boolean crossDimension = serverPlayer.level().dimension() != respawnInfo.level.dimension();

            long cost;
            if (crossDimension) {
                Vec3 currentMapped = getOverworldEquivalentPos(serverPlayer.level(), serverPlayer.position());
                Vec3 targetMapped = getOverworldEquivalentPos(respawnInfo.level, respawnInfo.pos);
                double distance = currentMapped.distanceTo(targetMapped);
                cost = 10000 + (long) Math.ceil(distance * 10);
            } else {
                double distance = serverPlayer.position().distanceTo(respawnInfo.pos);
                cost = (long) Math.ceil(distance * 10);
            }

            serverPlayer.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(emcData -> {
                long currentEMC = emcData.getEmc();

                if (currentEMC < cost) {
                    serverPlayer.displayClientMessage(Component.literal("§cNot enough EMC! Needs §e" + cost + " EMC §c(Have: §e" + currentEMC + " EMC§c)"), true);
                    serverPlayer.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
                    return;
                }

                // Deduct EMC
                emcData.setEmc(currentEMC - cost);
                SimpleEMC.syncPlayerData(serverPlayer);

                // Departure particles & sounds
                ServerLevel departureLevel = (ServerLevel) serverPlayer.level();
                departureLevel.sendParticles(ParticleTypes.PORTAL, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
                departureLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                // Teleport the player
                serverPlayer.teleportTo(respawnInfo.level, respawnInfo.pos.x, respawnInfo.pos.y, respawnInfo.pos.z, respawnInfo.angle, 0.0F);

                // Arrival particles & sounds
                respawnInfo.level.sendParticles(ParticleTypes.PORTAL, respawnInfo.pos.x, respawnInfo.pos.y + 1.0, respawnInfo.pos.z, 30, 0.5, 0.5, 0.5, 0.5);
                respawnInfo.level.playSound(null, respawnInfo.pos.x, respawnInfo.pos.y, respawnInfo.pos.z, SimpleEMC.MAGIC_MIRROR_USE_SOUND.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

                // Display messages
                if (respawnInfo.bedObstructed) {
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
            });
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.simpleemc.magic_mirror.tooltip1"));
        tooltipComponents.add(Component.translatable("item.simpleemc.magic_mirror.tooltip2"));
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
    }
}
