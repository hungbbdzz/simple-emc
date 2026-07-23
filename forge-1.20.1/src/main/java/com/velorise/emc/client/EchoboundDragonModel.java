package com.velorise.emc.client;

import com.velorise.emc.EchoboundDragonEntity;
import com.velorise.emc.VeloriseEMC;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Ender-Dragon-scale visual model without coupling the entity to vanilla EnderDragonFight. */
public final class EchoboundDragonModel extends HierarchicalModel<EchoboundDragonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        new ResourceLocation(VeloriseEMC.MODID, "echobound_dragon"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart[] neck = new ModelPart[5];
    private final ModelPart[] tail = new ModelPart[8];
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private final ModelPart[] claws = new ModelPart[4];

    public EchoboundDragonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.leftWing = root.getChild("left_wing");
        this.leftWingTip = leftWing.getChild("left_wing_tip");
        this.rightWing = root.getChild("right_wing");
        this.rightWingTip = rightWing.getChild("right_wing_tip");
        for (int i = 0; i < neck.length; i++) neck[i] = root.getChild("neck_" + i);
        for (int i = 0; i < tail.length; i++) tail[i] = root.getChild("tail_" + i);
        legs[0] = root.getChild("left_front_leg");
        legs[1] = root.getChild("right_front_leg");
        legs[2] = root.getChild("left_hind_leg");
        legs[3] = root.getChild("right_hind_leg");
        shins[0] = legs[0].getChild("left_front_shin");
        shins[1] = legs[1].getChild("right_front_shin");
        shins[2] = legs[2].getChild("left_hind_shin");
        shins[3] = legs[3].getChild("right_hind_shin");
        claws[0] = shins[0].getChild("left_front_claw");
        claws[1] = shins[1].getChild("right_front_claw");
        claws[2] = shins[2].getChild("left_hind_claw");
        claws[3] = shins[3].getChild("right_hind_claw");
    }

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(EchoboundDragonEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        resetAnimatedOffsets();

        float flap = ageInTicks * 0.28F;
        float slowWave = ageInTicks * 0.105F;
        boolean slamming = entity.isClientSlamming();
        boolean diving = entity.getDeltaMovement().y < -0.08D
            && entity.getDeltaMovement().horizontalDistanceSqr() > 0.08D;

        if (entity.isDeadOrDying()) {
            animateDeath(entity, flap, slowWave);
            return;
        }

        // Body breathing and hurt recoil make the torso participate instead of leaving the
        // wings as the only visibly animated parts.
        body.y = 4.0F + Mth.sin(flap * 0.52F) * 0.65F;
        body.xRot = diving ? -0.10F : Mth.sin(slowWave) * 0.025F;
        root.zRot = entity.hurtTime > 0 ? Mth.sin(entity.hurtTime * 1.65F) * 0.055F : 0.0F;

        float headSway = Mth.sin(slowWave * 1.35F) * 0.15F;
        float headBob = Mth.sin(flap * 0.47F) * 0.07F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.45F + headSway;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.35F + headBob + (diving ? -0.15F : 0.0F);
        head.zRot = Mth.sin(slowWave * 0.83F) * 0.035F;
        head.x = Mth.sin(slowWave * 1.35F) * 1.15F;
        head.y = 5.0F + Mth.cos(flap * 0.47F) * 0.45F;
        jaw.xRot = slamming ? 0.72F
            : diving ? 0.58F : 0.10F + (Mth.sin(flap * 0.55F) + 1.0F) * 0.11F;

        float wingPower = slamming ? 0.35F : 1.0F;
        leftWing.xRot = 0.12F - Mth.cos(flap) * 0.20F * wingPower;
        leftWing.yRot = -0.22F + (diving ? 0.10F : 0.0F);
        leftWing.zRot = -0.22F - Mth.sin(flap) * 0.76F * wingPower;
        leftWingTip.zRot = 0.35F + Mth.sin(flap + 1.8F) * 0.46F * wingPower;
        rightWing.xRot = leftWing.xRot;
        rightWing.yRot = -leftWing.yRot;
        rightWing.zRot = -leftWing.zRot;
        rightWingTip.zRot = -leftWingTip.zRot;

        // Neck pieces are siblings in this custom model, so position offsets are required
        // in addition to rotation to form a visible, connected serpentine motion.
        for (int i = 0; i < neck.length; i++) {
            float phase = slowWave * 1.55F - i * 0.50F;
            float follow = (i + 1.0F) / neck.length;
            neck[i].x = Mth.sin(phase) * (0.45F + follow * 1.35F);
            neck[i].y = 9.0F - i + Mth.cos(phase * 0.82F) * 0.32F;
            neck[i].yRot = head.yRot * follow + Mth.sin(phase) * 0.055F;
            neck[i].xRot = Mth.sin(flap * 0.42F - i * 0.48F) * 0.10F;
            neck[i].zRot = Mth.sin(phase * 0.72F) * 0.025F;
        }

        // Tail segments sweep progressively farther from the body, producing an actual wave
        // instead of several cubes rotating almost invisibly around their own centres.
        for (int i = 0; i < tail.length; i++) {
            float phase = slowWave * 1.85F - i * 0.58F;
            float reach = 0.55F + i * 0.48F;
            tail[i].x = Mth.sin(phase) * reach;
            tail[i].y = 13.0F + i * 0.45F + Mth.cos(phase * 0.70F) * (0.22F + i * 0.035F);
            tail[i].yRot = Mth.sin(phase) * (0.13F + i * 0.026F);
            tail[i].xRot = Mth.cos(phase * 0.78F) * (0.035F + i * 0.006F);
            tail[i].zRot = Mth.sin(phase * 0.55F) * 0.018F;
        }

        animateLegs(entity, limbSwing, limbSwingAmount, flap, diving, slamming);
    }

    private void resetAnimatedOffsets() {
        root.xRot = 0.0F;
        root.yRot = 0.0F;
        root.zRot = 0.0F;
        body.x = 0.0F;
        body.y = 4.0F;
        body.z = 8.0F;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        body.zRot = 0.0F;
        head.x = 0.0F;
        head.y = 5.0F;
        head.z = -42.0F;
        head.xRot = 0.0F;
        head.yRot = 0.0F;
        head.zRot = 0.0F;
        for (int i = 0; i < neck.length; i++) {
            neck[i].x = 0.0F;
            neck[i].y = 9.0F - i;
            neck[i].z = -8.0F - i * 8.0F;
            neck[i].xRot = neck[i].yRot = neck[i].zRot = 0.0F;
        }
        for (int i = 0; i < tail.length; i++) {
            tail[i].x = 0.0F;
            tail[i].y = 13.0F + i * 0.45F;
            tail[i].z = 60.0F + i * 8.0F;
            tail[i].xRot = tail[i].yRot = tail[i].zRot = 0.0F;
        }
    }

    private void animateDeath(EchoboundDragonEntity entity, float flap, float slowWave) {
        float progress = entity.deathAnimationProgress(0.0F);
        float strength = 1.0F - progress * 0.72F;
        float deathPulse = Mth.sin(slowWave * 1.7F) * strength;

        body.y = 4.0F + Mth.sin(flap * 0.22F) * 0.35F;
        body.xRot = -0.035F + progress * 0.06F;
        head.xRot = 0.10F + progress * 0.16F;
        head.yRot = deathPulse * 0.11F;
        head.zRot = Mth.sin(slowWave * 0.65F) * 0.025F * strength;
        head.x = deathPulse * 0.75F;
        jaw.xRot = 0.24F + Mth.sin(flap * 0.25F) * 0.06F;

        float dyingFlap = Mth.sin(flap * 0.30F) * 0.20F * strength;
        leftWing.xRot = 0.20F + progress * 0.18F;
        leftWing.yRot = -0.18F;
        leftWing.zRot = -0.34F - dyingFlap;
        leftWingTip.zRot = 0.50F + dyingFlap * 0.65F;
        rightWing.xRot = leftWing.xRot;
        rightWing.yRot = -leftWing.yRot;
        rightWing.zRot = -leftWing.zRot;
        rightWingTip.zRot = -leftWingTip.zRot;

        for (int i = 0; i < neck.length; i++) {
            float phase = slowWave - i * 0.48F;
            float follow = (i + 1.0F) / neck.length;
            neck[i].x = Mth.sin(phase) * follow * 1.0F * strength;
            neck[i].y = 9.0F - i + Mth.cos(phase) * 0.22F * strength;
            neck[i].yRot = Mth.sin(phase) * 0.09F * strength;
            neck[i].xRot = progress * 0.055F * follow;
        }
        for (int i = 0; i < tail.length; i++) {
            float phase = slowWave * 1.45F - i * 0.52F;
            float reach = (0.45F + i * 0.42F) * strength;
            tail[i].x = Mth.sin(phase) * reach;
            tail[i].y = 13.0F + i * 0.45F + Mth.cos(phase) * 0.20F * strength;
            tail[i].yRot = Mth.sin(phase) * (0.10F + i * 0.022F) * strength;
            tail[i].xRot = 0.025F + progress * i * 0.004F;
        }
        for (int i = 0; i < legs.length; i++) {
            float phase = ((i == 0 || i == 3) ? 0.0F : Mth.PI);
            float twitch = Mth.sin(flap * 0.26F + phase) * 0.07F * strength;
            legs[i].xRot = 0.55F + progress * 0.24F + twitch;
            shins[i].xRot = 0.42F + progress * 0.30F - twitch;
            claws[i].xRot = -0.10F + twitch * 0.5F;
            legs[i].yRot = legs[i].zRot = 0.0F;
            shins[i].yRot = shins[i].zRot = 0.0F;
        }
    }

    private void animateLegs(EchoboundDragonEntity entity, float limbSwing, float limbSwingAmount,
                             float flap, boolean diving, boolean slamming) {
        for (int i = 0; i < legs.length; i++) {
            boolean diagonalA = i == 0 || i == 3;
            float phase = diagonalA ? 0.0F : Mth.PI;
            if (entity.onGround()) {
                float stride = Mth.cos(limbSwing * 0.6662F + phase) * limbSwingAmount;
                float lift = Math.max(0.0F, Mth.sin(limbSwing * 0.6662F + phase)) * limbSwingAmount;
                // A small idle pulse remains visible even when navigation momentarily reports zero limb swing.
                float idle = Mth.sin(flap * 0.42F + phase) * 0.055F;
                legs[i].xRot = stride * 0.95F + idle;
                shins[i].xRot = 0.30F + lift * 0.85F - stride * 0.20F - idle * 0.5F;
                claws[i].xRot = -0.18F - lift * 0.35F + idle * 0.35F;
            } else if (slamming) {
                // Brace all four limbs toward the impact instead of leaving the flight pose frozen.
                boolean front = i < 2;
                float brace = Mth.sin(flap * 0.55F + phase) * 0.08F;
                legs[i].xRot = (front ? 0.48F : 0.72F) + brace;
                shins[i].xRot = 0.82F - brace * 0.5F;
                claws[i].xRot = -0.25F;
            } else {
                boolean front = i < 2;
                float flightCycle = Mth.sin(flap * 0.72F + phase) * 0.11F;
                if (diving) {
                    legs[i].xRot = (front ? 1.02F : 1.18F) + flightCycle;
                    shins[i].xRot = -0.38F - flightCycle * 0.55F;
                    claws[i].xRot = 0.18F + flightCycle * 0.25F;
                } else {
                    legs[i].xRot = (front ? 1.26F : 1.39F) + flightCycle;
                    shins[i].xRot = 0.12F - flightCycle * 0.65F;
                    claws[i].xRot = 0.08F + flightCycle * 0.30F;
                }
            }
            legs[i].yRot = 0.0F;
            legs[i].zRot = 0.0F;
            shins[i].yRot = 0.0F;
            shins[i].zRot = 0.0F;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(176, 44).addBox(-6, -1, -24, 12, 5, 16)
            .texOffs(112, 30).addBox(-8, -8, -10, 16, 16, 16)
            .texOffs(112, 0).addBox(-5, -3, -22, 2, 2, 4)
            .texOffs(112, 0).addBox(3, -3, -22, 2, 2, 4)
            .texOffs(0, 0).addBox(-6, -15, -3, 3, 8, 4)
            .texOffs(0, 0).addBox(3, -15, -3, 3, 8, 4), PartPose.offset(0, 5, -42));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(176, 65)
            .addBox(-6, 0, -16, 12, 4, 16), PartPose.offset(0, 4, -8));

        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-12, 0, -16, 24, 24, 64)
            .texOffs(220, 53).addBox(-1, -7, -10, 2, 7, 12)
            .texOffs(220, 53).addBox(-1, -7, 10, 2, 7, 12)
            .texOffs(220, 53).addBox(-1, -7, 30, 2, 7, 12), PartPose.offset(0, 4, 8));

        for (int i = 0; i < 5; i++) {
            root.addOrReplaceChild("neck_" + i, segment(), PartPose.offset(0, 9 - i, -8 - i * 8));
        }
        for (int i = 0; i < 8; i++) {
            float scale = Math.max(0.45F, 1.0F - i * 0.07F);
            root.addOrReplaceChild("tail_" + i, CubeListBuilder.create()
                .texOffs(192, 104).addBox(-5 * scale, -5 * scale, -5 * scale, 10 * scale, 10 * scale, 10 * scale)
                .texOffs(48, 0).addBox(-1, -9 * scale, -3, 2, 4 * scale, 6),
                PartPose.offset(0, 13 + i * 0.45F, 60 + i * 8));
        }

        PartDefinition lw = root.addOrReplaceChild("left_wing", CubeListBuilder.create().mirror()
            .texOffs(112, 88).addBox(0, -4, -4, 56, 8, 8)
            .texOffs(-56, 88).addBox(0, 0, 2, 56, 0, 56), PartPose.offset(12, 5, 2));
        lw.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().mirror()
            .texOffs(112, 136).addBox(0, -2, -2, 56, 4, 4)
            .texOffs(-56, 144).addBox(0, 0, 2, 56, 0, 56), PartPose.offset(56, 0, 0));
        PartDefinition rw = root.addOrReplaceChild("right_wing", CubeListBuilder.create()
            .texOffs(112, 88).addBox(-56, -4, -4, 56, 8, 8)
            .texOffs(-56, 88).addBox(-56, 0, 2, 56, 0, 56), PartPose.offset(-12, 5, 2));
        rw.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
            .texOffs(112, 136).addBox(-56, -2, -2, 56, 4, 4)
            .texOffs(-56, 144).addBox(-56, 0, 2, 56, 0, 56), PartPose.offset(-56, 0, 0));

        addLeg(root, "left_front", 12, 20, 2, false);
        addLeg(root, "right_front", -12, 20, 2, false);
        addLeg(root, "left_hind", 16, 16, 42, true);
        addLeg(root, "right_hind", -16, 16, 42, true);
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static CubeListBuilder segment() {
        return CubeListBuilder.create().texOffs(192, 104).addBox(-5, -5, -5, 10, 10, 10)
            .texOffs(48, 0).addBox(-1, -9, -3, 2, 4, 6);
    }

    private static void addLeg(PartDefinition root, String name, float x, float y, float z, boolean hind) {
        int size = hind ? 16 : 8;
        int length = hind ? 32 : 24;
        PartDefinition upper = root.addOrReplaceChild(name + "_leg", CubeListBuilder.create().texOffs(hind ? 0 : 112, hind ? 0 : 104)
            .addBox(-size / 2.0F, -4, -size / 2.0F, size, length, size), PartPose.offset(x, y, z));
        PartDefinition lower = upper.addOrReplaceChild(name + "_shin", CubeListBuilder.create().texOffs(196, 0)
            .addBox(-3, -1, -3, 6, 24, 6), PartPose.offset(0, length - 4, -1));
        lower.addOrReplaceChild(name + "_claw", CubeListBuilder.create().texOffs(144, 104)
            .addBox(-4, 0, -12, 8, 4, 16), PartPose.offset(0, 22, 0));
    }
}
