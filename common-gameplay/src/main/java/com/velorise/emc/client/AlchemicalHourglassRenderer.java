package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velorise.emc.AlchemicalHourglassBlock;
import com.velorise.emc.AlchemicalHourglassBlockEntity;
import com.velorise.emc.VeloriseEMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AlchemicalHourglassRenderer implements BlockEntityRenderer<AlchemicalHourglassBlockEntity> {
    private static final ItemStack CLOCK_STACK = new ItemStack(Items.CLOCK);

    public AlchemicalHourglassRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AlchemicalHourglassBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        CoreSocketRenderer.renderSocket(
            blockEntity, blockEntity.getInstalledCoreLevel(), poseStack, buffer,
            combinedLight, combinedOverlay, 0.08D, 0.16F
        );

        if (blockEntity.getLevel() == null) return;
        long time = blockEntity.getLevel().getGameTime();
        float gameTime = time + partialTick;
        boolean active = blockEntity.getBlockState().hasProperty(AlchemicalHourglassBlock.ACTIVE)
            ? blockEntity.getBlockState().getValue(AlchemicalHourglassBlock.ACTIVE)
            : blockEntity.isMachineEnabled();
        int coreLevel = Math.max(1, blockEntity.getInstalledCoreLevel());

        // 1. Render Center Clock (Nucleus)
        poseStack.pushPose();
        double clockY = 0.5D + (active ? 0.0D : 0.03D * Math.sin(gameTime * 0.05D));
        poseStack.translate(0.5D, clockY, 0.5D);
        float clockSpin = active ? gameTime * 4.0F : (gameTime * 1.2F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(clockSpin));
        poseStack.scale(0.35F, 0.35F, 0.35F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            CLOCK_STACK, ItemDisplayContext.FIXED, combinedLight, combinedOverlay,
            poseStack, buffer, blockEntity.getLevel(), 0
        );
        poseStack.popPose();

        // 2. Tier 1-4 Fuels:
        // Tier 1: Vanilla Coal
        // Tier 2: + Alchemical Coal
        // Tier 3: + Mobius Fuel
        // Tier 4: + Aeternalis Fuel
        ItemStack fuel1 = new ItemStack(Items.COAL);
        ItemStack fuel2 = coreLevel >= 2 ? new ItemStack(VeloriseEMC.ALCHEMICAL_COAL.get()) : ItemStack.EMPTY;
        ItemStack fuel3 = coreLevel >= 3 ? new ItemStack(VeloriseEMC.MOBIUS_FUEL.get()) : ItemStack.EMPTY;
        ItemStack fuel4 = coreLevel >= 4 ? new ItemStack(VeloriseEMC.AETERNALIS_FUEL.get()) : ItemStack.EMPTY;

        double r = 0.23D;

        if (!active) {
            // NORMAL STATE: Floating up and down slowly
            double bob1 = 0.04D * Math.sin(gameTime * 0.06D);
            double bob2 = 0.04D * Math.sin(gameTime * 0.06D + 1.5D);
            double bob3 = 0.04D * Math.sin(gameTime * 0.06D + 3.0D);
            double bob4 = 0.04D * Math.sin(gameTime * 0.06D + 4.5D);

            renderFuel(fuel1, 0.26D, 0.5D + bob1, 0.5D, gameTime * 1.5F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);
            renderFuel(fuel2, 0.74D, 0.5D + bob2, 0.5D, gameTime * 1.5F + 60.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);
            renderFuel(fuel3, 0.5D, 0.5D + bob3, 0.74D, gameTime * 1.5F + 120.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);
            renderFuel(fuel4, 0.5D, 0.74D + bob4, 0.5D, gameTime * 1.5F + 180.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);
        } else {
            // ACTIVE STATE: Atomic Orbital Rotations around the Clock Nucleus
            float speed = gameTime * 0.12F;

            // Orbit 1: Tilted around X/Z plane (30 deg tilt)
            float a1 = speed;
            renderFuel(fuel1, 0.5D + r * Math.cos(a1), 0.5D + r * Math.sin(a1) * 0.5D, 0.5D + r * Math.sin(a1) * 0.866D,
                gameTime * 5.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);

            // Orbit 2: Tilted around Y/Z plane (-45 deg tilt)
            float a2 = speed + 1.57F;
            renderFuel(fuel2, 0.5D + r * Math.cos(a2) * 0.707D, 0.5D + r * Math.sin(a2), 0.5D + r * Math.cos(a2) * 0.707D,
                gameTime * 6.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);

            // Orbit 3: Tilted around diagonal plane (60 deg tilt)
            float a3 = speed + 3.14F;
            renderFuel(fuel3, 0.5D + r * Math.sin(a3) * 0.866D, 0.5D + r * Math.cos(a3), 0.5D + r * Math.sin(a3) * 0.5D,
                gameTime * 7.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);

            // Orbit 4: Tilted around vertical plane (-30 deg tilt)
            float a4 = speed + 4.71F;
            renderFuel(fuel4, 0.5D + r * Math.sin(a4) * 0.5D, 0.5D + r * Math.sin(a4) * 0.866D, 0.5D + r * Math.cos(a4),
                gameTime * 8.0F, poseStack, buffer, combinedLight, combinedOverlay, blockEntity);
        }
    }

    private void renderFuel(ItemStack stack, double x, double y, double z, float spinDegrees,
                            PoseStack poseStack, MultiBufferSource buffer,
                            int light, int overlay, AlchemicalHourglassBlockEntity be) {
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spinDegrees));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(spinDegrees * 0.5F));
        poseStack.scale(0.24F, 0.24F, 0.24F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            stack, ItemDisplayContext.FIXED, light, overlay,
            poseStack, buffer, be.getLevel(), 0
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AlchemicalHourglassBlockEntity blockEntity) {
        return true;
    }
}
