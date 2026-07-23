package com.velorise.emc;

import com.velorise.emc.common.DeepboundStorageProfile;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Persistent per-player Deepbound Ender inventory. All upgraded Ender Chests open this same store. */
public final class PersonalDeepboundEnderStorage implements DeepboundStorageAccess {
    private static final String ROOT="VeloriseEMCDeepboundEnderStorage";
    private final Player player; private final WirelessCoreTier coreTier; private final int lockedPlayerSlot;
    private NonNullList<ItemStack> items=NonNullList.withSize(DeepboundStorageProfile.MAX_SLOTS,ItemStack.EMPTY);
    public PersonalDeepboundEnderStorage(Player player,WirelessCoreTier tier,int lockedPlayerSlot){this.player=player;this.coreTier=tier==null?WirelessCoreTier.I:tier;this.lockedPlayerSlot=lockedPlayerSlot;CompoundTag tag=player.getPersistentData().getCompound(ROOT);ContainerHelper.loadAllItems(tag,items);}
    public static void copyPlayerData(Player from,Player to){if(from==null||to==null)return;CompoundTag source=from.getPersistentData().getCompound(ROOT);if(!source.isEmpty())to.getPersistentData().put(ROOT,source.copy());}private void save(){CompoundTag tag=new CompoundTag();ContainerHelper.saveAllItems(tag,items);player.getPersistentData().put(ROOT,tag);}
    @Override public WirelessCoreTier getCoreTier(){return coreTier;}@Override public int getAccessibleSlotCount(){return DeepboundStorageProfile.slotCount(coreTier);}@Override public int clampPage(int page){return DeepboundStorageProfile.clampPage(coreTier,page);}@Override public boolean stillValid(Player p){return p==player&&p.isAlive();}@Override public int lockedPlayerSlot(){return lockedPlayerSlot;}@Override public int getContainerSize(){return DeepboundStorageProfile.MAX_SLOTS;}@Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}@Override public ItemStack getItem(int i){return i>=0&&i<items.size()?items.get(i):ItemStack.EMPTY;}@Override public ItemStack removeItem(int i,int n){ItemStack out=ContainerHelper.removeItem(items,i,n);if(!out.isEmpty())save();return out;}@Override public ItemStack removeItemNoUpdate(int i){ItemStack out=ContainerHelper.takeItem(items,i);save();return out;}@Override public void setItem(int i,ItemStack s){if(i<0||i>=items.size())return;items.set(i,s);if(!s.isEmpty()&&s.getCount()>getMaxStackSize())s.setCount(getMaxStackSize());save();}@Override public boolean canPlaceItem(int i,ItemStack s){return DeepboundStorageProfile.isAccessibleSlot(coreTier,i)&&!s.is(VeloriseEMC.DEEPBOUND_ENDER_CHEST_ITEM.get());}@Override public void setChanged(){save();}@Override public void clearContent(){items.clear();save();}
}
