package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.emc.AlchemicalHourglassBlockEntity;
import com.velorise.emc.AuraNodeBlockEntity;
import com.velorise.emc.CoreAuraMachineBlockEntity;
import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.common.AuraNodeProfile;
import com.velorise.emc.common.CoreAuraMachineProfile;
import com.velorise.emc.common.CondensedCoreGeneratorProfile;
import com.velorise.emc.common.HourglassProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws the selected Core machine's effective area while Vanilla debug hitboxes are enabled. */
public final class RangeVisualizationRenderer {
    private RangeVisualizationRenderer() {}

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;
        RangeInfo info = resolve(minecraft.level.getBlockEntity(pos), minecraft.level.getBlockState(pos).getBlock());
        if (info == null || info.radius <= 0) return;

        Vec3 cameraPos = camera.getPosition();
        AABB area = new AABB(
            pos.getX() - info.radius,
            pos.getY() - info.radius,
            pos.getZ() - info.radius,
            pos.getX() + info.radius + 1.0D,
            pos.getY() + info.radius + 1.0D,
            pos.getZ() + info.radius + 1.0D
        ).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float[] color = color(info.tier);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        poseStack.pushPose();
        LevelRenderer.renderLineBox(poseStack, lines, area, color[0], color[1], color[2], 0.85F);
        poseStack.popPose();
    }

    private static RangeInfo resolve(BlockEntity blockEntity, net.minecraft.world.level.block.Block block) {
        if (blockEntity instanceof AlchemicalHourglassBlockEntity hourglass) {
            int core = hourglass.getInstalledCoreLevel();
            return new RangeInfo(HourglassProfile.radius(core), core);
        }
        if (blockEntity instanceof CoreAuraMachineBlockEntity aura) {
            int core = aura.getCoreTierLevel();
            return new RangeInfo(CoreAuraMachineProfile.range(aura.getMachineKind(), core), core);
        }
        if (blockEntity instanceof AuraNodeBlockEntity node) {
            int tier = node.getTier();
            return new RangeInfo(AuraNodeProfile.outputRadius(tier), tier);
        }
        int generatorTier = generatorTier(block);
        if (generatorTier > 0) {
            return new RangeInfo(CondensedCoreGeneratorProfile.radius(generatorTier), generatorTier);
        }
        return null;
    }

    private static int generatorTier(net.minecraft.world.level.block.Block block) {
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_I.get()) return 1;
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_II.get()) return 2;
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_III.get()) return 3;
        if (block == VeloriseEMC.CONDENSED_CORE_BLOCK_IV.get()) return 4;
        return 0;
    }

    private static float[] color(int tier) {
        return switch (Math.max(0, Math.min(4, tier))) {
            case 1 -> new float[]{0.30F, 0.95F, 0.42F};
            case 2 -> new float[]{1.00F, 0.30F, 0.08F};
            case 3 -> new float[]{0.78F, 0.30F, 1.00F};
            case 4 -> new float[]{0.08F, 0.90F, 1.00F};
            default -> new float[]{0.85F, 0.85F, 0.85F};
        };
    }

    private record RangeInfo(int radius, int tier) {}
}
