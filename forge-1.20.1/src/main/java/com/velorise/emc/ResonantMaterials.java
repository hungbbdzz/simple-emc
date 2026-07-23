package com.velorise.emc;

import com.velorise.emc.common.ResonantEquipmentProfile;
import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

/** Forge 1.20.1 tool and armor material definitions for Resonant equipment. */
public final class ResonantMaterials {
    private ResonantMaterials() {}

    public static final Tier VERDANT_TOOLS = TierSortingRegistry.registerTier(
        toolTier(1, () -> VeloriseEMC.VERDANT_RESONANT_INGOT.get()),
        new net.minecraft.resources.ResourceLocation(VeloriseEMC.MODID, "verdant_resonant"),
        List.of(Tiers.DIAMOND), List.of(Tiers.NETHERITE));
    public static final Tier INFERNAL_TOOLS = TierSortingRegistry.registerTier(
        toolTier(2, () -> VeloriseEMC.INFERNAL_RESONANT_INGOT.get()),
        new net.minecraft.resources.ResourceLocation(VeloriseEMC.MODID, "infernal_resonant"),
        List.of(Tiers.NETHERITE), List.of());
    public static final Tier AETHERIC_TOOLS = TierSortingRegistry.registerTier(
        toolTier(3, () -> VeloriseEMC.AETHERIC_RESONANT_INGOT.get()),
        new net.minecraft.resources.ResourceLocation(VeloriseEMC.MODID, "aetheric_resonant"),
        List.of(INFERNAL_TOOLS), List.of());
    public static final Tier ECHOING_TOOLS = TierSortingRegistry.registerTier(
        toolTier(4, () -> VeloriseEMC.ECHOING_RESONANT_INGOT.get()),
        new net.minecraft.resources.ResourceLocation(VeloriseEMC.MODID, "echoing_resonant"),
        List.of(AETHERIC_TOOLS), List.of());
    public static final Tier ECHOBOUND_TOOLS = TierSortingRegistry.registerTier(
        echoboundToolTier(),
        new net.minecraft.resources.ResourceLocation(VeloriseEMC.MODID, "echobound"),
        List.of(ECHOING_TOOLS), List.of());

    public static final ArmorMaterial VERDANT_ARMOR = armor("verdant_resonant", 1, () -> VeloriseEMC.VERDANT_RESONANT_INGOT.get());
    public static final ArmorMaterial INFERNAL_ARMOR = armor("infernal_resonant", 2, () -> VeloriseEMC.INFERNAL_RESONANT_INGOT.get());
    public static final ArmorMaterial AETHERIC_ARMOR = armor("aetheric_resonant", 3, () -> VeloriseEMC.AETHERIC_RESONANT_INGOT.get());
    public static final ArmorMaterial ECHOING_ARMOR = armor("echoing_resonant", 4, () -> VeloriseEMC.ECHOING_RESONANT_INGOT.get());
    public static final ArmorMaterial ECHOBOUND_ARMOR = echoboundArmor();

    private static Tier toolTier(int tier, Supplier<Item> repairItem) {
        return new Tier() {
            @Override public int getUses() { return ResonantEquipmentProfile.toolDurability(tier); }
            @Override public float getSpeed() { return ResonantEquipmentProfile.toolSpeed(tier); }
            @Override public float getAttackDamageBonus() { return ResonantEquipmentProfile.attackDamageBonus(tier); }
            @Override public int getLevel() { return tier == 1 ? 3 : 4; }
            @Override public int getEnchantmentValue() { return ResonantEquipmentProfile.enchantability(tier); }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(repairItem.get()); }
            @Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getTag() { return BlockTags.NEEDS_DIAMOND_TOOL; }
        };
    }

    private static Tier echoboundToolTier() {
        return new Tier() {
            @Override public int getUses() { return LastEchoProfile.ECHOBOUND_TOOL_DURABILITY; }
            @Override public float getSpeed() { return LastEchoProfile.ECHOBOUND_TOOL_SPEED; }
            @Override public float getAttackDamageBonus() { return LastEchoProfile.ECHOBOUND_ATTACK_BONUS; }
            @Override public int getLevel() { return 4; }
            @Override public int getEnchantmentValue() { return LastEchoProfile.ECHOBOUND_ENCHANTABILITY; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get()); }
            @Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getTag() { return BlockTags.NEEDS_DIAMOND_TOOL; }
        };
    }

    private static ArmorMaterial armor(String name, int tier, Supplier<Item> repairItem) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, ResonantEquipmentProfile.defense(tier, 0));
        defense.put(ArmorItem.Type.CHESTPLATE, ResonantEquipmentProfile.defense(tier, 1));
        defense.put(ArmorItem.Type.LEGGINGS, ResonantEquipmentProfile.defense(tier, 2));
        defense.put(ArmorItem.Type.BOOTS, ResonantEquipmentProfile.defense(tier, 3));
        return new ArmorMaterial() {
            @Override public int getDurabilityForType(ArmorItem.Type type) { return ResonantEquipmentProfile.armorDurability(tier, slot(type)); }
            @Override public int getDefenseForType(ArmorItem.Type type) { return defense.getOrDefault(type, 0); }
            @Override public int getEnchantmentValue() { return ResonantEquipmentProfile.enchantability(tier); }
            @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(repairItem.get()); }
            @Override public String getName() { return VeloriseEMC.MODID + ":" + name; }
            @Override public float getToughness() { return ResonantEquipmentProfile.toughness(tier); }
            @Override public float getKnockbackResistance() { return ResonantEquipmentProfile.knockbackResistancePerPiece(tier); }
        };
    }

    private static ArmorMaterial echoboundArmor() {
        return new ArmorMaterial() {
            @Override public int getDurabilityForType(ArmorItem.Type type) { return LastEchoProfile.ECHOBOUND_DURABILITY[slot(type)]; }
            @Override public int getDefenseForType(ArmorItem.Type type) { return LastEchoProfile.ECHOBOUND_DEFENSE[slot(type)]; }
            @Override public int getEnchantmentValue() { return LastEchoProfile.ECHOBOUND_ENCHANTABILITY; }
            @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get()); }
            @Override public String getName() { return VeloriseEMC.MODID + ":echobound"; }
            @Override public float getToughness() { return LastEchoProfile.ECHOBOUND_TOUGHNESS; }
            @Override public float getKnockbackResistance() { return LastEchoProfile.ECHOBOUND_KNOCKBACK_PER_PIECE; }
        };
    }

    private static int slot(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
    }
}
