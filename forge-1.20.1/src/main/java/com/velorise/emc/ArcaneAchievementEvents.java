package com.velorise.emc;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArcaneAchievementEvents {
    private ArcaneAchievementEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ArcaneAchievementTracker.handleDeath(event.getEntity(), event.getSource());
    }
}
