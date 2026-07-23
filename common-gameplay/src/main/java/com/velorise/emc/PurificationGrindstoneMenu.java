package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PurificationGrindstoneMenu extends AbstractContainerMenu {
    private final SimpleContainer inputs = new SimpleContainer(2);
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final boolean portable;
    private final DataSlot operation = DataSlot.standalone();
    private final DataSlot cost = DataSlot.standalone();
    private final DataSlot experience = DataSlot.standalone();
    private final DataSlot coreTier = DataSlot.standalone();

    public PurificationGrindstoneMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, true, 1);
    }

    public PurificationGrindstoneMenu(int containerId, Inventory inventory,
                                      ContainerLevelAccess access, boolean portable) {
        this(containerId, inventory, access, portable, 1);
    }

    public PurificationGrindstoneMenu(int containerId, Inventory inventory,
                                      ContainerLevelAccess access, boolean portable, int coreTier) {
        super(VeloriseEMC.PURIFICATION_GRINDSTONE_MENU_TYPE.get(), containerId);
        this.access = access;
        this.portable = portable;
        this.coreTier.set(Math.max(1, Math.min(4, coreTier)));

        inputs.addListener(this::slotsChanged);
        // Match the vanilla Grindstone slot geometry exactly.
        addSlot(new Slot(inputs, 0, 49, 19) {
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(inputs, 1, 49, 40) {
            @Override public boolean mayPlace(ItemStack stack) {
                return getCoreTier() >= 3;
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(result, 0, 129, 30) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public boolean mayPickup(Player player) {
                return getOperation() != PurificationGrindstoneLogic.NONE && !getItem().isEmpty()
                    && (!(player instanceof ServerPlayer serverPlayer)
                    || getCost() <= 0L || EmcTransactionService.canSpend(serverPlayer, getCost()));
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (player instanceof ServerPlayer serverPlayer
                    && getCost() > 0L && !EmcTransactionService.spend(serverPlayer, getCost())) return;

                int op = getOperation();
                if (op == PurificationGrindstoneLogic.EXTRACT_ONE) {
                    inputs.setItem(0, PurificationGrindstoneLogic.commitExtraction(inputs.getItem(0), false, false));
                } else if (op == PurificationGrindstoneLogic.EXTRACT_ALL) {
                    inputs.setItem(0, PurificationGrindstoneLogic.commitExtraction(inputs.getItem(0), true, false));
                } else if (op == PurificationGrindstoneLogic.EXTRACT_CURSE) {
                    inputs.setItem(1, PurificationGrindstoneLogic.commitExtraction(inputs.getItem(1), false, true));
                }

                if (getExperience() > 0) player.giveExperiencePoints(getExperience());
                access.execute((level, pos) -> level.playSound(null, pos,
                    SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.9F, 1.05F));
                if (portable) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE,
                        SoundSource.PLAYERS, 0.7F, 1.05F);
                }
                if (getCost() > 0L) {
                    player.displayClientMessage(Component.literal("§dPurification complete §7(-" + getCost() + " EMC)"), true);
                }
                slotsChanged(inputs);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 83 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 141));

        addDataSlot(operation);
        addDataSlot(cost);
        addDataSlot(experience);
        addDataSlot(this.coreTier);
        slotsChanged(inputs);
    }

    @Override
    public void slotsChanged(Container container) {
        PurificationGrindstoneLogic.Preview preview = PurificationGrindstoneLogic.preview(
            inputs.getItem(0), inputs.getItem(1), getCoreTier());
        operation.set(preview.operation());
        cost.set((int)Math.min(Integer.MAX_VALUE, Math.max(0L, preview.cost())));
        experience.set(Math.max(0, preview.experience()));
        result.setItem(0, preview.output().copy());
        broadcastChanges();
    }

    public int getOperation() { return operation.get(); }
    public long getCost() { return Integer.toUnsignedLong(cost.get()); }
    public int getExperience() { return Math.max(0, experience.get()); }
    public int getCoreTier() { return Math.max(1, Math.min(4, coreTier.get())); }

    @Override
    public boolean stillValid(Player player) {
        return portable || AbstractContainerMenu.stillValid(access, player, VeloriseEMC.PURIFICATION_GRINDSTONE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return moved;
        ItemStack source = slot.getItem();
        moved = source.copy();

        if (index == 2) {
            if (!moveItemStackTo(source, 3, 39, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(source, moved);
        } else if (index >= 3) {
            int end = getCoreTier() >= 3 ? 2 : 1;
            if (!moveItemStackTo(source, 0, end, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 3, 39, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, inputs);
    }
}
