package com.velorise.emc;

import com.velorise.emc.common.CustomUtilityProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TeleportCostUtil {
    private TeleportCostUtil() {}

    private static Vec3 mapped(Level level, Vec3 pos) {
        return level.dimension() == Level.NETHER ? new Vec3(pos.x * 8.0, pos.y, pos.z * 8.0) : pos;
    }

    public static long cost(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos) {
        return cost(player, targetLevel, targetPos, CustomUtilityProfile.teleportEmcPerBlock());
    }

    public static long cost(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos, double emcPerBlock) {
        boolean crossDimension = player.level().dimension() != targetLevel.dimension();
        double distance = mapped(player.level(), player.position()).distanceTo(mapped(targetLevel, targetPos));
        long distanceCost = (long)Math.ceil(distance * Math.max(0.0D, emcPerBlock));
        long surcharge = crossDimension ? CustomUtilityProfile.teleportCrossDimensionSurcharge() : 0L;
        return distanceCost > Long.MAX_VALUE - surcharge ? Long.MAX_VALUE : distanceCost + surcharge;
    }
}
