package com.velorise.emc;

import com.velorise.emc.common.WorldProgressionObjective;
import com.velorise.emc.common.WorldProgressionState;
import com.velorise.emc.common.WorldTierStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Locator and altar key for The Infernal Crucible. */
public final class EyeOfTheCrucibleItem extends Item {
    public EyeOfTheCrucibleItem(Properties properties) { super(properties.stacksTo(16).fireResistant()); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player) || level.dimension() != Level.NETHER) return InteractionResult.PASS;
        BlockPos clicked = context.getClickedPos();
        BlockPos center = InfernalCrucibleArenaService.findAltarCenter((ServerLevel) level, clicked);
        if (center == null || !InfernalCrucibleArenaService.isAltar(clicked, center)) return InteractionResult.PASS;

        if (!player.getAbilities().instabuild) {
            WorldProgressionState state = WorldProgressionData.get(player.server).snapshot();
            for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
                if (objective.stage() == WorldTierStage.NETHER_CONQUEST
                    && objective != WorldProgressionObjective.INFERNAL_CRUCIBLE
                    && !state.objectiveComplete(objective)) {
                    player.displayClientMessage(Component.literal("The crucible rejects the eye. Missing: "
                        + objective.displayName() + " (" + state.count(objective) + "/" + state.required(objective) + ")."), true);
                    return InteractionResult.FAIL;
                }
            }
        }

        if (!ArenaSummonService.start((ServerLevel) level, center, ArenaSummonService.BossKind.INFERNAL_CRUCIBLE)) {
            player.displayClientMessage(Component.literal("The altar is already channeling or the boss is present."), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        player.displayClientMessage(Component.literal("The crucible begins drawing heat from the arena..."), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (level.dimension() != Level.NETHER) {
            serverPlayer.displayClientMessage(Component.literal("The Eye of the Crucible only responds in the Nether."), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos target = BossArenaRegistryData.get(serverPlayer.server)
            .nearest(BossArenaRegistryData.Kind.INFERNAL, player.blockPosition());
        if (target == null) target = InfernalCrucibleArenaService.locateArena(serverLevel, player.blockPosition());
        if (target == null) {
            serverPlayer.displayClientMessage(Component.literal("The eye cannot find a Nether Fortress suitable for the arena."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!InfernalCrucibleArenaService.hasArena((ServerLevel) level, target)
            && (player.getAbilities().instabuild || WorldProgressionData.get(serverPlayer.server).snapshot().worldTier() >= 1)) {
            InfernalCrucibleArenaService.buildArena((ServerLevel) level, target);
        }
        Vec3 start = player.getEyePosition();
        Vec3 dest = Vec3.atCenterOf(target);
        Vec3 delta = dest.subtract(start);
        Vec3 dir = delta.normalize();

        for (int i = 2; i <= 60; i++) {
            double d = i * 0.5D;
            double px = start.x + dir.x * d;
            double py = start.y + dir.y * d;
            double pz = start.z + dir.z * d;

            serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 2, 0.03, 0.03, 0.03, 0.01);
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
                serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        long distance = Math.round(Math.sqrt(delta.x * delta.x + delta.z * delta.z));
        serverPlayer.displayClientMessage(Component.literal("The Eye of the Crucible points toward the Infernal Crucible (" + distance + " blocks away)."), true);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.success(stack);
    }
}
