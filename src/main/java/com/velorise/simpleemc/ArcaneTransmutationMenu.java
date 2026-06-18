package com.velorise.simpleemc;

import com.velorise.simpleemc.capability.PlayerEMCCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class ArcaneTransmutationMenu extends AbstractContainerMenu {
    private final Container inputContainer = new SimpleContainer(1);
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Inventory playerInventory;

    public ArcaneTransmutationMenu(int containerId, Inventory playerInventory) {
        super(SimpleEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get(), containerId);
        this.playerInventory = playerInventory;

        // Slot 0: Burn slot
        this.addSlot(new Slot(inputContainer, 0, 216, 22) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if (!stack.isEmpty()) {
                    if (playerInventory.player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.getCapability(PlayerEMCCapability.PLAYER_EMC_CAP).ifPresent(data -> {
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
                                    set(ItemStack.EMPTY);
                                    SimpleEMC.syncPlayerData(serverPlayer);
                                }
                            }
                        });
                    }
                }
            }
        });

        // Slots 1-9: Crafting grid 3x3
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(craftSlots, col + row * 3, 130 + col * 18, 41 + row * 18));
            }
        }

        // Slot 10: Crafting result slot
        this.addSlot(new ResultSlot(playerInventory.player, craftSlots, resultSlots, 0, 216, 59));

        // Slots 11-37: Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 43 + col * 18, 120 + row * 18));
            }
        }

        // Slots 38-46: Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 43 + col * 18, 176));
        }
    }

    public void addCraftingStack(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            ItemStack slotStack = this.craftSlots.getItem(i);
            if (slotStack.isEmpty()) {
                int countToPut = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack newStack = stack.copy();
                newStack.setCount(countToPut);
                this.craftSlots.setItem(i, newStack);
                stack.shrink(countToPut);
                break;
            } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space > 0) {
                    int countToPut = Math.min(stack.getCount(), space);
                    slotStack.grow(countToPut);
                    stack.shrink(countToPut);
                    this.craftSlots.setItem(i, slotStack);
                }
            }
            if (stack.isEmpty()) {
                break;
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.craftSlots) {
            Player player = this.playerInventory.player;
            if (!player.level().isClientSide() && player instanceof ServerPlayer) {
                ItemStack result = ItemStack.EMPTY;
                var opt = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftSlots, player.level());
                if (opt.isPresent()) {
                    result = opt.get().assemble(this.craftSlots, player.level().registryAccess());
                }
                this.resultSlots.setItem(0, result);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 10) {
                if (!this.moveItemStackTo(itemstack1, 11, 47, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index >= 1 && index <= 9) {
                if (!this.moveItemStackTo(itemstack1, 11, 47, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 11, 47, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (EMCRegistry.hasEMC(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                        return ItemStack.EMPTY;
                    }
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

    public CraftingContainer getCraftSlots() {
        return this.craftSlots;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.craftSlots);
        this.clearContainer(player, this.inputContainer);
    }
}
