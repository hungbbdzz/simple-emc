package com.velorise.emc;

import com.velorise.emc.common.WorldTierEquipmentProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Adds only stronger Vanilla equipment and legal Vanilla enchantments as World Tier rises. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldTierEquipmentEvents {
    private static final String APPLIED = "VeloriseEMCWorldTierEquipmentApplied";
    private static final Set<String> ELIGIBLE = Set.of(
        "zombie", "husk", "zombie_villager", "drowned",
        "skeleton", "stray", "wither_skeleton", "pillager",
        "piglin", "piglin_brute", "zombified_piglin"
    );
    private static final List<EquipmentSlot> SLOTS = List.of(
        EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
        EquipmentSlot.LEGS, EquipmentSlot.FEET
    );

    private WorldTierEquipmentEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.getTags().contains("velorise_emc.tower_guard_tier_1")) {
            applyTierOneTowerGuard(mob);
            return;
        }
        if (mob.getPersistentData().getBoolean(APPLIED)) return;
        mob.getPersistentData().putBoolean(APPLIED, true);

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!"minecraft".equals(entityId.getNamespace()) || !ELIGIBLE.contains(entityId.getPath())) return;

        int tier = WorldProgressionData.get(level.getServer()).snapshot().worldTier();
        if (tier <= 0) return;
        RandomSource random = mob.getRandom();
        if (random.nextFloat() >= WorldTierEquipmentProfile.equipmentChance(tier)) return;

        int attempts = 1 + random.nextInt(WorldTierEquipmentProfile.maxGeneratedSlots(tier));
        List<EquipmentSlot> shuffled = new ArrayList<>(SLOTS);
        java.util.Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
        for (int i = 0; i < attempts; i++) {
            EquipmentSlot slot = shuffled.get(i);
            if (slot == EquipmentSlot.MAINHAND) upgradeWeapon(mob, entityId.getPath(), tier, random);
            else upgradeArmor(mob, slot, tier, random);
        }
    }

    private static void upgradeWeapon(Mob mob, String entityId, int tier, RandomSource random) {
        ItemStack current = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!isVanillaOrEmpty(current)) return;
        ItemStack candidate = weaponFor(entityId, tier, random);
        if (candidate.isEmpty()) return;

        boolean stronger = current.isEmpty() || weaponRank(candidate.getItem()) > weaponRank(current.getItem());
        boolean sameWeapon = !current.isEmpty() && current.is(candidate.getItem());
        ItemStack result = stronger ? candidate : (sameWeapon ? current.copy() : ItemStack.EMPTY);
        if (result.isEmpty()) return;
        boolean enchanted = maybeEnchant(result, EquipmentSlot.MAINHAND, tier, random);
        if (!stronger && !enchanted) return;
        result.setCount(1);
        mob.setItemSlot(EquipmentSlot.MAINHAND, result);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static void upgradeArmor(Mob mob, EquipmentSlot slot, int tier, RandomSource random) {
        ItemStack current = mob.getItemBySlot(slot);
        if (!isVanillaOrEmpty(current)) return;
        ItemStack candidate = armorFor(slot, tier, random);
        if (candidate.isEmpty()) return;

        boolean stronger = current.isEmpty() || armorRank(candidate.getItem()) > armorRank(current.getItem());
        boolean sameArmor = !current.isEmpty() && current.is(candidate.getItem());
        ItemStack result = stronger ? candidate : (sameArmor ? current.copy() : ItemStack.EMPTY);
        if (result.isEmpty()) return;
        boolean enchanted = maybeEnchant(result, slot, tier, random);
        if (!stronger && !enchanted) return;
        result.setCount(1);
        mob.setItemSlot(slot, result);
        mob.setDropChance(slot, 0.0F);
    }

    private static ItemStack weaponFor(String entityId, int tier, RandomSource random) {
        if ("skeleton".equals(entityId) || "stray".equals(entityId)) return new ItemStack(Items.BOW);
        if ("pillager".equals(entityId)) return new ItemStack(Items.CROSSBOW);
        if ("piglin".equals(entityId) && random.nextFloat() < 0.45F) return new ItemStack(Items.CROSSBOW);
        if ("drowned".equals(entityId) && random.nextFloat() < 0.05F * tier) return new ItemStack(Items.TRIDENT);
        boolean axe = "piglin_brute".equals(entityId) || random.nextFloat() < 0.35F;
        return new ItemStack(axe ? axeForTier(tier, random) : swordForTier(tier, random));
    }

    private static ItemStack armorFor(EquipmentSlot slot, int tier, RandomSource random) {
        ArmorMaterial material = chooseArmorMaterial(tier, random);
        Item item = switch (slot) {
            case HEAD -> switch (material) {
                case GOLD -> Items.GOLDEN_HELMET; case CHAIN -> Items.CHAINMAIL_HELMET;
                case IRON -> Items.IRON_HELMET; case DIAMOND -> Items.DIAMOND_HELMET;
            };
            case CHEST -> switch (material) {
                case GOLD -> Items.GOLDEN_CHESTPLATE; case CHAIN -> Items.CHAINMAIL_CHESTPLATE;
                case IRON -> Items.IRON_CHESTPLATE; case DIAMOND -> Items.DIAMOND_CHESTPLATE;
            };
            case LEGS -> switch (material) {
                case GOLD -> Items.GOLDEN_LEGGINGS; case CHAIN -> Items.CHAINMAIL_LEGGINGS;
                case IRON -> Items.IRON_LEGGINGS; case DIAMOND -> Items.DIAMOND_LEGGINGS;
            };
            case FEET -> switch (material) {
                case GOLD -> Items.GOLDEN_BOOTS; case CHAIN -> Items.CHAINMAIL_BOOTS;
                case IRON -> Items.IRON_BOOTS; case DIAMOND -> Items.DIAMOND_BOOTS;
            };
            default -> Items.AIR;
        };
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ArmorMaterial chooseArmorMaterial(int tier, RandomSource random) {
        if (random.nextFloat() < WorldTierEquipmentProfile.diamondChance(tier)) return ArmorMaterial.DIAMOND;
        float roll = random.nextFloat();
        if (tier == 1) return roll < 0.35F ? ArmorMaterial.GOLD : (roll < 0.70F ? ArmorMaterial.CHAIN : ArmorMaterial.IRON);
        if (tier == 2) return roll < 0.18F ? ArmorMaterial.GOLD : (roll < 0.38F ? ArmorMaterial.CHAIN : ArmorMaterial.IRON);
        return roll < 0.06F ? ArmorMaterial.CHAIN : ArmorMaterial.IRON;
    }

    private static Item swordForTier(int tier, RandomSource random) {
        if (random.nextFloat() < WorldTierEquipmentProfile.diamondChance(tier)) return Items.DIAMOND_SWORD;
        if (tier == 1) return random.nextFloat() < 0.35F ? Items.GOLDEN_SWORD : (random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD);
        return random.nextFloat() < 0.12F ? Items.GOLDEN_SWORD : Items.IRON_SWORD;
    }

    private static Item axeForTier(int tier, RandomSource random) {
        if (random.nextFloat() < WorldTierEquipmentProfile.diamondChance(tier)) return Items.DIAMOND_AXE;
        if (tier == 1) return random.nextFloat() < 0.35F ? Items.GOLDEN_AXE : (random.nextBoolean() ? Items.STONE_AXE : Items.IRON_AXE);
        return random.nextFloat() < 0.12F ? Items.GOLDEN_AXE : Items.IRON_AXE;
    }

    private static boolean maybeEnchant(ItemStack stack, EquipmentSlot slot, int tier, RandomSource random) {
        if (random.nextFloat() >= WorldTierEquipmentProfile.enchantChance(tier)) return false;
        int level = WorldTierEquipmentProfile.enchantLevel(tier, random.nextFloat());
        if (level <= 0) return false;
        if (slot != EquipmentSlot.MAINHAND) stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, level);
        else if (stack.is(Items.BOW)) stack.enchant(Enchantments.POWER_ARROWS, level);
        else if (stack.is(Items.CROSSBOW)) stack.enchant(Enchantments.QUICK_CHARGE, Math.min(3, level));
        else if (stack.is(Items.TRIDENT)) stack.enchant(Enchantments.IMPALING, level);
        else stack.enchant(Enchantments.SHARPNESS, level);
        return true;
    }

    /** Tower guards are authored encounters, never replaced by the global random scaling kit. */
    private static void applyTierOneTowerGuard(Mob mob) {
        ItemStack weapon = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (weapon.is(Items.BOW)) weapon.enchant(Enchantments.POWER_ARROWS, 2);
        mob.getPersistentData().putBoolean(APPLIED, true);
    }

    private static boolean isVanillaOrEmpty(ItemStack stack) {
        return stack.isEmpty() || "minecraft".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    private static int armorRank(Item item) {
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) return 6;
        if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) return 5;
        if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) return 4;
        if (item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS) return 3;
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) return 2;
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) return 1;
        return 0;
    }

    private static int weaponRank(Item item) {
        if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_AXE) return 6;
        if (item == Items.DIAMOND_SWORD || item == Items.DIAMOND_AXE || item == Items.TRIDENT) return 5;
        if (item == Items.IRON_SWORD || item == Items.IRON_AXE) return 4;
        if (item == Items.STONE_SWORD || item == Items.STONE_AXE || item == Items.BOW || item == Items.CROSSBOW) return 3;
        if (item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE || item == Items.WOODEN_SWORD || item == Items.WOODEN_AXE) return 2;
        return 0;
    }

    private enum ArmorMaterial { GOLD, CHAIN, IRON, DIAMOND }
}
