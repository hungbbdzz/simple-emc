package com.velorise.emc;

import com.velorise.emc.common.LastEchoProfile;
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

/** Reusable Tier-IV compass for the single Citadel of the Last Echo. */
public final class EyeOfLastEchoItem extends Item {
    public EyeOfLastEchoItem(Properties properties) { super(properties.stacksTo(1).fireResistant()); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            if (!player.getAbilities().instabuild
                && WorldProgressionData.get(player.server).snapshot().worldTier() < LastEchoProfile.REQUIRED_WORLD_TIER) {
                player.displayClientMessage(Component.translatable("message.velorise_emc.last_echo.locked"), true);
                return InteractionResult.FAIL;
            }
            if (LastEchoCitadelService.useRespawnFountain((ServerLevel)level,
                context.getClickedPos(), player, context.getItemInHand())) {
                return InteractionResult.CONSUME;
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer serverPlayer = (ServerPlayer)player;
        if (!serverPlayer.getAbilities().instabuild
            && WorldProgressionData.get(serverPlayer.server).snapshot().worldTier() < LastEchoProfile.REQUIRED_WORLD_TIER) {
            serverPlayer.displayClientMessage(Component.translatable("message.velorise_emc.last_echo.locked"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!Level.END.equals(level.dimension())) {
            serverPlayer.displayClientMessage(Component.translatable("message.velorise_emc.last_echo.end_only"), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos target = LastEchoCitadelService.ensureLocated(serverPlayer.server);
        if (target == null) return InteractionResultHolder.fail(stack);
        Vec3 delta = Vec3.atCenterOf(target).subtract(player.position());
        Vec3 direction = new Vec3(delta.x, 0.0D, delta.z).normalize();
        ServerLevel end = (ServerLevel)level;
        for (int i = 2; i <= 18; i += 2) end.sendParticles(ParticleTypes.SCULK_SOUL,
            player.getX() + direction.x * i, player.getY() + 1.2D, player.getZ() + direction.z * i,
            1, 0.05D, 0.05D, 0.05D, 0.0D);
        long distance = Math.round(Math.sqrt(delta.x * delta.x + delta.z * delta.z));
        serverPlayer.displayClientMessage(Component.translatable("message.velorise_emc.last_echo.direction", distance), true);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.65F);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.success(stack);
    }
}
