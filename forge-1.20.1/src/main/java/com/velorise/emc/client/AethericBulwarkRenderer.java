package com.velorise.emc.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Shulker;

/** Standard scale Shulker renderer for Aetheric Bulwark boss. */
public final class AethericBulwarkRenderer extends ShulkerRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/shulker/shulker_purple.png");

    public AethericBulwarkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(Shulker entity) {
        return TEXTURE;
    }
}
