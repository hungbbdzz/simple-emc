package com.velorise.emc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.velorise.emc.AbstractResonantFurnaceBlockEntity;
import com.velorise.emc.AlchemicalHourglassBlockEntity;
import com.velorise.emc.CoreAuraMachineBlockEntity;
import com.velorise.emc.CoreTierData;
import com.velorise.emc.DeepboundEnderChestBlockEntity;
import com.velorise.emc.ResonanceJukeboxBlockEntity;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Renders the installed Wireless Core as a compact machine socket marker. */
public final class CoreSocketRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public CoreSocketRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        int tier = resolveCoreTier(blockEntity);
        if (tier <= 0) return;
        renderSocket(blockEntity, tier, poseStack, buffer, packedLight, packedOverlay,
            resolveHeight(blockEntity), 0.22F);
    }

    /** Shared by machines that already have a dedicated renderer. */
    public static void renderSocket(BlockEntity blockEntity, int coreTier, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay,
                                    double y, float scale) {
        if (blockEntity == null || coreTier <= 0) return;
        if (blockEntity instanceof AbstractResonantFurnaceBlockEntity || blockEntity instanceof ResonanceJukeboxBlockEntity) {
            return;
        }
        WirelessCoreTier tier = WirelessCoreTier.byLevel(coreTier);
        ItemStack core = CoreTierData.createCoreStack(tier);
        float markerScale = scale * 0.55F;

        double offset = blockEntity instanceof DeepboundEnderChestBlockEntity ? 0.35D : 0.39D;
        double[] offsets = {-offset, offset};
        for (double dx : offsets) {
            for (double dz : offsets) {
                poseStack.pushPose();
                poseStack.translate(0.5D + dx, y, 0.5D + dz);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.scale(markerScale, markerScale, markerScale);
                renderCore(core, tier, blockEntity, poseStack, buffer, packedLight, packedOverlay);
                poseStack.popPose();
            }
        }
    }

    private static void renderCore(ItemStack core, WirelessCoreTier tier, BlockEntity blockEntity,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
            core, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer,
            blockEntity.getLevel(), tier.level());
    }

    private static int resolveCoreTier(BlockEntity blockEntity) {
        if (blockEntity instanceof AbstractResonantFurnaceBlockEntity furnace) return furnace.getCoreTierLevel();
        if (blockEntity instanceof DeepboundEnderChestBlockEntity chest) return chest.getCoreTier().level();
        if (blockEntity instanceof CoreAuraMachineBlockEntity machine) return machine.getCoreTierLevel();
        if (blockEntity instanceof ResonanceJukeboxBlockEntity jukebox) return jukebox.getCoreTierLevel();
        if (blockEntity instanceof AlchemicalHourglassBlockEntity hourglass) return hourglass.getInstalledCoreLevel();
        return 0;
    }

    private static double resolveHeight(BlockEntity blockEntity) {
        return blockEntity instanceof DeepboundEnderChestBlockEntity ? 0.91D : 1.0125D;
    }
}
