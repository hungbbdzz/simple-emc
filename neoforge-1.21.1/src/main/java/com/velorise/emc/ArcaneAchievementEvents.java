package com.velorise.emc;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArcaneAchievementEvents {
    private ArcaneAchievementEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ArcaneAchievementTracker.handleDeath(event.getEntity(), event.getSource());
    }
}
