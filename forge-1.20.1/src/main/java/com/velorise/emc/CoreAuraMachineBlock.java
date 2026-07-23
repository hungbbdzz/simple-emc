package com.velorise.emc;
import com.velorise.emc.common.CoreAuraMachineProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import java.util.List;
/** Core-powered Tidal Conduit block. */
public class CoreAuraMachineBlock extends BaseEntityBlock {
    private final CoreAuraMachineProfile.Kind kind;
    public CoreAuraMachineBlock(Properties p, CoreAuraMachineProfile.Kind k) {
        super(p);
        kind = k;
    }public CoreAuraMachineProfile.Kind kind(){return kind;}
    public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CoreAuraMachineBlockEntity(p,s);}@Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return null;}public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    public void setPlacedBy(Level l,BlockPos p,BlockState s,@Nullable LivingEntity e,ItemStack st){super.setPlacedBy(l,p,s,e,st);if(l.getBlockEntity(p) instanceof CoreAuraMachineBlockEntity m)m.setCoreTier(CoreTierData.get(st));}
    public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){BlockEntity be=l.getBlockEntity(p);if(!(be instanceof MenuProvider provider))return InteractionResult.PASS;if(!l.isClientSide&&player instanceof ServerPlayer sp)sp.openMenu(provider);return InteractionResult.sidedSuccess(l.isClientSide);}
    public List<ItemStack> getDrops(BlockState s,LootParams.Builder b){BlockEntity be=b.getOptionalParameter(LootContextParams.BLOCK_ENTITY);if(be instanceof CoreAuraMachineBlockEntity m)return List.of(m.createDropStack());return List.of();}
    public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moved){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof CoreAuraMachineBlockEntity m)Containers.dropContents(l,p,m);super.onRemove(s,l,p,n,moved);}
}
