package com.velorise.emc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
    public DeepboundEnderChestBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DeepboundEnderChestBlockEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,@Nullable LivingEntity placer,ItemStack stack){super.setPlacedBy(l,p,s,placer,stack);if(l.getBlockEntity(p) instanceof DeepboundEnderChestBlockEntity be)be.setCoreTier(CoreTierData.get(stack));}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){if(l.getBlockState(p.above()).blocksMotion())return InteractionResult.sidedSuccess(l.isClientSide);if(!l.isClientSide&&player instanceof ServerPlayer sp&&l.getBlockEntity(p) instanceof DeepboundEnderChestBlockEntity be){PersonalDeepboundEnderStorage storage=new PersonalDeepboundEnderStorage(sp,be.getCoreTier(),-1);sp.openMenu(new SimpleMenuProvider((id,inv,ignored)->new DeepboundStorageMenu(id,inv,storage),net.minecraft.network.chat.Component.translatable("container.velorise_emc.deepbound_ender_chest")));}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder b){ItemStack out=new ItemStack(VeloriseEMC.DEEPBOUND_ENDER_CHEST_ITEM.get());BlockEntity be=b.getOptionalParameter(LootContextParams.BLOCK_ENTITY);if(be instanceof DeepboundEnderChestBlockEntity e)CoreTierData.set(out,e.getCoreTier());return List.of(out);}
}
