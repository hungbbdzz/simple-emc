package com.velorise.emc;

import com.velorise.emc.common.PurificationProfile;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.LinkedHashMap;
import java.util.Map;

/** Core-tiered enchantment extraction used by the Purification Grindstone. */
public final class PurificationGrindstoneLogic {
    public static final int NONE = 0;
    public static final int EXTRACT_ALL = 1;
    public static final int EXTRACT_ONE = 2;
    public static final int EXTRACT_CURSE = 3;

    private PurificationGrindstoneLogic() {}

    public static Preview preview(ItemStack firstInput, ItemStack secondInput, int coreTier) {
        int tier = Math.max(1, Math.min(3, coreTier));
        boolean allNormal = tier == 1;
        Preview normal = previewExtraction(firstInput, allNormal, false, tier);
        if (normal.operation() != NONE) return normal;
        return tier >= 3 ? previewExtraction(secondInput, false, true, tier) : Preview.EMPTY;
    }

    private static Preview previewExtraction(ItemStack input, boolean all, boolean curse, int tier) {
        if (input.isEmpty()) return Preview.EMPTY;
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(input));
        LinkedHashMap<Enchantment, Integer> selected = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getValue() <= 0 || entry.getKey().isCurse() != curse) continue;
            if (!all) selected.clear();
            selected.put(entry.getKey(), entry.getValue());
        }
        if (selected.isEmpty()) return Preview.EMPTY;

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        selected.forEach((enchantment, level) ->
            EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, level)));
        int operation = curse ? EXTRACT_CURSE : all ? EXTRACT_ALL : EXTRACT_ONE;
        return new Preview(operation, book, PurificationProfile.operationCost(tier), 0);
    }

    public static ItemStack commitExtraction(ItemStack source, boolean all, boolean curse) {
        if (source.isEmpty()) return ItemStack.EMPTY;
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(source));
        Enchantment selected = null;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getValue() > 0 && entry.getKey().isCurse() == curse) selected = entry.getKey();
        }
        if (selected == null) return source.copy();
        if (all) enchantments.entrySet().removeIf(entry -> entry.getKey().isCurse() == curse);
        else enchantments.remove(selected);

        if (source.is(Items.ENCHANTED_BOOK) && enchantments.isEmpty()) {
            ItemStack book = new ItemStack(Items.BOOK);
            if (source.hasCustomHoverName()) book.setHoverName(source.getHoverName());
            return book;
        }
        ItemStack remainder = source.copy();
        remainder.setCount(1);
        EnchantmentHelper.setEnchantments(enchantments, remainder);
        return remainder;
    }

    public record Preview(int operation, ItemStack output, long cost, int experience) {
        public static final Preview EMPTY = new Preview(NONE, ItemStack.EMPTY, 0L, 0);
    }
}
