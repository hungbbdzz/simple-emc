package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.velorise.emc.InfernalCrucibleEntity;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.phys.Vec3;

/** Enlarged vanilla-Blaze model with dynamic flight tilt, hover bobbing, and cast pulse scaling. */
public final class InfernalCrucibleRenderer extends BlazeRenderer {
    public InfernalCrucibleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.8F;
    }

    @Override
    protected void setupRotations(Blaze entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick);
        if (entity.isDeadOrDying()) return;

        float age = entity.tickCount + partialTick;
        // Smooth vertical hover bobbing
        poseStack.translate(0.0F, Mth.sin(age * 0.11F) * 0.14F, 0.0F);

        Vec3 vel = entity.getDeltaMovement();
        float yawRad = yBodyRot * Mth.DEG_TO_RAD;
        float sinYaw = Mth.sin(yawRad);
        float cosYaw = Mth.cos(yawRad);
        // Project world velocity onto local forward and lateral axes
        float forwardSpeed = (float) (-vel.x * sinYaw + vel.z * cosYaw);
        float strafeSpeed = (float) (vel.x * cosYaw + vel.z * sinYaw);

        float pitchTilt = Mth.clamp(forwardSpeed * 28.0F, -22.0F, 26.0F);
        float rollTilt = Mth.clamp(-strafeSpeed * 24.0F, -18.0F, 18.0F);

        if (entity instanceof InfernalCrucibleEntity boss && boss.clientDashTicks() > 0) {
            pitchTilt = 32.0F;
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(pitchTilt));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollTilt));
    }

    @Override
    protected void scale(Blaze entity, PoseStack poseStack, float partialTick) {
        float age = entity.tickCount + partialTick;
        float breathe = 1.0F + Mth.sin(age * 0.14F) * 0.025F;
        float castPulse = 0.0F;
        if (entity instanceof InfernalCrucibleEntity boss && boss.clientCastPulseTicks() > 0) {
            float progress = (boss.clientCastPulseTicks() - partialTick) / 18.0F;
            castPulse = Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * Mth.PI) * 0.11F;
        }
        float s = 2.35F * (breathe + castPulse);
        poseStack.scale(s, s, s);
    }
}
