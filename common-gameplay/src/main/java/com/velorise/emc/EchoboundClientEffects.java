package com.velorise.emc;

import net.minecraft.world.phys.Vec3;

/** Small loader-neutral state bridge for the boss impact camera shake. */
public final class EchoboundClientEffects {
    private static long shakeEndNanos;
    private static long shakeDurationNanos;
    private static Vec3 shakeOrigin = Vec3.ZERO;
    private static float shakeStrength;

    private EchoboundClientEffects() {}

    public static void startShake(Vec3 origin, int ticks, float strength) {
        shakeOrigin = origin == null ? Vec3.ZERO : origin;
        shakeDurationNanos = Math.max(1L, ticks) * 50_000_000L;
        shakeEndNanos = System.nanoTime() + shakeDurationNanos;
        shakeStrength = Math.max(0.0F, strength);
    }

    public static float intensity(Vec3 cameraPosition) {
        long remaining = shakeEndNanos - System.nanoTime();
        if (remaining <= 0L || cameraPosition == null) return 0.0F;
        double distance = cameraPosition.distanceTo(shakeOrigin);
        if (distance >= 64.0D) return 0.0F;
        float time = (float)Math.min(1.0D, (double)remaining / (double)shakeDurationNanos);
        float range = (float)(1.0D - distance / 64.0D);
        return shakeStrength * time * range;
    }
}
