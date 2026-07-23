package com.velorise.emc;

import com.velorise.emc.common.UtilityItemProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** EMC-powered soul-fire igniter. Block ignition is unlimited; entity ignition has cooldown. */
public final class CursedIgniterItem extends Item {
    public CursedIgniterItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(target).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;

        int core = CoreTierData.get(stack).level();
        long cost = UtilityItemProfile.cursedIgniterCost(core);
        if (!EmcTransactionService.spend(player, cost)) {
            EmcFailureMessages.show(player, cost);
            return InteractionResult.FAIL;
        }
        // Soul fire is intentionally allowed on any supporting block. No durability and no block-use cooldown.
        level.setBlock(target, VeloriseEMC.CURSED_SOUL_FIRE.get().defaultBlockState(), 11);
        ((ServerLevel) level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            target.getX() + 0.5D, target.getY() + 0.35D, target.getZ() + 0.5D,
            12, 0.25D, 0.18D, 0.25D, 0.01D);
        level.playSound(null, target, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9F, 0.8F);
        return InteractionResult.CONSUME;
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
}
