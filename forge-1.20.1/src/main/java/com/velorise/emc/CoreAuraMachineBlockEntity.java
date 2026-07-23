package com.velorise.emc;

import com.velorise.emc.common.CoreAuraMachineProfile;
import com.velorise.emc.common.EmcMath;
import com.velorise.emc.common.WirelessCoreTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Pulse-based EMC aura machine used by the Tidal Conduit. */
public class CoreAuraMachineBlockEntity extends RandomizableContainerBlockEntity implements CoreAuraMachineAccess {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private long storedEmc;
    private int coreTier = 1;
    private int selectedEffect;
    private boolean enabled = true;
    private boolean running;
    private long runningUntil;

    public CoreAuraMachineBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }

    @Override public CoreAuraMachineProfile.Kind getMachineKind() {
        return getBlockState().getBlock() instanceof CoreAuraMachineBlock block
            ? block.kind() : CoreAuraMachineProfile.Kind.TIDAL_CONDUIT;
    }
    @Override public int getCoreTierLevel() { return CoreAuraMachineProfile.clampTier(coreTier); }
    public void setCoreTier(WirelessCoreTier tier) {
        coreTier = tier == null ? 1 : tier.level();
        selectedEffect = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoreAuraMachineBlockEntity machine) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        long max = CoreAuraMachineProfile.maxEmc(machine.getMachineKind());
        if (machine.storedEmc > max) { machine.storedEmc = max; machine.setChanged(); }
        long gameTime = level.getGameTime();
        if (CoreAuraMachineProfile.isFuelTickDue(machine.getMachineKind(), gameTime, pos.asLong())) machine.charge();
        if (machine.running && gameTime >= machine.runningUntil) machine.setRunning(false);
        if (!CoreAuraMachineProfile.isPulseDue(machine.getMachineKind(), gameTime, pos.asLong())) return;
        machine.processPulse(serverLevel, pos);
    }

    private void processPulse(ServerLevel level, BlockPos pos) {
        if (!enabled) { setRunning(false); return; }
        long cost = CoreAuraMachineProfile.emcPerPulse(CoreAuraMachineProfile.Kind.TIDAL_CONDUIT);
        if (storedEmc < cost) { setRunning(false); return; }
        int radius = CoreAuraMachineProfile.range(CoreAuraMachineProfile.Kind.TIDAL_CONDUIT, coreTier);
        int applied = applyConduit(level, new AABB(pos).inflate(radius));
        if (applied <= 0) { setRunning(false); return; }
        storedEmc -= cost;
        runningUntil = level.getGameTime() + CoreAuraMachineProfile.pulseIntervalTicks(CoreAuraMachineProfile.Kind.TIDAL_CONDUIT);
        setRunning(true);
        setChanged();
    }


    private int applyConduit(ServerLevel level, AABB area) {
        int count = 0;
        int duration = CoreAuraMachineProfile.effectDurationTicks(getMachineKind());
        for (Player player : level.getEntitiesOfClass(Player.class, area,
            p -> !p.isSpectator() && p.isAlive() && p.isInWaterOrRain())) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, duration, 0, true, true));
            count++;
        }
        return count;
    }

    private void charge() {
        ItemStack fuel = items.get(0);
        long each = EMCRegistry.getAlchemicalFuelEMC(fuel);
        long max = CoreAuraMachineProfile.maxEmc(getMachineKind());
        if (each <= 0L || storedEmc >= max) return;
        long fits = Math.max(0L, max - storedEmc) / each;
        if (fits <= 0L) return;
        int consume = (int) Math.min(fuel.getCount(), fits);
        storedEmc += each * consume;
        fuel.shrink(consume);
        if (fuel.isEmpty()) items.set(0, ItemStack.EMPTY);
        setChanged();
    }

    private void setRunning(boolean value) { if (running != value) { running = value; setChanged(); } }
    @Override public boolean isRunning() { return running; }

    public ItemStack createDropStack() {
        ItemStack output = ItemStack.EMPTY;
        CoreTierData.set(output, WirelessCoreTier.byLevel(coreTier));
        if (hasCustomName()) output.setHoverName(getCustomName());
        return output;
    }

    private void writeData(CompoundTag tag) {
        tag.putLong("StoredEMC", storedEmc);
        tag.putInt("CoreTier", coreTier);
        tag.putInt("SelectedEffect", selectedEffect);
        tag.putBoolean("MachineEnabled", enabled);
    }
    private void readData(CompoundTag tag) {
        storedEmc = Math.max(0L, Math.min(CoreAuraMachineProfile.maxEmc(getMachineKind()), tag.getLong("StoredEMC")));
        coreTier = CoreAuraMachineProfile.clampTier(tag.contains("CoreTier") ? tag.getInt("CoreTier") : 1);
        selectedEffect = 0;
        enabled = !tag.contains("MachineEnabled") || tag.getBoolean("MachineEnabled");
        running = false;
        runningUntil = 0L;
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items);
        readData(tag);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items);
        writeData(tag);
    }
    @Override protected Component getDefaultName() {
        return Component.translatable("container.velorise_emc.tidal_conduit");
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new CoreAuraMachineMenu(id, inventory, this); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && EMCRegistry.getAlchemicalFuelEMC(stack) > 0L; }
    @Override public boolean stillValid(Player player) {
        return level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this
            && player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) <= 64.0D;
    }
    @Override public long getStoredEmc() { return storedEmc; }
    @Override public long getMaxStoredEmc() { return CoreAuraMachineProfile.maxEmc(getMachineKind()); }
    @Override public long addEmc(long amount) {
        long before = storedEmc;
        storedEmc = EmcMath.saturatingAdd(storedEmc, Math.max(0L, amount), getMaxStoredEmc());
        if (before != storedEmc) setChanged();
        return storedEmc - before;
    }
    @Override public int getSelectedEffect() { return 0; }
    @Override public void setSelectedEffect(int selection) {}
    @Override public boolean isMachineEnabled() { return enabled; }
    @Override public void setMachineEnabled(boolean value) { enabled = value; if (!value) setRunning(false); setChanged(); }
}
