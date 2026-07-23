package com.velorise.emc;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Resonant sword with a tier-scaled hit-chain echo. */
public final class ResonantSwordItem extends SwordItem {
    private final int resonanceTier;

    public ResonantSwordItem(Tier tier, Properties properties, int resonanceTier) {
        super(tier, properties);
        this.resonanceTier = resonanceTier;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        ResonantSwordEffects.recordHit(attacker, target, resonanceTier);
    }
}
