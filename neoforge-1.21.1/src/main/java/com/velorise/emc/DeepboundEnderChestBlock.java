package com.velorise.emc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DeepboundEnderChestBlock extends BaseEntityBlock {
    public static final MapCodec<DeepboundEnderChestBlock> CODEC = simpleCodec(DeepboundEnderChestBlock::new);

    public DeepboundEnderChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DeepboundEnderChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeepboundEnderChestBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof DeepboundEnderChestBlockEntity chest) {
            chest.setCoreTier(CoreTierData.get(stack));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.getBlockState(pos.above()).blocksMotion()) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof DeepboundEnderChestBlockEntity chest) {
            PersonalDeepboundEnderStorage storage = new PersonalDeepboundEnderStorage(
                serverPlayer, chest.getCoreTier(), -1);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new DeepboundStorageMenu(id, inventory, storage),
                net.minecraft.network.chat.Component.translatable(
                    "container.velorise_emc.deepbound_ender_chest")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack output = new ItemStack(VeloriseEMC.DEEPBOUND_ENDER_CHEST_ITEM.get());
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof DeepboundEnderChestBlockEntity chest) {
            CoreTierData.set(output, chest.getCoreTier());
        }
        return List.of(output);
    }
}
