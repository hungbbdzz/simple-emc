package com.velorise.emc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.DimensionTransition;

/** NeoForge-specific personal-respawn resolution for the Last Echo fountain. */
public final class FountainPortalCompat {
    private FountainPortalCompat() {}

    public static void teleportHome(ServerPlayer player) {
        DimensionTransition target = player.findRespawnPositionAndUseSpawnBlock(
            false, DimensionTransition.DO_NOTHING);
        player.teleportTo(target.newLevel(), target.pos().x, target.pos().y, target.pos().z,
            target.yRot(), 0.0F);
        target.newLevel().sendParticles(ParticleTypes.PORTAL,
            target.pos().x, target.pos().y + 1.0D, target.pos().z,
            40, 0.5D, 0.7D, 0.5D, 0.20D);
        target.newLevel().playSound(null, target.pos().x, target.pos().y, target.pos().z,
            SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.85F);
    }
}
