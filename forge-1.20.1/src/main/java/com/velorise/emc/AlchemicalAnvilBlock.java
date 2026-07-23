package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One fixed registry block per Alchemical Anvil tier. */
public final class AlchemicalAnvilBlock extends AnvilBlock {
    private final int anvilLevel;

    public AlchemicalAnvilBlock(Properties properties, int anvilLevel) {
        super(properties);
        this.anvilLevel = Math.max(1, Math.min(4, anvilLevel));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(getMenuProvider(state, level, pos));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new AlchemicalAnvilMenu(
                id, inventory, ContainerLevelAccess.create(level, pos), anvilLevel),
            Component.translatable("container.velorise_emc.alchemical_anvil.level", AlchemicalAnvilItem.roman(anvilLevel))
        );
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(AlchemicalAnvilItem.createStack(anvilLevel));
    }
}
