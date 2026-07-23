package com.velorise.emc;

import com.velorise.emc.common.PurificationProfile;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

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
        List<Choice> selected = choices(input, all, curse);
        if (selected.isEmpty()) return Preview.EMPTY;

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        for (Choice choice : selected) {
            EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(choice.enchantment(), choice.level()));
        }
        int operation = curse ? EXTRACT_CURSE : all ? EXTRACT_ALL : EXTRACT_ONE;
        return new Preview(operation, book, PurificationProfile.operationCost(tier), 0);
    }

    public static ItemStack commitExtraction(ItemStack source, boolean all, boolean curse) {
        List<Choice> selected = choices(source, all, curse);
        if (selected.isEmpty()) return source.copy();

        ItemStack remainder = source.copy();
        remainder.setCount(1);
        EnchantmentHelper.updateEnchantments(remainder, mutable -> {
            for (Choice choice : selected) mutable.set(choice.enchantment(), 0);
        });
        if (source.is(Items.ENCHANTED_BOOK)
            && EnchantmentHelper.getEnchantmentsForCrafting(remainder).isEmpty()) {
            ItemStack book = new ItemStack(Items.BOOK);
            if (source.has(DataComponents.CUSTOM_NAME)) {
                book.set(DataComponents.CUSTOM_NAME, source.get(DataComponents.CUSTOM_NAME));
            }
            return book;
        }
        return remainder;
    }

    private static List<Choice> choices(ItemStack input, boolean all, boolean curse) {
        List<Choice> result = new ArrayList<>();
        if (input.isEmpty()) return result;
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(input).entrySet()) {
            boolean entryCurse = entry.getKey().is(EnchantmentTags.CURSE);
            if (entry.getIntValue() <= 0 || entryCurse != curse) continue;
            if (!all) result.clear();
            result.add(new Choice(entry.getKey(), entry.getIntValue()));
        }
        return result;
    }

    private record Choice(Holder<Enchantment> enchantment, int level) {}

    public record Preview(int operation, ItemStack output, long cost, int experience) {
        public static final Preview EMPTY = new Preview(NONE, ItemStack.EMPTY, 0L, 0);
    }
}
