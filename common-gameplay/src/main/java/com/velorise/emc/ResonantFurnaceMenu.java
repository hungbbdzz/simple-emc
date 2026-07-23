package com.velorise.emc;

import com.velorise.emc.common.ResonantMachineProfile;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ResonantFurnaceMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 7;
    private final Container machine;
    private final AbstractResonantFurnaceBlockEntity furnace;
    private final ContainerData data;

    public ResonantFurnaceMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(3), null, new SimpleContainerData(DATA_COUNT)); }
    public ResonantFurnaceMenu(int id, Inventory inventory, AbstractResonantFurnaceBlockEntity furnace) { this(id, inventory, furnace, furnace, null); }

    private ResonantFurnaceMenu(int id, Inventory inventory, Container machine,
                                AbstractResonantFurnaceBlockEntity furnace, ContainerData clientData) {
        super(VeloriseEMC.RESONANT_FURNACE_MENU_TYPE.get(), id);
        this.machine = machine;
        this.furnace = furnace;
        this.data = clientData != null ? clientData : createServerData();
        checkContainerSize(machine, 3);
        checkContainerDataCount(data, DATA_COUNT);
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, 0, 56, 17));
        addSlot(new FuelSlot(machine, 1, 56, 53));
        addSlot(new FurnaceResultSlot(inventory.player, machine, 2, 116, 35) {
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (player instanceof ServerPlayer serverPlayer && ResonantFurnaceMenu.this.furnace != null) {
                    AdvancementHooks.onResonantFurnaceTake(serverPlayer, ResonantFurnaceMenu.this.furnace, stack);
                }
            }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col, 8 + col * 18, 142);
        addDataSlots(data);
    }

    private void addPlayerSlot(Inventory inventory, int slot, int x, int y) {
        int locked = furnace instanceof PortableMenuBacking portable ? portable.lockedPlayerSlot() : -1;
        addSlot(slot == locked ? new LockedSlot(inventory, slot, x, y) : new Slot(inventory, slot, x, y));
    }

    private ContainerData createServerData() {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> furnace.getLitTime();
                    case 1 -> furnace.getLitDuration();
                    case 2 -> furnace.getCookingProgress();
                    case 3 -> furnace.getCookingTotalTime();
                    case 4 -> furnace.getCoreTierLevel();
                    case 5 -> furnace.isMachineEnabled() ? 1 : 0;
                    case 6 -> furnace.getKind().ordinal();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (furnace == null) return false;
        if (id == 0) furnace.setMachineEnabled(!furnace.isMachineEnabled());
        else return false;
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;
        ItemStack stack = slot.getItem(); result = stack.copy();
        if (index < 3) { if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY; }
        else if (machine.canPlaceItem(1, stack)) { if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) { return furnace == null || furnace instanceof PortableMenuBacking || furnace.stillValid(player); }

    @Override public void removed(Player player) {
        super.removed(player); machine.stopOpen(player);
        if (furnace instanceof PortableMenuBacking portable) portable.savePortableBacking();
    }

    public long getStoredEmc() { return 0L; }
    public int getLitTime() { return data.get(0); }
    public int getLitDuration() { return data.get(1); }
    public int getCookingProgress() { return data.get(2); }
    public int getCookingTotalTime() { return data.get(3); }
    public int getCoreTier() { return Math.max(1, Math.min(4, data.get(4))); }
    public boolean isEnabled() { return data.get(5) != 0; }
    public AbstractResonantFurnaceBlockEntity.Kind getKind() {
        int index = Math.max(0, Math.min(AbstractResonantFurnaceBlockEntity.Kind.values().length - 1, data.get(6)));
        return AbstractResonantFurnaceBlockEntity.Kind.values()[index];
    }
    public boolean isLit() { return getLitTime() > 0; }
    public int getOperationCost() { return 0; }
    public int getSpeedMultiplier() { return ResonantMachineProfile.furnaceSpeedMultiplier(getCoreTier()); }

    /** Legacy 1.20 furnace-arrow width. */
    public int getBurnProgress() {
        return getProgressPixels(24);
    }

    /** Legacy 1.20 furnace-flame height. */
    public int getLitProgress() {
        return getLitProgress(12);
    }

    /** Returns cooking progress scaled to the requested sprite width. */
    public int getProgressPixels(int width) {
        int safeWidth = Math.max(0, width);
        int progress = getCookingProgress();
        int total = getCookingTotalTime();
        if (safeWidth == 0 || progress <= 0 || total <= 0) return 0;
        return Math.min(safeWidth, Math.max(0, progress * safeWidth / total));
    }

    /** Returns remaining fuel time scaled to the requested sprite height. */
    public int getLitProgress(int height) {
        int safeHeight = Math.max(0, height);
        int duration = getLitDuration();
        int time = getLitTime();
        if (safeHeight == 0 || duration <= 0 || time <= 0) return 0;
        return Math.min(safeHeight, Math.max(1, time * safeHeight / duration));
    }

    private static class FuelSlot extends Slot {
        private final Container fuelContainer;
        private final int fuelSlot;
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.fuelContainer = container;
            this.fuelSlot = slot;
        }
        @Override public boolean mayPlace(ItemStack stack) { return fuelContainer.canPlaceItem(fuelSlot, stack); }
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
