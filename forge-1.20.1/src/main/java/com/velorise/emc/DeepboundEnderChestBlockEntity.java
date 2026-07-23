package com.velorise.emc;

import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DeepboundEnderChestBlockEntity extends BlockEntity {
    private WirelessCoreTier coreTier=WirelessCoreTier.I;
    public DeepboundEnderChestBlockEntity(BlockPos pos,BlockState state){super(VeloriseEMC.DEEPBOUND_ENDER_CHEST_BE.get(),pos,state);}
    public WirelessCoreTier getCoreTier(){return coreTier;}
    public void setCoreTier(WirelessCoreTier tier){coreTier=tier==null?WirelessCoreTier.I:tier;setChanged();}
    @Override public void load(CompoundTag tag){super.load(tag);coreTier=WirelessCoreTier.byLevel(tag.contains("CoreTier")?tag.getInt("CoreTier"):1);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("CoreTier",coreTier.level());}
}
