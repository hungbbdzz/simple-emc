package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velorise.emc.AethericBulwarkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Shulker;

/** Standard scale Shulker renderer for Aetheric Bulwark boss with smooth breathing and cast/slam recoil. */
public final class AethericBulwarkRenderer extends ShulkerRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/shulker/shulker_purple.png");

    public AethericBulwarkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.65F;
    }

    @Override
    protected void scale(Shulker entity, PoseStack poseStack, float partialTick) {
        float age = entity.tickCount + partialTick;
        float breathe = 1.0F + Mth.sin(age * 0.12F) * 0.02F;
        float sx = breathe;
        float sy = breathe;
        float sz = breathe;
        if (entity instanceof AethericBulwarkEntity boss) {
            if (boss.clientPulseTicks() > 0) {
                float p = Mth.clamp((boss.clientPulseTicks() - partialTick) / 20.0F, 0.0F, 1.0F);
                float pulse = Mth.sin(p * Mth.PI) * 0.09F;
                sx += pulse;
                sy += pulse * 1.2F;
                sz += pulse;
            }
            if (boss.clientSlamTicks() > 0) {
                float p = Mth.clamp((boss.clientSlamTicks() - partialTick) / 18.0F, 0.0F, 1.0F);
                float slam = Mth.sin(p * Mth.PI) * 0.12F;
                sx += slam;
                sy -= slam * 0.85F;
                sz += slam;
            }
        }
        poseStack.scale(sx, sy, sz);
    }

    @Override
    public ResourceLocation getTextureLocation(Shulker entity) {
        return TEXTURE;
    }
}
