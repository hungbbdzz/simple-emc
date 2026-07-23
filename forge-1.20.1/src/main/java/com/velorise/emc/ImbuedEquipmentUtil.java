package com.velorise.emc;

import com.velorise.emc.common.CoreEquipmentProfile;
import com.velorise.emc.common.WirelessCoreTier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ImbuedEquipmentUtil {
    public static final String TAG_IMBUED = "VeloriseEMCImbued";
    public static final String TAG_MODE = "VeloriseEMCMode";
    public static final String TAG_ACTIVE = "VeloriseEMCActive";
    public static final String TAG_NEXT_CHARGE_TICK = "VeloriseEMCNextChargeTick";
    public static final String TAG_MINING_ACTIVE_UNTIL = "VeloriseEMCMiningActiveUntil";
    public static final String TAG_ACTION_COOLDOWN_TICK = "VeloriseEMCActionCooldownTick";
    public static final int MIN_MODE = 1;
    public static final int MAX_MODE = 4;

    private ImbuedEquipmentUtil() {}

    public static boolean isImbued(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(TAG_IMBUED);
    }

    public static void setImbued(ItemStack stack, boolean imbued) {
        if (stack.isEmpty()) return;
        stack.getOrCreateTag().putBoolean(TAG_IMBUED, imbued);
        if (!imbued) {
            stack.getOrCreateTag().remove(TAG_MODE);
            stack.getOrCreateTag().remove(TAG_ACTIVE);
            stack.getOrCreateTag().remove(TAG_NEXT_CHARGE_TICK);
            stack.getOrCreateTag().remove(TAG_MINING_ACTIVE_UNTIL);
        }
    }

    public static int getMode(ItemStack stack) {
        if (!supportsMode(stack)) return 1;
        int max = CoreTierData.maximumMode(stack);
        if (!stack.hasTag() || !stack.getTag().contains("CoreMode")) return 1;
        return Math.min(max, Math.max(1, stack.getTag().getInt("CoreMode")));
    }

    public static void setMode(ItemStack stack, int mode) {
        if (!supportsMode(stack)) return;
        int max = CoreTierData.maximumMode(stack);
        int clamped = Math.min(max, Math.max(1, mode));
        stack.getOrCreateTag().putInt("CoreMode", clamped);
    }

    public static boolean isActive(ItemStack stack) {
        return isImbued(stack);
    }

    public static void setActive(ItemStack stack, boolean active) {
        // Core-bound equipment bonuses are always passive.
    }

    public static long getNextChargeTick(ItemStack stack) {
        return stack.isEmpty() || !stack.hasTag() ? 0L : stack.getTag().getLong(TAG_NEXT_CHARGE_TICK);
    }

    public static void setNextChargeTick(ItemStack stack, long gameTick) {
        if (!stack.isEmpty()) stack.getOrCreateTag().putLong(TAG_NEXT_CHARGE_TICK, Math.max(0L, gameTick));
    }

    public static long getMiningActiveUntil(ItemStack stack) {
        return stack.isEmpty() || !stack.hasTag() ? 0L : stack.getTag().getLong(TAG_MINING_ACTIVE_UNTIL);
    }

    public static void setMiningActiveUntil(ItemStack stack, long gameTick) {
        if (!stack.isEmpty()) stack.getOrCreateTag().putLong(TAG_MINING_ACTIVE_UNTIL, Math.max(0L, gameTick));
    }

    public static boolean isMineableTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        return item instanceof net.minecraft.world.item.DiggerItem
            || item instanceof net.minecraft.world.item.PickaxeItem
            || item instanceof net.minecraft.world.item.ShovelItem
            || item instanceof net.minecraft.world.item.AxeItem
            || item instanceof net.minecraft.world.item.HoeItem;
    }

    public static boolean supportsMode(ItemStack stack) {
        return isImbued(stack) && isMineableTool(stack);
    }

    public static boolean usesActiveMode(ItemStack stack) {
        return false;
    }

    public static boolean drainsWhileHeld(ItemStack stack) {
        return false;
    }

    public static boolean usesMiningActivation(ItemStack stack) {
        return false;
    }

    public static long miningCostPerSecond(ItemStack stack) {
        return 0L;
    }

    public static boolean drainsWhileUsing(ItemStack stack) {
        return false;
    }

    public static long useCostPerSecond(ItemStack stack) {
        return 0L;
    }



    public static boolean isElytra(ItemStack stack) {
        return isImbued(stack) && ImbuedCompatibility.isElytra(stack);
    }


    public static long activeCostPerSecond(ItemStack stack) {
        return 0L;
    }

    /**
     * Universal passive damage bonus for any Core-bound item. The bonus is only consumed by
     * an actual damage event, so non-weapon items gain nothing unless another mod legitimately
     * uses that stack to deal damage.
     */
    public static float damageBonus(ItemStack stack) {
        if (!isImbued(stack) || !ImbuedCompatibility.allowsDamageBonus(stack)) return 0.0F;
        return CoreEquipmentProfile.weaponDamageBonus(CoreTierData.get(stack).level());
    }

    /** Retained for source compatibility with the previous melee-only implementation. */
    public static float configuredSwordBonusDamage(ItemStack stack) {
        return damageBonus(stack);
    }

    /** Retained for source compatibility with the previous melee-only implementation. */
    public static float swordBonusDamage(ItemStack stack) {
        return damageBonus(stack);
    }

    public static long getActionCooldownTick(ItemStack stack) {
        if (stack.isEmpty()) return 0L;
        return stack.hasTag() ? stack.getTag().getLong(TAG_ACTION_COOLDOWN_TICK) : 0L;
    }

    public static void setActionCooldownTick(ItemStack stack, long gameTick) {
        if (!stack.isEmpty()) stack.getOrCreateTag().putLong(TAG_ACTION_COOLDOWN_TICK, Math.max(0L, gameTick));
    }


    public static ItemStack createCreativeVariant(ItemStack vanillaStack) {
        ItemStack result = vanillaStack.copy();
        result.setCount(1);
        setImbued(result, true);
        CoreTierData.set(result, WirelessCoreTier.IV);
        String nameKey = specialNameKey(vanillaStack);
        result.setHoverName(nameKey == null
            ? Component.translatable("item.velorise_emc.imbued_variant", vanillaStack.getHoverName())
            : Component.translatable(nameKey));
        return result;
    }

    public static String specialNameKey(ItemStack stack) {
        if (stack.is(Items.FISHING_ROD)) return "item.velorise_emc.imbued_fishing_rod";
        if (stack.is(Items.FLINT_AND_STEEL)) return "item.velorise_emc.cursed_igniter";
        if (stack.is(Items.TURTLE_HELMET)) return "item.velorise_emc.tidal_turtle_helmet";
        if (stack.is(Items.ELYTRA)) return "item.velorise_emc.aetheric_elytra";
        return null;
    }
}
