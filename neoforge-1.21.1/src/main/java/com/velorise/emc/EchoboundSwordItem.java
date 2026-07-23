package com.velorise.emc;

import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Post-boss sword: twenty heavy echo hits and a charged Sonic Boom. */
public final class EchoboundSwordItem extends SwordItem {
    private static final String CONE_MODE_TAG = "velorise_emc.echobound_sword_cone";
    public EchoboundSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        EchoboundWeaponEffects.applyMeleeEchoes(attacker, target);
    }

    public static boolean toggleMode(Player player) {
        boolean cone = !player.getTags().contains(CONE_MODE_TAG);
        if (cone) player.addTag(CONE_MODE_TAG); else player.removeTag(CONE_MODE_TAG);
        player.displayClientMessage(Component.literal(cone
            ? "Echobound Sword: 180° Echo Wave"
            : "Echobound Sword: Sonic Boom"), true);
        return cone;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                toggleMode(player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return LastEchoProfile.ECHOBOUND_SONIC_CHARGE_TICKS;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && remaining % 4 == 0) {
            if (entity instanceof Player player && player.getTags().contains(CONE_MODE_TAG)) {
                EchoboundWeaponEffects.renderWaveCharge(serverLevel, entity);
            } else {
                EchoboundWeaponEffects.renderCharge(serverLevel, entity);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player
            && (player.getTags().contains(CONE_MODE_TAG)
                ? EchoboundWeaponEffects.fireForwardCone(player)
                : EchoboundWeaponEffects.fireSonicBoom(player))) {
            player.getCooldowns().addCooldown(this, LastEchoProfile.ECHOBOUND_SONIC_COOLDOWN_TICKS);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return stack;
    }
}
