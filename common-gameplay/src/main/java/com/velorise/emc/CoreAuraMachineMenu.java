package com.velorise.emc;

import com.velorise.emc.common.CoreAuraMachineProfile;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CoreAuraMachineMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 9;
    private final CoreAuraMachineAccess machine;
    private final ContainerData data;

    public CoreAuraMachineMenu(int id, Inventory inventory) {
        this(id, inventory, new ClientMachine(), new SimpleContainerData(DATA_COUNT));
    }
    public CoreAuraMachineMenu(int id, Inventory inventory, CoreAuraMachineAccess machine) {
        this(id, inventory, machine, null);
    }
    private CoreAuraMachineMenu(int id, Inventory inventory, CoreAuraMachineAccess machine, ContainerData clientData) {
        super(VeloriseEMC.CORE_AURA_MACHINE_MENU_TYPE.get(), id);
        this.machine = machine;
        this.data = clientData != null ? clientData : createServerData();
        checkContainerSize(machine, 1);
        checkContainerDataCount(data, DATA_COUNT);
        machine.onMenuOpened(inventory.player);
        addSlot(new FuelSlot(machine, 0, 8, 57));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int inventorySlot = col + row * 9 + 9;
            addSlot(inventorySlot == machine.lockedPlayerSlot()
                ? new LockedSlot(inventory, inventorySlot, 8 + col * 18, 108 + row * 18)
                : new Slot(inventory, inventorySlot, 8 + col * 18, 108 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(col == machine.lockedPlayerSlot()
            ? new LockedSlot(inventory, col, 8 + col * 18, 166)
            : new Slot(inventory, col, 8 + col * 18, 166));
        addDataSlots(data);
    }

    private ContainerData createServerData() {
        return new ContainerData() {
            public int get(int index) {
                long emc = machine.getStoredEmc();
                return switch (index) {
                    case 0 -> (int) (emc & 0xFFFFL);
                    case 1 -> (int) ((emc >>> 16) & 0xFFFFL);
                    case 2 -> (int) ((emc >>> 32) & 0xFFFFL);
                    case 3 -> (int) ((emc >>> 48) & 0xFFFFL);
                    case 4 -> machine.getCoreTierLevel();
                    case 5 -> machine.getSelectedEffect();
                    case 6 -> machine.isMachineEnabled() ? 1 : 0;
                    case 7 -> machine.isRunning() ? 1 : 0;
                    case 8 -> machine.getMachineKind().ordinal();
                    default -> 0;
                };
            }
            public void set(int index, int value) {}
            public int getCount() { return DATA_COUNT; }
        };
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0) return false;
        machine.setMachineEnabled(!machine.isMachineEnabled());
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index < 1) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (EMCRegistry.getAlchemicalFuelEMC(stack) > 0L) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.onMenuClosed(player); }

    public long getStoredEmc() {
        return ((long) data.get(0) & 0xFFFFL) | (((long) data.get(1) & 0xFFFFL) << 16)
            | (((long) data.get(2) & 0xFFFFL) << 32) | (((long) data.get(3) & 0xFFFFL) << 48);
    }
    public int getCoreTier() { return CoreAuraMachineProfile.clampTier(data.get(4)); }
    public boolean isEnabled() { return data.get(6) != 0; }
    public boolean isRunning() { return data.get(7) != 0; }
    public CoreAuraMachineProfile.Kind getKind() { return CoreAuraMachineProfile.Kind.TIDAL_CONDUIT; }
    public int getRange() { return CoreAuraMachineProfile.range(getKind(), getCoreTier()); }
    public long getCostPerSecond() { return CoreAuraMachineProfile.emcPerSecond(getKind()); }
    public long getCostPerPulse() { return CoreAuraMachineProfile.emcPerPulse(getKind()); }

    private static final class LockedSlot extends Slot {
        LockedSlot(net.minecraft.world.Container c, int i, int x, int y) { super(c, i, x, y); }
        @Override public boolean mayPickup(Player p) { return false; }
        @Override public boolean mayPlace(ItemStack s) { return false; }
    }
    private static final class FuelSlot extends Slot {
        FuelSlot(net.minecraft.world.Container c, int i, int x, int y) { super(c, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return EMCRegistry.getAlchemicalFuelEMC(s) > 0L; }
    }
    private static final class ClientMachine extends SimpleContainer implements CoreAuraMachineAccess {
        ClientMachine() { super(1); }
        public CoreAuraMachineProfile.Kind getMachineKind() { return CoreAuraMachineProfile.Kind.TIDAL_CONDUIT; }
        public int getCoreTierLevel() { return 1; }
        public int getSelectedEffect() { return 0; }
        public void setSelectedEffect(int selection) {}
        public boolean isRunning() { return false; }
        public boolean stillValid(Player p) { return true; }
        public long getStoredEmc() { return 0; }
        public long getMaxStoredEmc() { return CoreAuraMachineProfile.maxEmc(CoreAuraMachineProfile.Kind.TIDAL_CONDUIT); }
        public long addEmc(long a) { return 0; }
        public boolean isMachineEnabled() { return true; }
        public void setMachineEnabled(boolean e) {}
    }
}
