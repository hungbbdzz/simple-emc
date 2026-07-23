package com.velorise.emc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.StonecutterMenu;

/** Opens Vanilla workstation menus without creating a fake BlockEntity. */
public final class PortableWorkstationUi {
    private PortableWorkstationUi() {}

    public static void open(ServerPlayer player, PortableWorkstationItem.Kind kind) {
        player.openMenu(provider(kind));
    }

    public static MenuProvider provider(PortableWorkstationItem.Kind kind) {
        return new SimpleMenuProvider((id, inventory, ignored) -> switch (kind) {
            case CRAFTING -> new PortableCraftingMenu(id, inventory);
            case STONECUTTER -> new PortableStonecutterMenu(id, inventory);
            case LOOM -> new PortableLoomMenu(id, inventory);
            case CARTOGRAPHY -> new PortableCartographyMenu(id, inventory);
        }, title(kind));
    }

    private static Component title(PortableWorkstationItem.Kind kind) {
        return Component.translatable(switch (kind) {
            case CRAFTING -> "block.velorise_emc.portable_crafting_table";
            case STONECUTTER -> "block.velorise_emc.portable_stonecutter";
            case LOOM -> "block.velorise_emc.portable_loom";
            case CARTOGRAPHY -> "block.velorise_emc.portable_cartography_table";
        });
    }

    private static final class PortableCraftingMenu extends CraftingMenu {
        private PortableCraftingMenu(int id, net.minecraft.world.entity.player.Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.NULL);
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return player.isAlive(); }
    }

    private static final class PortableStonecutterMenu extends StonecutterMenu {
        private PortableStonecutterMenu(int id, net.minecraft.world.entity.player.Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.NULL);
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return player.isAlive(); }
    }

    private static final class PortableLoomMenu extends LoomMenu {
        private PortableLoomMenu(int id, net.minecraft.world.entity.player.Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.NULL);
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return player.isAlive(); }
    }

    private static final class PortableCartographyMenu extends CartographyTableMenu {
        private PortableCartographyMenu(int id, net.minecraft.world.entity.player.Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.NULL);
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return player.isAlive(); }
    }
}
