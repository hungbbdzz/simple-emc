package com.velorise.emc;

import com.velorise.emc.capability.PlayerEMCCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TransmutationMenu extends AbstractContainerMenu {
    private final Container inputContainer = new SimpleContainer(1);
    private final Inventory playerInventory;
    private final ContainerLevelAccess access;

    public TransmutationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public TransmutationMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(VeloriseEMC.TRANSMUTATION_MENU_TYPE.get(), containerId);
        this.playerInventory = playerInventory;
        this.access = access == null ? ContainerLevelAccess.NULL : access;

        // Slot 0: Input / Burn slot (in middle of right inset panel)
        this.addSlot(new Slot(inputContainer, 0, 169, 52) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if (!stack.isEmpty()) {
                    if (playerInventory.player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
                            if (stack.getItem() instanceof TomeOfKnowledgeItem) {
                                for (net.minecraft.world.item.Item item : EMCRegistry.getAllItems()) {
                                    if (EMCRegistry.canLearn(item)) data.learnItem(item);
                                }
                                set(ItemStack.EMPTY);
                                VeloriseEMC.syncPlayerData(serverPlayer);
                            } else if (stack.getItem() instanceof KnowledgeSharingBookItem && KnowledgeSharingBookItem.isWritten(stack)) {
                                for (net.minecraft.resources.ResourceLocation rl : KnowledgeSharingBookItem.getStoredItems(stack)) {
                                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                                    if (item != null && item != net.minecraft.world.item.Items.AIR && EMCRegistry.canLearn(item)) {
                                        data.learnItem(item);
                                    }
                                }
                                set(ItemStack.EMPTY);
                                VeloriseEMC.syncPlayerData(serverPlayer);
                            } else {
                                long totalEmc = EMCRegistry.getEMC(stack);
                                if (totalEmc > 0 && EMCRegistry.canBurn(stack.getItem())) {
                                    data.addEmc(totalEmc);
                                    if (EMCRegistry.canLearn(stack.getItem())) data.learnItem(stack.getItem());
                                    AdvancementHooks.onBurn(serverPlayer, stack.getItem(), data);
                                    set(ItemStack.EMPTY);
                                    VeloriseEMC.syncPlayerData(serverPlayer);
                                }
                            }
                        });
                    }
                }
            }
        });

        // Slots 1-27: Player inventory (centered at x = 20)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 20 + col * 18, 130 + row * 18));
            }
        }

        // Slots 28-36: Player Hotbar (centered at x = 20)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 20 + col * 18, 186));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 1) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (EMCRegistry.hasEMC(itemstack1) && EMCRegistry.canBurn(itemstack1.getItem())) {
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
        return AbstractContainerMenu.stillValid(this.access, player, VeloriseEMC.TRANSMUTATION_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.inputContainer);
    }
}
