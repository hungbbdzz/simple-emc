package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Arcane menu variant sharing the normal table facing and stored-Core block state. */
public class ArcaneTransmutationTableBlock extends TransmutationTableBlock {
    private static final Component CONTAINER_TITLE = Component.translatable("container.velorise_emc.arcane_transmutation_table");

    public ArcaneTransmutationTableBlock(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) ->
            new ArcaneTransmutationMenu(id, inventory, ContainerLevelAccess.create(level, pos)), CONTAINER_TITLE);
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        net.minecraft.world.item.ItemStack result = new net.minecraft.world.item.ItemStack(VeloriseEMC.ARCANE_TRANSMUTATION_TABLE_ITEM.get());
        int core = state.getValue(CORE_LEVEL);
        if (core > 0) CoreTierData.set(result, com.velorise.emc.common.WirelessCoreTier.byLevel(core));
        return java.util.List.of(result);
    }
}
