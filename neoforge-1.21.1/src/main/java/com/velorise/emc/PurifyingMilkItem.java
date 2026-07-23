package com.velorise.emc;

import com.velorise.emc.common.EffectConsumableProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Reusable Core-bound purifying milk that removes eligible harmful effects without deleting other effects. */
public final class PurifyingMilkItem extends MilkBucketItem implements CoreIIntegratedItem {
    public PurifyingMilkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int tier = Math.max(1, CoreTierData.installedLevel(stack));
            long cost = EffectConsumableProfile.purificationCost(tier);
            if (!EmcTransactionService.canSpend(serverPlayer, cost)) {
                EmcFailureMessages.show(serverPlayer, cost);
                return InteractionResultHolder.fail(stack);
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int tier = Math.max(1, CoreTierData.installedLevel(stack));
        long cost = EffectConsumableProfile.purificationCost(tier);

        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (!EmcTransactionService.spend(player, cost)) {
                EmcFailureMessages.show(player, cost);
                return stack;
            }

            List<MobEffectInstance> eligible = eligibleEffects(player, tier);
            int removed = 0;
            for (MobEffectInstance effect : eligible) {
                if (player.removeEffect(effect.getEffect())) removed++;
            }

            if (removed > 0) {
                player.displayClientMessage(Component.literal("§aRemoved " + removed + " harmful effect"
                    + (removed == 1 ? "" : "s") + " §7(-" + cost + " EMC)"), true);
            } else if (tier >= 4) {
                player.displayClientMessage(Component.literal("§6You are now exactly as healthy as before. §7(-" + cost + " EMC)"), true);
                SimpleAdvancements.award(player, "world/easter/absolutely_purified");
            } else {
                player.displayClientMessage(Component.literal("§7No eligible harmful effects were found. §8(-" + cost + " EMC)"), true);
            }
        }

        return stack;
    }

    private static List<MobEffectInstance> eligibleEffects(LivingEntity entity, int tier) {
        List<MobEffectInstance> result = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (isEligible(effect, tier)) result.add(new MobEffectInstance(effect));
        }
        return result;
    }

    private static boolean isEligible(MobEffectInstance effect, int tier) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
            && EffectConsumableProfile.acceptsEffectLevel(tier, effect.getAmplifier());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

        
}
