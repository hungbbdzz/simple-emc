package com.velorise.emc;

import com.velorise.emc.common.DeepboundStorageProfile;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Paged server-authoritative menu shared by Deepbound Chest and Barrel. */
public class DeepboundStorageMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 3;
    private static final int PLAYER_SLOT_COUNT = 36;

    private final DeepboundStorageAccess storage;
    private final ContainerData data;
    private int selectedPage;

    public DeepboundStorageMenu(int id, Inventory inventory) {
        this(id, inventory, new ClientStorage(), new SimpleContainerData(DATA_COUNT));
    }

    public DeepboundStorageMenu(int id, Inventory inventory, DeepboundStorageAccess storage) {
        this(id, inventory, storage, null);
    }

    private DeepboundStorageMenu(int id, Inventory inventory, DeepboundStorageAccess storage, ContainerData clientData) {
        super(VeloriseEMC.DEEPBOUND_STORAGE_MENU_TYPE.get(), id);
        this.storage = storage;
        this.data = clientData != null ? clientData : createServerData();

        for (int index = 0; index < DeepboundStorageProfile.MAX_SLOTS; index++) {
            int local = index % DeepboundStorageProfile.SLOTS_PER_PAGE;
            int x = 8 + (local % 9) * 18;
            int y = 46 + (local / 9) * 18;
            addSlot(new PagedStorageSlot(storage, index, x, y, this));
        }

        int playerY = 120;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = column + row * 9 + 9;
                addSlot(inventorySlot == storage.lockedPlayerSlot()
                    ? new LockedSlot(inventory, inventorySlot, 8 + column * 18, playerY + row * 18)
                    : new Slot(inventory, inventorySlot, 8 + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(column == storage.lockedPlayerSlot()
                ? new LockedSlot(inventory, column, 8 + column * 18, playerY + 58)
                : new Slot(inventory, column, 8 + column * 18, playerY + 58));
        }

        addDataSlots(data);
        storage.onMenuOpened(inventory.player);
    }

    private ContainerData createServerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> selectedPage;
                    case 1 -> storage.getCoreTier().level();
                    case 2 -> storage.getAccessibleSlotCount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) selectedPage = storage.clampPage(value);
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public int getCurrentPage() {
        return Math.max(0, Math.min(getPageCount() - 1, data.get(0)));
    }

    public int getPageCount() {
        return Math.max(1, data.get(2) / DeepboundStorageProfile.SLOTS_PER_PAGE);
    }

    public int getCoreTierLevel() {
        return Math.max(1, Math.min(4, data.get(1)));
    }

    public int getAccessibleSlotCount() {
        return Math.max(0, Math.min(DeepboundStorageProfile.MAX_SLOTS, data.get(2)));
    }

    boolean isStorageSlotVisible(int storageIndex) {
        return storageIndex >= 0
            && storageIndex < getAccessibleSlotCount()
            && storageIndex / DeepboundStorageProfile.SLOTS_PER_PAGE == getCurrentPage();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        int page = getCurrentPage();
        if (buttonId == 0) page--;
        else if (buttonId == 1) page++;
        else return false;
        selectedPage = storage.clampPage(page);
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        int playerStart = DeepboundStorageProfile.MAX_SLOTS;
        int playerEnd = playerStart + PLAYER_SLOT_COUNT;

        if (index < playerStart) {
            if (index >= getAccessibleSlotCount()
                || !moveItemStackTo(source, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, getAccessibleSlotCount(), false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        storage.onMenuClosed(player);
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static final class ClientStorage extends SimpleContainer implements DeepboundStorageAccess {
        private ClientStorage() { super(DeepboundStorageProfile.MAX_SLOTS); }
        public com.velorise.emc.common.WirelessCoreTier getCoreTier() { return com.velorise.emc.common.WirelessCoreTier.I; }
        public int getAccessibleSlotCount() { return DeepboundStorageProfile.MAX_SLOTS; }
        public int clampPage(int page) { return Math.max(0, Math.min(5, page)); }
        public boolean stillValid(Player player) { return true; }
    }

    private static final class PagedStorageSlot extends Slot {
        private final int storageIndex;
        private final DeepboundStorageMenu menu;

        private PagedStorageSlot(Container container, int storageIndex, int x, int y,
                                 DeepboundStorageMenu menu) {
            super(container, storageIndex, x, y);
            this.storageIndex = storageIndex;
            this.menu = menu;
        }

        @Override
        public boolean isActive() {
            return menu.isStorageSlotVisible(storageIndex);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return storageIndex < menu.getAccessibleSlotCount() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return storageIndex < menu.getAccessibleSlotCount() && super.mayPickup(player);
        }
    }
}
