package com.velorise.emc;

import com.velorise.emc.common.ImbuedMeleeProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Loader-local compatibility surface for Core-bound equipment. */
public final class ImbuedCompatibility {
    public enum Kind {
        REPAIR_ONLY,
        ACTIVE_MELEE,
        ACTIVE_MINING,
        ELYTRA,
        FISHING,
        IGNITER,
        TIDAL_HELMET
    }

    public static final TagKey<Item> IMBUE_BLACKLIST = tag("imbue_blacklist");
    public static final TagKey<Item> REPAIR_ONLY = tag("repair_only");
    public static final TagKey<Item> ACTIVE_MELEE = tag("active_melee_weapons");
    private static final TagKey<Item> VANILLA_STYLE_SWORDS = externalTag("minecraft", "enchantable/sword");
    public static final TagKey<Item> ACTIVE_MINING = tag("active_mining_tools");
    public static final TagKey<Item> LEFT_HOLD_TOOLS = tag("left_hold_tools");
    public static final TagKey<Item> ELYTRAS = tag("elytra_items");

    private static final List<Rule> RULES = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<Player, Consumer<ItemStack>>> EQUIPPED_PROVIDERS = new CopyOnWriteArrayList<>();

    private ImbuedCompatibility() {}

    public static boolean canImbue(ItemStack stack) {
        return !stack.isEmpty()
            && stack.isDamageableItem()
            && !stack.is(IMBUE_BLACKLIST)
            && !(stack.getItem() instanceof ElytraItem);
    }

    /**
     * Retained for binary/source compatibility with older integrations. Vanilla combat
     * equipment is no longer retired; the datapack blacklist is the single opt-out.
     */
    public static boolean isRetiredVanillaCombatItem(ItemStack stack) {
        return false;
    }

    /** Explicit repair-only rules remain the server/datapack opt-out from universal damage. */
    public static boolean allowsDamageBonus(ItemStack stack) {
        if (stack.isEmpty() || stack.is(REPAIR_ONLY)) return false;
        return !ImbuedMeleeProfile.forceRepairOnly(registryId(stack));
    }

    public static Kind kind(ItemStack stack) {
        if (stack.isEmpty()) return Kind.REPAIR_ONLY;
        String registryId = registryId(stack);
        if (ImbuedMeleeProfile.forceRepairOnly(registryId) || stack.is(REPAIR_ONLY)) return Kind.REPAIR_ONLY;
        if (ImbuedMeleeProfile.configuredMelee(registryId)) return Kind.ACTIVE_MELEE;

        for (Rule rule : RULES) {
            try {
                if (rule.predicate().test(stack)) return rule.kind();
            } catch (RuntimeException ignored) {
                // Optional third-party classifiers are isolated from core item processing.
            }
        }

        if (stack.is(Items.FISHING_ROD)) return Kind.FISHING;
        if (stack.is(Items.FLINT_AND_STEEL)) return Kind.IGNITER;
        if (stack.is(Items.TURTLE_HELMET)) return Kind.TIDAL_HELMET;
        if (stack.is(ELYTRAS) || stack.getItem() instanceof ElytraItem) return Kind.ELYTRA;
        if (stack.is(ACTIVE_MELEE) || stack.is(VANILLA_STYLE_SWORDS) || stack.getItem() instanceof SwordItem
            || stack.is(Items.TRIDENT) || stack.is(Items.MACE)) return Kind.ACTIVE_MELEE;
        if (stack.is(ACTIVE_MINING) || stack.is(LEFT_HOLD_TOOLS)
            || stack.getItem() instanceof DiggerItem || looksLikeLeftHoldTool(stack)) return Kind.ACTIVE_MINING;
        return Kind.REPAIR_ONLY;
    }

    public static boolean supportsMode(ItemStack stack) { return kind(stack) != Kind.REPAIR_ONLY; }
    public static boolean usesActiveToggle(ItemStack stack) { return kind(stack) == Kind.ACTIVE_MELEE; }
    public static boolean drainsWhileHeld(ItemStack stack) { return kind(stack) == Kind.ACTIVE_MELEE; }
    public static boolean usesMiningActivation(ItemStack stack) { return kind(stack) == Kind.ACTIVE_MINING; }
    public static boolean drainsWhileUsing(ItemStack stack) { return false; }

    private static boolean looksLikeLeftHoldTool(ItemStack stack) {
        if (!stack.isDamageableItem()) return false;
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem
            || item instanceof ShieldItem || item instanceof ElytraItem
            || stack.is(Items.FISHING_ROD) || stack.is(Items.FLINT_AND_STEEL)
            || stack.is(Items.TURTLE_HELMET)) return false;
        try {
            return stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) > 1.0F;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isMelee(ItemStack stack) { return kind(stack) == Kind.ACTIVE_MELEE; }
    public static boolean isMining(ItemStack stack) { return kind(stack) == Kind.ACTIVE_MINING; }
    public static boolean isElytra(ItemStack stack) { return kind(stack) == Kind.ELYTRA; }

    public static void registerBehavior(Predicate<ItemStack> predicate, Kind kind) {
        RULES.add(new Rule(Objects.requireNonNull(predicate), Objects.requireNonNull(kind)));
    }

    public static void registerEquippedStackProvider(BiConsumer<Player, Consumer<ItemStack>> provider) {
        EQUIPPED_PROVIDERS.add(Objects.requireNonNull(provider));
    }

    public static void collectExternalEquipped(Player player, Consumer<ItemStack> sink) {
        for (BiConsumer<Player, Consumer<ItemStack>> provider : EQUIPPED_PROVIDERS) {
            try {
                provider.accept(player, sink);
            } catch (RuntimeException ignored) {
                // Optional equipment integrations are isolated from the repair loop.
            }
        }
    }

    public static String registryId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static TagKey<Item> tag(String path) { return externalTag(VeloriseEMC.MODID, path); }

    private static TagKey<Item> externalTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private record Rule(Predicate<ItemStack> predicate, Kind kind) {}
}
