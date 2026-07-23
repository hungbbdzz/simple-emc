package com.velorise.emc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Post-boss pickaxe with a client-rendered right-click slam pose. */
public final class EchoboundPickaxeItem extends PickaxeItem {
    public static final int SLAM_DURATION_TICKS = 24;
    public static final int SLAM_COOLDOWN_TICKS = 60;

    public EchoboundPickaxeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    /**
     * Post-boss privilege: this specific pickaxe, not an installed Core, mines every
     * breakable block at near-instant speed. Unbreakable blocks remain protected by
     * their negative destroy-time rule in the level.
     */
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return 4096.0F; }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return state.getDestroySpeed(null, null) >= 0.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.getDestroySpeed(null, null) >= 0.0F;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public boolean canBeDepleted() { return false; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack) { return SLAM_DURATION_TICKS; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (level.isClientSide && entity instanceof Player player) {
            EchoboundClientEffects.startShake(EchoboundPickaxeEffects.impactPoint(player), 14, 0.85F);
        } else if (entity instanceof ServerPlayer player) {
            // Do not trigger the vanilla swing here. The charge pose already provides
            // the custom animation, and an extra swing causes a jarring melee animation
            // after the slam charge finishes.
            EchoboundPickaxeEffects.slam(player);
            player.getCooldowns().addCooldown(this, SLAM_COOLDOWN_TICKS);
        }
        return stack;
    }
}
