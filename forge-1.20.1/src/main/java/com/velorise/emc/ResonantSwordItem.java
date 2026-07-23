package com.velorise.emc;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Resonant sword with a tier-scaled hit-chain echo. */
public final class ResonantSwordItem extends SwordItem {
    private final int resonanceTier;

    public ResonantSwordItem(Tier tier, int attackDamage, float attackSpeed,
                             Properties properties, int resonanceTier) {
        super(tier, attackDamage, attackSpeed, properties);
        this.resonanceTier = resonanceTier;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) ResonantSwordEffects.recordHit(attacker, target, resonanceTier);
        return result;
    }
}
