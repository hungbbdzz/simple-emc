package com.velorise.emc;

import com.velorise.emc.common.ResonantEquipmentProfile;
import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/** NeoForge 1.21.1 tool and armor material definitions for Resonant equipment. */
public final class ResonantMaterials {
    private ResonantMaterials() {}

    private static final TagKey<Block> INCORRECT_FOR_VERDANT = BlockTags.create(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "incorrect_for_verdant_tool"));
    private static final TagKey<Block> INCORRECT_FOR_INFERNAL = BlockTags.create(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "incorrect_for_infernal_tool"));
    private static final TagKey<Block> INCORRECT_FOR_AETHERIC = BlockTags.create(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "incorrect_for_aetheric_tool"));
    private static final TagKey<Block> INCORRECT_FOR_ECHOING = BlockTags.create(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "incorrect_for_echoing_tool"));

    public static final Tier VERDANT_TOOLS = toolTier(1, () -> VeloriseEMC.VERDANT_RESONANT_INGOT.get());
    public static final Tier INFERNAL_TOOLS = toolTier(2, () -> VeloriseEMC.INFERNAL_RESONANT_INGOT.get());
    public static final Tier AETHERIC_TOOLS = toolTier(3, () -> VeloriseEMC.AETHERIC_RESONANT_INGOT.get());
    public static final Tier ECHOING_TOOLS = toolTier(4, () -> VeloriseEMC.ECHOING_RESONANT_INGOT.get());
    public static final Tier ECHOBOUND_TOOLS = echoboundToolTier();

    private static Tier toolTier(int tier, Supplier<Item> repairItem) {
        return new Tier() {
            @Override public int getUses() { return ResonantEquipmentProfile.toolDurability(tier); }
            @Override public float getSpeed() { return ResonantEquipmentProfile.toolSpeed(tier); }
            @Override public float getAttackDamageBonus() { return ResonantEquipmentProfile.attackDamageBonus(tier); }
            @Override public int getEnchantmentValue() { return ResonantEquipmentProfile.enchantability(tier); }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(repairItem.get()); }
            @Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
                return switch (tier) {
                    case 1 -> INCORRECT_FOR_VERDANT;
                    case 2 -> INCORRECT_FOR_INFERNAL;
                    case 3 -> INCORRECT_FOR_AETHERIC;
                    default -> INCORRECT_FOR_ECHOING;
                };
            }
        };
    }

    private static Tier echoboundToolTier() {
        return new Tier() {
            @Override public int getUses() { return LastEchoProfile.ECHOBOUND_TOOL_DURABILITY; }
            @Override public float getSpeed() { return LastEchoProfile.ECHOBOUND_TOOL_SPEED; }
            @Override public float getAttackDamageBonus() { return LastEchoProfile.ECHOBOUND_ATTACK_BONUS; }
            @Override public int getEnchantmentValue() { return LastEchoProfile.ECHOBOUND_ENCHANTABILITY; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get()); }
            @Override public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() { return INCORRECT_FOR_ECHOING; }
        };
    }

    public static ArmorMaterial armor(String name, int tier, Supplier<Item> repairItem) {
        return new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET, ResonantEquipmentProfile.defense(tier, 0));
                map.put(ArmorItem.Type.CHESTPLATE, ResonantEquipmentProfile.defense(tier, 1));
                map.put(ArmorItem.Type.LEGGINGS, ResonantEquipmentProfile.defense(tier, 2));
                map.put(ArmorItem.Type.BOOTS, ResonantEquipmentProfile.defense(tier, 3));
                map.put(ArmorItem.Type.BODY, ResonantEquipmentProfile.defense(tier, 1));
            }),
            ResonantEquipmentProfile.enchantability(tier),
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(repairItem.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, name))),
            ResonantEquipmentProfile.toughness(tier),
            ResonantEquipmentProfile.knockbackResistancePerPiece(tier)
        );
    }

    public static ArmorMaterial echoboundArmor() {
        return new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET, LastEchoProfile.ECHOBOUND_DEFENSE[0]);
                map.put(ArmorItem.Type.CHESTPLATE, LastEchoProfile.ECHOBOUND_DEFENSE[1]);
                map.put(ArmorItem.Type.LEGGINGS, LastEchoProfile.ECHOBOUND_DEFENSE[2]);
                map.put(ArmorItem.Type.BOOTS, LastEchoProfile.ECHOBOUND_DEFENSE[3]);
                map.put(ArmorItem.Type.BODY, LastEchoProfile.ECHOBOUND_DEFENSE[1]);
            }),
            LastEchoProfile.ECHOBOUND_ENCHANTABILITY,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(VeloriseEMC.ECHOBOUND_DRAGON_SCALE.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "echobound"))),
            LastEchoProfile.ECHOBOUND_TOUGHNESS,
            LastEchoProfile.ECHOBOUND_KNOCKBACK_PER_PIECE
        );
    }

    public static Item.Properties echoboundArmorProperties(ArmorItem.Type type) {
        return new Item.Properties().durability(LastEchoProfile.ECHOBOUND_DURABILITY[slot(type)]).fireResistant();
    }

    public static Item.Properties armorProperties(int tier, ArmorItem.Type type) {
        Item.Properties properties = new Item.Properties().durability(ResonantEquipmentProfile.armorDurability(tier, slot(type)));
        return ResonantEquipmentProfile.fireproof(tier) ? properties.fireResistant() : properties;
    }

    private static int slot(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
            case BODY -> 1;
        };
    }
}
