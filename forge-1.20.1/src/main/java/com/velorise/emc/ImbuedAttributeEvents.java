package com.velorise.emc;

import com.velorise.emc.common.CoreEquipmentProfile;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Directly injects Core bonus armor and damage into Minecraft's native item attribute modifiers. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ImbuedAttributeEvents {
    private static final UUID CORE_ARMOR_UUID = UUID.nameUUIDFromBytes("velorise_emc:imbued_core_armor".getBytes());
    private static final UUID CORE_DAMAGE_UUID = UUID.nameUUIDFromBytes("velorise_emc:imbued_core_damage".getBytes());

    private ImbuedAttributeEvents() {}

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ImbuedEquipmentUtil.isImbued(stack) && !CoreTierData.hasTier(stack)) return;

        int tier = CoreTierData.get(stack).level();

        if (stack.getItem() instanceof ArmorItem armorItem) {
            EquipmentSlot slot = armorItem.getEquipmentSlot();
            if (event.getSlotType() == slot) {
                double bonus = CoreEquipmentProfile.armorBonus(tier);
                if (bonus > 0) {
                    event.addModifier(
                        Attributes.ARMOR,
                        new AttributeModifier(CORE_ARMOR_UUID, "Imbued Core Armor", bonus, AttributeModifier.Operation.ADDITION)
                    );
                }
            }
        } else if (ImbuedCompatibility.isElytra(stack)) {
            if (event.getSlotType() == EquipmentSlot.CHEST) {
                double bonus = CoreEquipmentProfile.elytraArmorBonus(tier);
                if (bonus > 0) {
                    event.addModifier(
                        Attributes.ARMOR,
                        new AttributeModifier(CORE_ARMOR_UUID, "Imbued Core Armor", bonus, AttributeModifier.Operation.ADDITION)
                    );
                }
            }
        } else if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem) {
            if (event.getSlotType() == EquipmentSlot.MAINHAND) {
                float bonus = CoreEquipmentProfile.weaponDamageBonus(tier);
                if (bonus > 0) {
                    event.addModifier(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(CORE_DAMAGE_UUID, "Imbued Core Damage", (double) bonus, AttributeModifier.Operation.ADDITION)
                    );
                }
            }
        }
    }
}
