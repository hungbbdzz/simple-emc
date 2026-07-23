package com.velorise.emc;

import com.velorise.emc.common.CoreEquipmentProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Aggregates Core armor into one transient player attribute without modifying the source item class. */
@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ImbuedArmorEvents {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "core_armor");

    private ImbuedArmorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        refresh(player);
    }

    private static void refresh(Player player) {
        double bonus = 0.0D;
        for (ItemStack stack : player.getArmorSlots()) {
            if (!ImbuedEquipmentUtil.isImbued(stack)) continue;
            int tier = CoreTierData.get(stack).level();
            if (ImbuedCompatibility.isElytra(stack)) bonus += CoreEquipmentProfile.elytraArmorBonus(tier);
            else bonus += CoreEquipmentProfile.armorBonus(tier);
        }
        var attribute = player.getAttribute(Attributes.ARMOR);
        if (attribute == null) return;
        attribute.removeModifier(MODIFIER_ID);
        if (bonus > 0.0D) attribute.addTransientModifier(new AttributeModifier(
            MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
    }
}
