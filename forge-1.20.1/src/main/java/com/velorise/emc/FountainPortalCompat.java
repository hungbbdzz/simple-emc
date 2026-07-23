package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Forge-specific personal-respawn resolution for the Last Echo fountain. */
public final class FountainPortalCompat {
    private FountainPortalCompat() {}

    public static void teleportHome(ServerPlayer player) {
        BlockPos respawn = player.getRespawnPosition();
        ServerLevel destination = player.server.getLevel(player.getRespawnDimension());
        if (destination == null) destination = player.server.overworld();
        Vec3 target;
        float angle = 0.0F;
        if (respawn != null) {
            Optional<Vec3> resolved = Player.findRespawnPositionAndUseSpawnBlock(
                destination, respawn, player.getRespawnAngle(), player.isRespawnForced(), true);
            if (resolved.isPresent()) {
                target = resolved.get();
                angle = player.getRespawnAngle();
            } else {
                destination = player.server.overworld();
                target = Vec3.atBottomCenterOf(destination.getSharedSpawnPos());
            }
        } else {
            destination = player.server.overworld();
            target = Vec3.atBottomCenterOf(destination.getSharedSpawnPos());
        }
        player.teleportTo(destination, target.x, target.y, target.z, angle, 0.0F);
        destination.sendParticles(ParticleTypes.PORTAL, target.x, target.y + 1.0D, target.z,
            40, 0.5D, 0.7D, 0.5D, 0.20D);
        destination.playSound(null, target.x, target.y, target.z,
            SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.85F);
    }
}
