package com.velorise.emc;

import com.velorise.emc.common.ArcaneWeaponProfile;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Full-set bonuses for the four Resonant armor progressions. */
public final class ResonantSetBonus {
    public static final float INFERNAL_INCOMING_DAMAGE_MULTIPLIER = 0.90F;
    public static final float AETHERIC_INCOMING_DAMAGE_MULTIPLIER = 0.80F;
    public static final float ECHOING_INCOMING_DAMAGE_MULTIPLIER = 0.70F;

    private ResonantSetBonus() {}

    public static boolean hasInfernal(Player player) {
        return hasSet(player,
            VeloriseEMC.INFERNAL_RESONANT_HELMET.get(), VeloriseEMC.INFERNAL_RESONANT_CHESTPLATE.get(),
            VeloriseEMC.INFERNAL_RESONANT_LEGGINGS.get(), VeloriseEMC.INFERNAL_RESONANT_BOOTS.get());
    }

    public static boolean hasAetheric(Player player) {
        return hasSet(player,
            VeloriseEMC.AETHERIC_RESONANT_HELMET.get(), VeloriseEMC.AETHERIC_RESONANT_CHESTPLATE.get(),
            VeloriseEMC.AETHERIC_RESONANT_LEGGINGS.get(), VeloriseEMC.AETHERIC_RESONANT_BOOTS.get());
    }

    public static boolean hasEchoing(Player player) {
        return hasEchobound(player) || hasEchoingResonant(player);
    }

    public static boolean hasEchoingResonant(Player player) {
        return hasSet(player,
            VeloriseEMC.ECHOING_RESONANT_HELMET.get(), VeloriseEMC.ECHOING_RESONANT_CHESTPLATE.get(),
            VeloriseEMC.ECHOING_RESONANT_LEGGINGS.get(), VeloriseEMC.ECHOING_RESONANT_BOOTS.get());
    }

    public static boolean hasEchobound(Player player) {
        return hasSet(player,
            VeloriseEMC.ECHOBOUND_HELMET.get(), VeloriseEMC.ECHOBOUND_CHESTPLATE.get(),
            VeloriseEMC.ECHOBOUND_LEGGINGS.get(), VeloriseEMC.ECHOBOUND_BOOTS.get());
    }

    /** Echoing is the universal end-game set; themed sets boost only their own weapon family. */
    public static float arcaneMultiplier(Player player, ArcaneWeaponProfile.Kind kind) {
        if (hasEchobound(player)) return 2.0F;
        if (hasEchoingResonant(player)) return kind == ArcaneWeaponProfile.Kind.ECHO ? 1.5F : 1.1F;
        if (hasAetheric(player) && (kind == ArcaneWeaponProfile.Kind.DRAGONFIRE
            || kind == ArcaneWeaponProfile.Kind.SHULKER)) return 1.1F;
        if (hasInfernal(player) && (kind == ArcaneWeaponProfile.Kind.INFERNO
            || kind == ArcaneWeaponProfile.Kind.GHASTFIRE
            || kind == ArcaneWeaponProfile.Kind.WITHER)) return 1.1F;
        return 1.0F;
    }

    public static double voidTranslocatorMultiplier(Player player) {
        return hasAetheric(player) || hasEchoing(player) ? 1.1D : 1.0D;
    }

    /** Applies one full-set reduction only; the strongest matching set wins. */
    public static float incomingDamageMultiplier(Player player) {
        if (hasEchobound(player)) return com.velorise.emc.common.LastEchoProfile.ECHOBOUND_INCOMING_DAMAGE_MULTIPLIER;
        if (hasEchoingResonant(player)) return ECHOING_INCOMING_DAMAGE_MULTIPLIER;
        if (hasAetheric(player)) return AETHERIC_INCOMING_DAMAGE_MULTIPLIER;
        if (hasInfernal(player)) return INFERNAL_INCOMING_DAMAGE_MULTIPLIER;
        return 1.0F;
    }

    private static boolean hasSet(Player player, Item helmet, Item chestplate, Item leggings, Item boots) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(helmet)
            && player.getItemBySlot(EquipmentSlot.CHEST).is(chestplate)
            && player.getItemBySlot(EquipmentSlot.LEGS).is(leggings)
            && player.getItemBySlot(EquipmentSlot.FEET).is(boots);
    }
}
