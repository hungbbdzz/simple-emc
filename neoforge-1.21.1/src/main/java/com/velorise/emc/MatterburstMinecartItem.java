package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

/** Matterburst Minecart is born with Core I and upgrades sequentially through Core IV. */
public final class MatterburstMinecartItem extends Item implements CoreIIntegratedItem {
    public MatterburstMinecartItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) return InteractionResult.FAIL;

        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel serverLevel) {
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                ? rail.getRailDirection(state, level, pos, null)
                : RailShape.NORTH_SOUTH;
            double yOffset = shape.isAscending() ? 0.5D : 0.0D;
            MinecartTNT cart = new MinecartTNT(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.0625D + yOffset,
                pos.getZ() + 0.5D
            );
            cart.getPersistentData().putInt(
                MatterburstExplosionManager.ENTITY_CORE_TAG,
                Math.max(1, CoreTierData.installedLevel(stack))
            );
            if (ItemStackNameCompat.hasCustomName(stack)) cart.setCustomName(ItemStackNameCompat.getCustomName(stack));
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
