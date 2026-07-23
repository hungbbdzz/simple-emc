package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Explicit portable-UI registry. Core data alone never makes an arbitrary machine portable.
 * Only Velorise EMC items registered here can open from an inventory stack.
 */
public final class InventoryUiFallback {
    private static final List<Adapter> ADAPTERS = new CopyOnWriteArrayList<>();

    private InventoryUiFallback() {}

    public static void register(Predicate<ItemStack> matcher, BiConsumer<ServerPlayer, ItemStack> opener) {
        if (matcher != null && opener != null) ADAPTERS.add(new Adapter(matcher, opener));
    }

    /** Kept for the Anvil upgrade resolver; no third-party provider is auto-upgraded anymore. */
    public static boolean hasStandardProvider(ItemStack stack) {
        return PortableWorkstationItem.isPortableWorkstation(stack)
            || stack.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get());
    }

    public static boolean canOpen(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get())) return true;
        for (Adapter adapter : ADAPTERS) {
            if (adapter.matcher().test(stack)) return true;
        }
        return false;
    }

    public static int findInventorySlot(ServerPlayer player, ItemStack stack) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot) == stack) return slot;
        }
        return -1;
    }

    public static boolean open(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(VeloriseEMC.PURIFICATION_GRINDSTONE_ITEM.get())) {
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new PurificationGrindstoneMenu(
                    id, inventory, net.minecraft.world.inventory.ContainerLevelAccess.NULL, true,
                    CoreTierData.get(stack).level()),
                Component.translatable("container.velorise_emc.purification_grindstone")
            ));
            return true;
        }
        for (Adapter adapter : ADAPTERS) {
            if (adapter.matcher().test(stack)) {
                adapter.opener().accept(player, stack);
                return true;
            }
        }
        return false;
    }

    private record Adapter(Predicate<ItemStack> matcher, BiConsumer<ServerPlayer, ItemStack> opener) {}
}
