package com.velorise.simpleemc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TransmutationMenu extends AbstractContainerMenu {
    private final Container inputContainer = new SimpleContainer(1);
    private final Inventory playerInventory;

    public TransmutationMenu(int containerId, Inventory playerInventory) {
        super(SimpleEMC.TRANSMUTATION_MENU_TYPE.get(), containerId);
        this.playerInventory = playerInventory;

        // Input / Burn slot
        this.addSlot(new Slot(inputContainer, 0, 150, 22) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if (!stack.isEmpty()) {
                    if (playerInventory.player instanceof ServerPlayer serverPlayer) {
                        PlayerEMC data = serverPlayer.getData(SimpleEMC.PLAYER_EMC.get());
                        if (stack.getItem() instanceof TomeOfKnowledgeItem) {
                            for (net.minecraft.world.item.Item item : EMCRegistry.getAllItems()) {
                                data.learnItem(item);
                            }
                            set(ItemStack.EMPTY);
                            SimpleEMC.syncPlayerData(serverPlayer);
                        } else if (stack.getItem() instanceof KnowledgeSharingBookItem && KnowledgeSharingBookItem.isWritten(stack)) {
                            for (net.minecraft.resources.ResourceLocation rl : KnowledgeSharingBookItem.getStoredItems(stack)) {
                                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    data.learnItem(item);
                                }
                            }
                            set(ItemStack.EMPTY);
                            SimpleEMC.syncPlayerData(serverPlayer);
                        } else {
                            long itemEmc = EMCRegistry.getEMC(stack.getItem());
                            if (itemEmc > 0) {
                                long totalEmc = itemEmc * stack.getCount();
                                data.addEmc(totalEmc);
                                data.learnItem(stack.getItem());
                                
                                // Clear slot
                                set(ItemStack.EMPTY);
                                
                                // Sync
                                SimpleEMC.syncPlayerData(serverPlayer);
                            }
                        }
                    }
                }
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 1) { // From input slot to player inventory
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory to input slot (which burns it immediately)
                if (EMCRegistry.hasEMC(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.inputContainer);
    }
}
