package com.velorise.emc;

import com.mojang.serialization.MapCodec;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PurificationGrindstoneBlock extends GrindstoneBlock {
    public static final MapCodec<PurificationGrindstoneBlock> CODEC = simpleCodec(PurificationGrindstoneBlock::new);
    public static final IntegerProperty CORE_TIER = IntegerProperty.create("core_tier", 1, 4);
    private static final Component TITLE = Component.translatable("container.velorise_emc.purification_grindstone");

    public PurificationGrindstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CORE_TIER, 1));
    }

    @Override @SuppressWarnings("unchecked") public MapCodec<GrindstoneBlock> codec() { return (MapCodec) CODEC; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos, state.setValue(CORE_TIER, CoreTierData.get(stack).level()), 3);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        int tier = state.getValue(CORE_TIER);
        return new SimpleMenuProvider(
            (id, inventory, player) -> new PurificationGrindstoneMenu(
                id, inventory, ContainerLevelAccess.create(level, pos), false, tier),
            TITLE
        );
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack result = new ItemStack(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get());
        CoreTierData.set(result, WirelessCoreTier.byLevel(state.getValue(CORE_TIER)));
        return List.of(result);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CORE_TIER);
    }
}
