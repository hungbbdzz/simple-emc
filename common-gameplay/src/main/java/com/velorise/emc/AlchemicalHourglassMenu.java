package com.velorise.emc;

import com.velorise.emc.common.HourglassProfile;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AlchemicalHourglassMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 12;
    private static final int FUEL_SLOT = 0;
    private static final int PLAYER_SLOT_START = 1;
    private static final int PLAYER_SLOT_END = 37;

    private final AlchemicalHourglassBlockEntity hourglass;
    private final ContainerData data;
    private final SimpleContainer fuel = new SimpleContainer(1);
    private boolean consumingFuel;

    public AlchemicalHourglassMenu(int id, Inventory inventory) {
        this(id, inventory, null, new SimpleContainerData(DATA_COUNT));
    }

    public AlchemicalHourglassMenu(int id, Inventory inventory, AlchemicalHourglassBlockEntity hourglass) {
        this(id, inventory, hourglass, null);
    }

    private AlchemicalHourglassMenu(int id, Inventory inventory, AlchemicalHourglassBlockEntity hourglass,
                                    ContainerData clientData) {
        super(VeloriseEMC.ALCHEMICAL_HOURGLASS_MENU_TYPE.get(), id);
        this.hourglass = hourglass;
        this.data = clientData != null ? clientData : createServerData();
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);

        this.fuel.addListener(this::slotsChanged);
        addSlot(new Slot(this.fuel, 0, 148, 30) {
            @Override public boolean mayPlace(ItemStack stack) {
                return EMCRegistry.getAlchemicalFuelEMC(stack) > 0L;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    private ContainerData createServerData() {
        return new ContainerData() {
            @Override public int get(int index) {
                long emc = hourglass.getStoredEmc();
                long cost = hourglass.getLastPulseCost();
                return switch (index) {
                    case 0 -> (int) (emc & 0xFFFFL);
                    case 1 -> (int) ((emc >>> 16) & 0xFFFFL);
                    case 2 -> (int) ((emc >>> 32) & 0xFFFFL);
                    case 3 -> (int) ((emc >>> 48) & 0xFFFFL);
                    case 4 -> hourglass.isMachineEnabled() ? 1 : 0;
                    case 5 -> hourglass.getInstalledCoreLevel();
                    case 6 -> HourglassProfile.radius(hourglass.getInstalledCoreLevel());
                    case 7 -> hourglass.getLastTargetCount();
                    case 8 -> hourglass.getLastSuccessfulTicks();
                    case 9 -> (int) (cost & 0xFFFFL);
                    case 10 -> (int) ((cost >>> 16) & 0xFFFFL);
                    case 11 -> hourglass.isActive() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (hourglass == null || consumingFuel || container != fuel) return;
        ItemStack stack = fuel.getItem(0);
        long value = EMCRegistry.getAlchemicalFuelEMC(stack);
        if (stack.isEmpty() || value <= 0L) return;

        consumingFuel = true;
        try {
            int consumed = 0;
            while (consumed < stack.getCount()
                && hourglass.getStoredEmc() <= HourglassProfile.maxEmc() - value) {
                if (hourglass.addEmc(value) != value) break;
                consumed++;
            }
            if (consumed > 0) {
                stack.shrink(consumed);
                if (stack.isEmpty()) fuel.setItem(0, ItemStack.EMPTY);
                fuel.setChanged();
                broadcastChanges();
            }
        } finally {
            consumingFuel = false;
        }
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (hourglass == null) return false;
        if (id == 0) hourglass.setMachineEnabled(!hourglass.isMachineEnabled());
        else return false;
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index == FUEL_SLOT) {
            if (!moveItemStackTo(source, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) return ItemStack.EMPTY;
        } else if (EMCRegistry.getAlchemicalFuelEMC(source) > 0L) {
            if (!moveItemStackTo(source, FUEL_SLOT, FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) {
        if (hourglass == null || hourglass.isRemoved() || hourglass.getLevel() == null) return false;
        if (hourglass.getLevel().getBlockEntity(hourglass.getBlockPos()) != hourglass) return false;
        double x = hourglass.getBlockPos().getX() + 0.5D;
        double y = hourglass.getBlockPos().getY() + 0.5D;
        double z = hourglass.getBlockPos().getZ() + 0.5D;
        return player.distanceToSqr(x, y, z) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, fuel);
    }

    public long getStoredEmc() {
        return ((long) data.get(0) & 0xFFFFL)
            | (((long) data.get(1) & 0xFFFFL) << 16)
            | (((long) data.get(2) & 0xFFFFL) << 32)
            | (((long) data.get(3) & 0xFFFFL) << 48);
    }
    public boolean isEnabled() { return data.get(4) != 0; }
    public int getCoreTierLevel() { return HourglassProfile.clampCoreLevel(data.get(5)); }
    public int getRadius() { return Math.max(1, data.get(6)); }
    public int getTargetCount() { return Math.max(0, data.get(7)); }
    public int getLastSuccessfulTicks() { return Math.max(0, data.get(8)); }
    public long getLastPulseCost() { return ((long) data.get(9) & 0xFFFFL) | (((long) data.get(10) & 0xFFFFL) << 16); }
    public boolean isActive() { return data.get(11) != 0; }
    public long getCostPerSuccessfulTick() { return HourglassProfile.emcPerSuccessfulTick(); }
    public int getBonusTicks() { return HourglassProfile.bonusTicksPerPulse(); }
}
