package com.velorise.emc;

import com.velorise.emc.common.ResonanceJukeboxProfile;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative playlist and fuel UI for the Resonance Jukebox. */
public class ResonanceJukeboxMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 14;
    private final Container machine;
    private final ResonanceJukeboxBlockEntity jukebox;
    private final ContainerData data;
    private final Player menuPlayer;

    public ResonanceJukeboxMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(ResonanceJukeboxProfile.CONTAINER_SIZE), null,
            new SimpleContainerData(DATA_COUNT));
    }

    public ResonanceJukeboxMenu(int id, Inventory inventory, ResonanceJukeboxBlockEntity jukebox) {
        this(id, inventory, jukebox, jukebox, null);
    }

    private ResonanceJukeboxMenu(int id, Inventory inventory, Container machine,
                                 ResonanceJukeboxBlockEntity jukebox, ContainerData clientData) {
        super(VeloriseEMC.RESONANCE_JUKEBOX_MENU_TYPE.get(), id);
        this.machine = machine;
        this.jukebox = jukebox;
        this.data = clientData != null ? clientData : createServerData();
        this.menuPlayer = inventory.player;
        checkContainerSize(machine, ResonanceJukeboxProfile.CONTAINER_SIZE);
        checkContainerDataCount(this.data, DATA_COUNT);
        machine.startOpen(inventory.player);

        // Twenty records fit in two horizontal rows. This keeps the playlist easy
        // to scan while leaving all playback controls in one compact header.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 10; col++) {
                int slot = col + row * 10;
                addSlot(new DiscSlot(machine, slot, 8 + col * 18, 76 + row * 20));
            }
        }
        addSlot(new FuelSlot(machine, ResonanceJukeboxProfile.FUEL_SLOT, 170, 40));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addPlayerSlot(inventory, col + row * 9 + 9, 16 + col * 18, 142 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            addPlayerSlot(inventory, col, 16 + col * 18, 200);
        }
        addDataSlots(this.data);
    }

    private void addPlayerSlot(Inventory inventory, int slot, int x, int y) {
        int locked = jukebox instanceof PortableMenuBacking portable ? portable.lockedPlayerSlot() : -1;
        addSlot(slot == locked ? new LockedSlot(inventory, slot, x, y) : new Slot(inventory, slot, x, y));
    }

    private ContainerData createServerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long emc = jukebox.getStoredEmc();
                return switch (index) {
                    case 0 -> (int) (emc & 0xFFFFL);
                    case 1 -> (int) ((emc >>> 16) & 0xFFFFL);
                    case 2 -> (int) ((emc >>> 32) & 0xFFFFL);
                    case 3 -> (int) ((emc >>> 48) & 0xFFFFL);
                    case 4 -> jukebox.getSelectedSlot();
                    case 5 -> jukebox.isPlaying() ? 1 : 0;
                    case 6 -> jukebox.getRepeatMode().ordinal();
                    case 7 -> jukebox.isShuffle() ? 1 : 0;
                    case 8 -> jukebox.getElapsedTicks();
                    case 9 -> jukebox.getCurrentLengthTicks();
                    case 10 -> jukebox.getVolume();
                    case 11 -> jukebox.getCoreTierLevel();
                    case 12 -> jukebox.getDistinctDiscTypeCount();
                    case 13 -> jukebox.getRequiredCollectionTypeCount();
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (jukebox == null) return false;
        if (id == 0) jukebox.togglePlayback();
        else if (id == 1) jukebox.previousTrack();
        else if (id == 2) jukebox.nextTrack();
        else if (id == 3) jukebox.cycleRepeatMode();
        else if (id == 4) jukebox.toggleShuffle();
        else if (id >= 10 && id < 10 + jukebox.getAccessibleDiscSlots()) jukebox.selectSlot(id - 10);
        else if (id == 30) jukebox.changeVolume(-10);
        else if (id == 31) jukebox.changeVolume(10);
        else return false;
        broadcastChanges();
        return true;
    }


    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
        if (jukebox != null && slotId >= 0 && slotId < getAccessibleDiscSlots()) {
            jukebox.selectSlot(slotId);
            broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        int machineSlots = ResonanceJukeboxProfile.CONTAINER_SIZE;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ResonanceJukeboxBlockEntity.isPlayable(stack)) {
            if (!moveItemStackTo(stack, 0, getAccessibleDiscSlots(), false)) return ItemStack.EMPTY;
        } else if (EMCRegistry.getAlchemicalFuelEMC(stack) > 0L) {
            if (!moveItemStackTo(stack, ResonanceJukeboxProfile.FUEL_SLOT,
                ResonanceJukeboxProfile.FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (jukebox != null && menuPlayer instanceof ServerPlayer serverPlayer) {
            jukebox.checkCollectionAdvancement(serverPlayer);
        }
    }

    @Override public boolean stillValid(Player player) { return jukebox == null || jukebox instanceof PortableMenuBacking || jukebox.stillValid(player); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        machine.stopOpen(player);
        if (jukebox instanceof PortableMenuBacking portable) portable.savePortableBacking();
    }

    public long getStoredEmc() {
        return ((long) data.get(0) & 0xFFFFL)
            | (((long) data.get(1) & 0xFFFFL) << 16)
            | (((long) data.get(2) & 0xFFFFL) << 32)
            | (((long) data.get(3) & 0xFFFFL) << 48);
    }

    public int getCoreTier() { return Math.max(1, Math.min(4, data.get(11))); }
    public int getDistinctDiscTypeCount() { return Math.max(0, data.get(12)); }
    public int getRequiredCollectionTypeCount() { return Math.max(0, data.get(13)); }
    public int getAccessibleDiscSlots() { return ResonanceJukeboxProfile.discSlots(getCoreTier()); }
    public int getSelectedSlot() { return Math.max(0, Math.min(getAccessibleDiscSlots() - 1, data.get(4))); }
    public boolean isPlaying() { return data.get(5) != 0; }
    public ResonanceJukeboxBlockEntity.RepeatMode getRepeatMode() { return ResonanceJukeboxBlockEntity.RepeatMode.byId(data.get(6)); }
    public boolean isShuffle() { return data.get(7) != 0; }
    public int getElapsedTicks() { return Math.max(0, data.get(8)); }
    public int getCurrentLengthTicks() { return Math.max(0, data.get(9)); }
    public int getVolume() { return data.get(10); }
    public ItemStack getSelectedDisc() { return slots.get(getSelectedSlot()).getItem(); }

    private final class DiscSlot extends Slot {
        private final int discIndex;
        private DiscSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.discIndex = slot;
        }
        @Override public boolean mayPlace(ItemStack stack) {
            return discIndex < getAccessibleDiscSlots() && ResonanceJukeboxBlockEntity.isPlayable(stack);
        }
        @Override public boolean mayPickup(Player player) { return discIndex < getAccessibleDiscSlots(); }
        @Override public int getMaxStackSize() { return 64; }
        @Override public int getMaxStackSize(ItemStack stack) { return 64; }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return EMCRegistry.getAlchemicalFuelEMC(stack) > 0L; }
    }
    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

}
