package com.velorise.emc;

import com.velorise.emc.common.CoreEquipmentProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/** Directly injects Core bonus armor and damage into Minecraft's native item attribute modifiers. */
@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ImbuedAttributeEvents {
    private static final ResourceLocation CORE_ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "imbued_core_armor");
    private static final ResourceLocation CORE_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "imbued_core_damage");

    private ImbuedAttributeEvents() {}

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!ImbuedEquipmentUtil.isImbued(stack) && !CoreTierData.hasTier(stack)) return;

        int tier = CoreTierData.get(stack).level();

        if (stack.getItem() instanceof ArmorItem armorItem) {
            EquipmentSlot slot = armorItem.getEquipmentSlot();
            double bonus = CoreEquipmentProfile.armorBonus(tier);
            if (bonus > 0) {
                event.addModifier(
                    Attributes.ARMOR,
                    new AttributeModifier(CORE_ARMOR_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(slot)
                );
            }
        } else if (ImbuedCompatibility.isElytra(stack)) {
            double bonus = CoreEquipmentProfile.elytraArmorBonus(tier);
            if (bonus > 0) {
                event.addModifier(
                    Attributes.ARMOR,
                    new AttributeModifier(CORE_ARMOR_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST
                );
            }
        } else if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem) {
            float bonus = CoreEquipmentProfile.weaponDamageBonus(tier);
            if (bonus > 0) {
                event.addModifier(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(CORE_DAMAGE_MODIFIER_ID, (double) bonus, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
                );
            }
        }
    }
}
