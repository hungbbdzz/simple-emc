package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.monster.Blaze;

/** Enlarged vanilla-Blaze model used as the first-pass body for the living forge boss. */
public final class InfernalCrucibleRenderer extends BlazeRenderer {
    public InfernalCrucibleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.8F;
    }

    @Override
    protected void scale(Blaze entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.35F, 2.35F, 2.35F);
    }
}
