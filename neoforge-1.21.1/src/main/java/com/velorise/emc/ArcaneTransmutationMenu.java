package com.velorise.emc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

public class ArcaneTransmutationMenu extends AbstractContainerMenu {
    private final Container inputContainer = new SimpleContainer(1);
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Inventory playerInventory;
    private final ContainerLevelAccess access;

    public ArcaneTransmutationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public ArcaneTransmutationMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(VeloriseEMC.ARCANE_TRANSMUTATION_MENU_TYPE.get(), containerId);
        this.playerInventory = playerInventory;
        this.access = access == null ? ContainerLevelAccess.NULL : access;

        // Slot 0: Burn slot (at bottom-right of right panel)
        this.addSlot(new Slot(inputContainer, 0, 211, 68) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                if (!stack.isEmpty()) {
                    if (playerInventory.player instanceof ServerPlayer serverPlayer) {
                        PlayerEMC data = serverPlayer.getData(VeloriseEMC.PLAYER_EMC.get());
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
                    }
                }
            }
        });

        // Slots 1-9: Crafting grid 3x3
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(craftSlots, col + row * 3, 144 + col * 18, 32 + row * 18));
            }
        }

        // Slot 10: Crafting result slot (middle right)
        this.addSlot(new ResultSlot(playerInventory.player, craftSlots, resultSlots, 0, 211, 50));

        // Slots 11-37: Player inventory (centered at x = 39)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 39 + col * 18, 130 + row * 18));
            }
        }

        // Slots 38-46: Player Hotbar (centered at x = 39)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 39 + col * 18, 186));
        }
    }

    public void addCraftingStack(ItemStack stack) {
        // Try to place into crafting grid slots (slots 0 to 8 in craftSlots)
        for (int i = 0; i < 9; i++) {
            ItemStack slotStack = this.craftSlots.getItem(i);
            if (slotStack.isEmpty()) {
                int countToPut = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack newStack = stack.copy();
                newStack.setCount(countToPut);
                this.craftSlots.setItem(i, newStack);
                stack.shrink(countToPut);
                break;
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
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
                CraftingInput input = this.craftSlots.asCraftInput();
                var opt = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level());
                if (opt.isPresent()) {
                    result = opt.get().value().assemble(input, player.level().registryAccess());
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
            if (index == 10) { // Crafting Result slot
                if (!this.moveItemStackTo(itemstack1, 11, 47, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index >= 1 && index <= 9) { // Crafting grid slots
                if (!this.moveItemStackTo(itemstack1, 11, 47, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == 0) { // Burn slot
                if (!this.moveItemStackTo(itemstack1, 11, 47, true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory
                if (EMCRegistry.hasEMC(itemstack1) && EMCRegistry.canBurn(itemstack1.getItem())) {
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

    /** Exposed for server-side JEI fill handler and for client-side JEI transfer check. */
    public CraftingContainer getCraftSlots() {
        return this.craftSlots;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, VeloriseEMC.ARCANE_TRANSMUTATION_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.craftSlots);
        this.clearContainer(player, this.inputContainer);
    }
}
