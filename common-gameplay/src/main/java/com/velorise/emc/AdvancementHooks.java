package com.velorise.emc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Small event hooks used by the advancement tree and hidden easter eggs.
 *
 * <p>The hooks only run from existing server-side transactions and pulses; they do not add
 * a background scanner or a new per-tick system.</p>
 */
public final class AdvancementHooks {
    private static final String LAST_BURN_ITEM = "VeloriseEMCLastBurnItem";
    private static final String LAST_BURN_TICK = "VeloriseEMCLastBurnTick";
    private static final String SIPHON_COUNT = "VeloriseEMCSiphonCount";
    private static final String SIPHON_LAST_TICK = "VeloriseEMCSiphonLastTick";
    private static final String CORE_SEQUENCE_PREFIX = "VeloriseEMCCoreSequence_";
    private static final String CORE_IV_ARCANE_MASK = "VeloriseEMCCoreIVArcaneMask";
    private static final String FURNACE_OUTPUT_TOTAL = "VeloriseEMCFurnaceOutputTotal";
    private static final String FOUNDRY_KIND_MASK = "VeloriseEMCFoundryKindMask";
    private static final String HOURGLASS_SPENT = "VeloriseEMCHourglassSpent";

    private AdvancementHooks() {}

    public static void onBurn(ServerPlayer player, Item item, PlayerEMC data) {
        if (player == null || item == null) return;
        SimpleAdvancements.award(player, "journey/first_exchange");
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id != null) {
            player.getPersistentData().putString(LAST_BURN_ITEM, id.toString());
            player.getPersistentData().putLong(LAST_BURN_TICK, player.level().getGameTime());
        }
        checkEmcMilestones(player, data);
    }

    public static void onMaterialize(ServerPlayer player, Item item, int count, PlayerEMC data) {
        if (player == null || item == null || count <= 0) return;
        SimpleAdvancements.award(player, "journey/first_materialization");
        if (data != null && data.getTotalMaterializedItems() >= 1_024L) {
            SimpleAdvancements.award(player, "journey/materialize_1024");
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String last = player.getPersistentData().getString(LAST_BURN_ITEM);
        long elapsed = player.level().getGameTime() - player.getPersistentData().getLong(LAST_BURN_TICK);
        if (id != null && id.toString().equals(last) && elapsed >= 0L && elapsed <= 600L) {
            SimpleAdvancements.award(player, "world/easter/questionable_investment");
        }
        checkEmcMilestones(player, data);
    }

    public static void checkEmcMilestones(ServerPlayer player, PlayerEMC data) {
        if (player == null || data == null) return;
        long emc = data.getEmc();
        if (emc >= 65_536L) SimpleAdvancements.award(player, "journey/emc_reserve");
        if (emc >= 1_048_576L) SimpleAdvancements.award(player, "journey/emc_millionaire");
    }

    public static void onSpent(ServerPlayer player, PlayerEMC data, long previousEmc) {
        if (player != null && data != null && previousEmc > 0L && data.getEmc() == 0L) {
            SimpleAdvancements.award(player, "world/easter/bankruptcy");
        }
    }

    public static void onFailedSpend(ServerPlayer player, long availableEmc) {
        if (player != null && availableEmc >= 1_048_576L) {
            SimpleAdvancements.award(player, "world/easter/not_rich_enough");
        }
    }

    /** Awards the personal Core-bound equipment or machine milestones for an anvil result. */
    public static void onCoreBoundResult(ServerPlayer player, ItemStack result) {
        if (player == null || result == null || result.isEmpty()
            || CoreTierData.isCoreItem(result) || !CoreTierData.hasTier(result)) return;

        int tier = CoreTierData.get(result).level();
        if (isCoreMachine(result)) {
            SimpleAdvancements.award(player, "engineering/machine_core_i");
            if (tier >= 4) SimpleAdvancements.award(player, "engineering/machine_core_iv");
        } else {
            SimpleAdvancements.award(player, "journey/equipment_core_i");
            if (tier >= 4) SimpleAdvancements.award(player, "journey/equipment_core_iv");
        }
        if (result.getItem() instanceof ArcaneWeaponItem && tier >= 4) {
            SimpleAdvancements.award(player, "journey/overqualified");
            trackCoreIvArcaneWeapon(player, result);
        }
        trackSequentialCoreUpgrade(player, result, tier);
        checkFullCoreIvArmor(player);
    }

    /** Checks the equipped armor slots only; mixed Vanilla and modded armor sets are valid. */
    public static void checkFullCoreIvArmor(ServerPlayer player) {
        if (player == null) return;
        int pieces = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            pieces++;
            if (armor.isEmpty() || !CoreTierData.hasTier(armor) || CoreTierData.get(armor).level() < 4) return;
        }
        if (pieces == 4) SimpleAdvancements.award(player, "journey/full_core_iv_armor");
    }

    /** Keeps global World Tier advancements aligned for current and late-joining players. */
    public static void syncWorldTierAdvancements(ServerPlayer player, int worldTier) {
        if (player == null) return;
        if (worldTier >= 1) SimpleAdvancements.award(player, "world/overworld_proven");
        if (worldTier >= 2) SimpleAdvancements.award(player, "world/trial_of_nether");
        if (worldTier >= 3) SimpleAdvancements.award(player, "world/end_answers");
        if (worldTier >= 4) {
            SimpleAdvancements.award(player, "world/silence_below");
            SimpleAdvancements.award(player, "world/one_boss_remaining");
        }
    }

    private static void trackSequentialCoreUpgrade(ServerPlayer player, ItemStack result, int tier) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (id == null) return;
        String key = CORE_SEQUENCE_PREFIX + id.toString().replace(':', '_').replace('/', '_');
        int previous = player.getPersistentData().getInt(key);
        boolean sequential = tier == 1 || previous == tier - 1
            || (tier == 2 && previous == 0 && result.getItem() instanceof ArcaneWeaponItem);
        player.getPersistentData().putInt(key, sequential ? tier : 0);
        if (tier >= 4 && sequential && previous == 3) {
            SimpleAdvancements.award(player, "journey/no_skipping_orientation");
        }
    }

    private static void trackCoreIvArcaneWeapon(ServerPlayer player, ItemStack result) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (id == null) return;
        String path = id.getPath();
        String[] weapons = {"dragonfire_scepter", "resonant_echo", "inferno_rod", "ghastfire_staff",
            "wither_scepter", "shulker_seeker", "guardian_lens", "evoker_fang_staff", "stormcaller",
            "frostbolt_wand", "webcaster"};
        int mask = player.getPersistentData().getInt(CORE_IV_ARCANE_MASK);
        for (int i = 0; i < weapons.length; i++) if (weapons[i].equals(path)) mask |= 1 << i;
        player.getPersistentData().putInt(CORE_IV_ARCANE_MASK, mask);
        if ((mask & ((1 << weapons.length) - 1)) == ((1 << weapons.length) - 1)) {
            SimpleAdvancements.award(player, "journey/full_promotion");
        }
    }

    public static void onResonantFurnaceTake(ServerPlayer player, AbstractResonantFurnaceBlockEntity furnace, ItemStack taken) {
        if (player == null || furnace == null || taken == null || taken.isEmpty()) return;
        long total = player.getPersistentData().getLong(FURNACE_OUTPUT_TOTAL) + taken.getCount();
        player.getPersistentData().putLong(FURNACE_OUTPUT_TOTAL, total);
        if (total >= 64L) SimpleAdvancements.award(player, "engineering/microwave_economics");
        if (furnace.getCoreTierLevel() >= 4) {
            if (taken.getCount() >= 16) {
                SimpleAdvancements.award(player, "engineering/not_a_duplication_bug");
            }
            int mask = player.getPersistentData().getInt(FOUNDRY_KIND_MASK) | (1 << furnace.getKind().ordinal());
            player.getPersistentData().putInt(FOUNDRY_KIND_MASK, mask);
            if ((mask & 7) == 7) SimpleAdvancements.award(player, "engineering/industrial_revolution_again");
        }
    }

    public static void onHourglassPulse(ServerPlayer player, long spent) {
        if (player == null || spent <= 0L) return;
        long total = player.getPersistentData().getLong(HOURGLASS_SPENT) + spent;
        player.getPersistentData().putLong(HOURGLASS_SPENT, total);
        if (total >= 4_096L) SimpleAdvancements.award(player, "engineering/paid_by_the_second");
    }

    private static boolean isCoreMachine(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem || PortableWorkstationItem.isPortableWorkstation(stack)) return true;
        return stack.is(VeloriseEMC.MATTERBURST_MINECART_ITEM.get());
    }

    /** Awards the dedicated Core IV vehicle challenge once actual movement reaches the configured maximum. */

    /** Preserves long-lived personal advancement counters across death clones. */
    public static void copyPlayerData(ServerPlayer original, ServerPlayer replacement) {
        if (original == null || replacement == null) return;
        String[] keys = {CORE_IV_ARCANE_MASK, FURNACE_OUTPUT_TOTAL, FOUNDRY_KIND_MASK, HOURGLASS_SPENT,
            "VeloriseEMCArcaneFieldTests", "VeloriseEMCPersonalMobKills"};
        for (String key : keys) {
            if (original.getPersistentData().contains(key)) {
                replacement.getPersistentData().put(key, original.getPersistentData().get(key).copy());
            }
        }
        for (String key : original.getPersistentData().getAllKeys()) {
            if (key.startsWith(CORE_SEQUENCE_PREFIX) && original.getPersistentData().get(key) != null) {
                replacement.getPersistentData().put(key, original.getPersistentData().get(key).copy());
            }
        }
    }

    public static void onSiphonPulse(ServerPlayer player, long gameTime, int pulseIntervalTicks) {
        if (player == null) return;
        SimpleAdvancements.award(player, "world/tower_hungers");
        long last = player.getPersistentData().getLong(SIPHON_LAST_TICK);
        int count = gameTime - last <= Math.max(2L, pulseIntervalTicks + 2L)
            ? player.getPersistentData().getInt(SIPHON_COUNT) + 1 : 1;
        player.getPersistentData().putLong(SIPHON_LAST_TICK, gameTime);
        player.getPersistentData().putInt(SIPHON_COUNT, count);
        int requiredPulses = Math.max(1, (15 * 20 + pulseIntervalTicks - 1) / pulseIntervalTicks);
        if (count >= requiredPulses && player.isAlive()) {
            SimpleAdvancements.award(player, "world/easter/this_seems_unsafe");
        }
    }
}
