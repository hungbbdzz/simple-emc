package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.velorise.emc.EchoboundDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class EchoboundDragonRenderer extends MobRenderer<EchoboundDragonEntity, EchoboundDragonModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/enderdragon/dragon.png");
    private static final ResourceLocation EYES = new ResourceLocation("textures/entity/enderdragon/dragon_eyes.png");
    private static final ResourceLocation EXPLODING = new ResourceLocation("textures/entity/enderdragon/dragon_exploding.png");

    public EchoboundDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new EchoboundDragonModel(context.bakeLayer(EchoboundDragonModel.LAYER)), 2.5F);
        addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() { return RenderType.eyes(EYES); }
        });
    }

    @Override public ResourceLocation getTextureLocation(EchoboundDragonEntity entity) { return TEXTURE; }

    @Override
    protected void setupRotations(EchoboundDragonEntity entity, PoseStack poseStack, float bob, float yHeadRot, float partialTick) {
        if (entity.isDeadOrDying()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yHeadRot));
        } else {
            super.setupRotations(entity, poseStack, bob, yHeadRot, partialTick);
            float bank = entity.clientBankDegrees(partialTick);
            if (Math.abs(bank) > 0.01F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(bank));
            }
            float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            if (!entity.isClientSlamming() && Math.abs(pitch) > 0.01F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch * 0.45F));
            }
        }
    }

    @Override
    protected void scale(EchoboundDragonEntity entity, PoseStack pose, float partialTick) {
        float scale = 1.30F + entity.getPhantomSize() * 0.10F;
        pose.scale(scale, scale, scale);
        if (!entity.isDeadOrDying() && entity.isClientSlamming() && entity.getDeltaMovement().y < -0.25D) {
            pose.mulPose(Axis.XP.rotationDegrees(58.0F));
        }
    }

    @Override
    public void render(EchoboundDragonEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (entity.deathTime > 0) {
            float deathProgress = Math.min(1.0F, (entity.deathTime + partialTick) / 240.0F);

            this.model.attackTime = 0.0F;
            this.model.riding = false;
            this.model.young = false;
            this.model.prepareMobModel(entity, 0.0F, 0.0F, partialTick);
            this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);

            poseStack.pushPose();
            this.setupRotations(entity, poseStack, 0.0F, entity.getYRot(), partialTick);
            this.scale(entity, poseStack, partialTick);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);

            VertexConsumer baseConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            this.model.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            VertexConsumer explosionConsumer = buffers.getBuffer(RenderType.dragonExplosionAlpha(EXPLODING));
            this.model.renderToBuffer(poseStack, explosionConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, deathProgress);
            poseStack.popPose();

            // Beams use world/entity space, not the inverted model-space transform.
            poseStack.pushPose();
            renderCrystalBeam(entity, partialTick, poseStack, buffers);
            renderDeathBeams(deathProgress, poseStack, buffers);
            poseStack.popPose();
        } else {
            super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
            poseStack.pushPose();
            this.scale(entity, poseStack, partialTick);
            this.setupRotations(entity, poseStack, 0.0F, entity.getYRot(), partialTick);
            renderCrystalBeam(entity, partialTick, poseStack, buffers);
            poseStack.popPose();
        }
    }

    private void renderDeathBeams(float deathProgress, PoseStack poseStack, MultiBufferSource buffers) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.0D, 0.0D);
        VertexConsumer beamConsumer = buffers.getBuffer(RenderType.lightning());
        java.util.Random rand = new java.util.Random(432L);
        int beamCount = (int)((deathProgress + deathProgress * deathProgress) / 2.0F * 45.0F);

        for (int i = 0; i < beamCount; i++) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rand.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(rand.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rand.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(rand.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(rand.nextFloat() * 360.0F + deathProgress * 90.0F));

            float length = rand.nextFloat() * 25.0F + 8.0F + deathProgress * 12.0F;
            float width = rand.nextFloat() * 2.5F + 1.2F + deathProgress * 2.5F;
            Matrix4f matrix = poseStack.last().pose();
            addForgeBeamSpike(beamConsumer, matrix, length, width);
        }
        poseStack.popPose();
    }

    private void renderCrystalBeam(EchoboundDragonEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        EndCrystal crystal = findNearestCrystal(entity);
        if (crystal == null) return;

        double ex = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        double cx = Mth.lerp(partialTick, crystal.xOld, crystal.getX());
        double cy = Mth.lerp(partialTick, crystal.yOld, crystal.getY()) + 1.0D;
        double cz = Mth.lerp(partialTick, crystal.zOld, crystal.getZ());

        float dx = (float)(cx - ex);
        float dy = (float)(cy - ey - 2.0D);
        float dz = (float)(cz - ez);
        float horizontal = Mth.sqrt(dx * dx + dz * dz);
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001F) return;

        poseStack.pushPose();
        poseStack.translate(0.0D, 2.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotation((float)(-Math.atan2(dz, dx)) - Mth.HALF_PI));
        poseStack.mulPose(Axis.XP.rotation((float)(-Math.atan2(horizontal, dy)) - Mth.HALF_PI));
        poseStack.mulPose(Axis.YP.rotation((entity.tickCount + partialTick) * 0.18F));

        float width = 0.55F + 0.08F * Mth.sin((entity.tickCount + partialTick) * 0.35F);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer beamConsumer = buffers.getBuffer(RenderType.lightning());
        addForgeBeamSpike(beamConsumer, matrix, length, width);
        poseStack.popPose();
    }

    private EndCrystal findNearestCrystal(EchoboundDragonEntity entity) {
        AABB searchBox = entity.getBoundingBox().inflate(96.0D, 64.0D, 96.0D);
        EndCrystal nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (EndCrystal crystal : entity.level().getEntitiesOfClass(EndCrystal.class, searchBox,
            c -> c.isAlive() && c.getTags().contains(EchoboundDragonEntity.CRYSTAL_TAG))) {
            double dist = crystal.distanceToSqr(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = crystal;
            }
        }
        return nearest;
    }

    private void addForgeBeamSpike(VertexConsumer consumer, Matrix4f matrix, float length, float width) {
        consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        consumer.vertex(matrix, -0.866F * width, length, -0.5F * width).color(210, 120, 255, 0).endVertex();
        consumer.vertex(matrix, 0.866F * width, length, -0.5F * width).color(210, 120, 255, 0).endVertex();
        consumer.vertex(matrix, 0.0F, length, width).color(210, 120, 255, 0).endVertex();
        consumer.vertex(matrix, -0.866F * width, length, -0.5F * width).color(210, 120, 255, 0).endVertex();
    }
}
