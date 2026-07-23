package com.velorise.emc;

import com.velorise.emc.common.EmcMath;
import com.velorise.emc.common.HourglassProfile;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Core-I-through-IV event-driven Alchemical Hourglass. */
public class AlchemicalHourglassBlockEntity extends BlockEntity implements MenuProvider, ResonantMachine {
    private long emc;
    private int installedCoreLevel = HourglassProfile.MIN_CORE_LEVEL;
    private boolean machineEnabled = true;
    private int lastSuccessfulTicks;
    private long lastPulseCost;
    private int lastTargetCount;
    private long activeUntil;

    public AlchemicalHourglassBlockEntity(BlockPos pos, BlockState state) {
        super(VeloriseEMC.ALCHEMICAL_HOURGLASS_BE.get(), pos, state);
    }

    @Override public Component getDisplayName() { return Component.translatable("container.velorise_emc.alchemical_hourglass"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlchemicalHourglassMenu(id, inventory, this);
    }

    public int getInstalledCoreLevel() { return HourglassProfile.clampCoreLevel(installedCoreLevel); }
    public void setInstalledCoreLevel(int level) {
        int next = HourglassProfile.clampCoreLevel(level);
        if (installedCoreLevel == next) return;
        installedCoreLevel = next;
        lastTargetCount = 0;
        requestRefresh();
        setChanged();
    }
    public void setCoreTier(@Nullable WirelessCoreTier tier) { setInstalledCoreLevel(tier == null ? HourglassProfile.MIN_CORE_LEVEL : tier.level()); }

    public long getEMC() { return emc; }
    public void setEMC(long value) { emc = Math.max(0L, Math.min(HourglassProfile.maxEmc(), value)); setChanged(); syncVisual(); }
    @Override public long getStoredEmc() { return emc; }
    @Override public long getMaxStoredEmc() { return HourglassProfile.maxEmc(); }
    public long addEMC(long amount) { return addEmc(amount); }
    @Override public long addEmc(long amount) {
        long previous = emc;
        emc = EmcMath.saturatingAdd(emc, Math.max(0L, amount), HourglassProfile.maxEmc());
        if (emc != previous) { setChanged(); syncVisual(); }
        return emc - previous;
    }

    @Override public boolean isMachineEnabled() { return machineEnabled; }
    @Override public void setMachineEnabled(boolean enabled) {
        if (machineEnabled == enabled) return;
        machineEnabled = enabled;
        if (!enabled) activeUntil = 0L; else requestRefresh();
        setChanged();
        syncVisual();
    }
    public boolean canAccelerate() {
        return !isRemoved() && machineEnabled && emc >= HourglassProfile.emcPerSuccessfulTick();
    }
    public void finishPulse(int successfulTicks, long cost, int targetCount) {
        lastSuccessfulTicks = Math.max(0, successfulTicks);
        lastPulseCost = Math.max(0L, Math.min(cost, emc));
        lastTargetCount = Math.max(0, targetCount);
        if (lastPulseCost > 0L) {
            emc -= lastPulseCost;
            if (level != null) {
                Player nearest = level.getNearestPlayer(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D, 16.0D, false);
                if (nearest instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    AdvancementHooks.onHourglassPulse(serverPlayer, lastPulseCost);
                }
            }
        }
        if (lastSuccessfulTicks > 0 && level != null) activeUntil = level.getGameTime() + HourglassProfile.pulseIntervalTicks();
        setChanged();
        syncVisual();
    }
    public int getLastSuccessfulTicks() { return lastSuccessfulTicks; }
    public long getLastPulseCost() { return lastPulseCost; }
    public int getLastTargetCount() { return lastTargetCount; }
    public boolean isActive() { return level != null && level.getGameTime() < activeUntil; }
    public void requestRefresh() {
        if (level instanceof ServerLevel serverLevel) HourglassAccelerationManager.requestRefresh(serverLevel, worldPosition);
    }

    public ItemStack createDropStack() {
        ItemStack result = new ItemStack(VeloriseEMC.ALCHEMICAL_HOURGLASS_ITEM.get());
        CoreTierData.set(result, WirelessCoreTier.byLevel(getInstalledCoreLevel()));
        CompoundTag data = new CompoundTag();
        writeData(data);
        BlockItem.setBlockEntityData(result, VeloriseEMC.ALCHEMICAL_HOURGLASS_BE.get(), data);
        return result;
    }

    private void readData(CompoundTag tag) {
        long saved = tag.contains("StoredEMC") ? tag.getLong("StoredEMC") : tag.getLong("emc");
        emc = Math.max(0L, Math.min(HourglassProfile.maxEmc(), saved));
        installedCoreLevel = HourglassProfile.clampCoreLevel(tag.contains("InstalledCoreLevel")
            ? tag.getInt("InstalledCoreLevel") : (tag.contains("CoreTier") ? tag.getInt("CoreTier") : HourglassProfile.MIN_CORE_LEVEL));
        machineEnabled = !tag.contains("MachineEnabled") || tag.getBoolean("MachineEnabled");
        lastSuccessfulTicks = 0;
        lastPulseCost = 0L;
        lastTargetCount = 0;
        activeUntil = 0L;
    }
    private void writeData(CompoundTag tag) {
        tag.putLong("StoredEMC", emc);
        tag.putInt("InstalledCoreLevel", getInstalledCoreLevel());
        tag.putBoolean("MachineEnabled", machineEnabled);
    }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = super.getUpdateTag(); writeData(tag); return tag; }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    private void syncVisual() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public void load(CompoundTag tag) { super.load(tag); readData(tag); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); writeData(tag); }
    @Override public void onLoad() { super.onLoad(); requestRefresh(); }
    @Override public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) HourglassAccelerationManager.releaseSource(serverLevel, worldPosition);
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlchemicalHourglassBlockEntity hourglass) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        if (hourglass.emc > HourglassProfile.maxEmc()) {
            hourglass.emc = HourglassProfile.maxEmc();
            hourglass.setChanged();
            hourglass.syncVisual();
        }
        if (HourglassProfile.isPulseDue(level.getGameTime(), pos.asLong())) {
            HourglassAccelerationManager.PulseResult result = hourglass.canAccelerate()
                ? HourglassAccelerationManager.processPulse(serverLevel, hourglass)
                : new HourglassAccelerationManager.PulseResult(0, 0L,
                    HourglassAccelerationManager.getOwnedTargetCount(serverLevel, pos));
            if (!hourglass.canAccelerate()) hourglass.finishPulse(0, 0L, result.targetCount());
        }
        boolean active = hourglass.isActive();
        if (active && level.getGameTime() % 3L == 0L) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_OBSIDIAN_TEAR,
                pos.getX() + 0.5D, pos.getY() + 0.72D, pos.getZ() + 0.5D,
                1, 0.008D, 0.0D, 0.008D, 0.0D);
        }
        if (state.hasProperty(AlchemicalHourglassBlock.ACTIVE) && state.getValue(AlchemicalHourglassBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(AlchemicalHourglassBlock.ACTIVE, active), 3);
        }
    }
}
