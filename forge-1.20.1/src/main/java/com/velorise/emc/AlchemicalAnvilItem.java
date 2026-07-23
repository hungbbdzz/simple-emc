package com.velorise.emc;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Fixed-tier block item for one of the four separately registered Alchemical Anvils. */
public final class AlchemicalAnvilItem extends GlintBlockItem {
    private final int anvilLevel;

    public AlchemicalAnvilItem(Block block, Properties properties, int anvilLevel) {
        super(block, properties);
        this.anvilLevel = Math.max(1, Math.min(4, anvilLevel));
    }

    public int anvilLevel() { return anvilLevel; }

    public static boolean isAnvil(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AlchemicalAnvilItem;
    }

    public static int level(ItemStack stack) {
        return stack.getItem() instanceof AlchemicalAnvilItem item ? item.anvilLevel : 0;
    }

    public static ItemStack createStack(int level) {
        return new ItemStack(switch (Math.max(1, Math.min(4, level))) {
            case 2 -> VeloriseEMC.ALCHEMICAL_ANVIL_II_ITEM.get();
            case 3 -> VeloriseEMC.ALCHEMICAL_ANVIL_III_ITEM.get();
            case 4 -> VeloriseEMC.ALCHEMICAL_ANVIL_IV_ITEM.get();
            default -> VeloriseEMC.ALCHEMICAL_ANVIL_I_ITEM.get();
        });
    }

    public static String roman(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "I";
        };
    }
}
