package com.velorise.emc;

import com.velorise.emc.common.CoreEquipmentProfile;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Aggregates Core armor into one transient player attribute without modifying the source item class. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ImbuedArmorEvents {
    private static final UUID MODIFIER_ID = UUID.fromString("908b2cb3-e548-48fa-b953-5ddf88d23b16");

    private ImbuedArmorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) return;
        refresh(event.player);
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
            MODIFIER_ID, "velorise_emc.core_armor", bonus, AttributeModifier.Operation.ADDITION));
    }
}
