package com.velorise.emc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server manager retained for projectile clouds and temporary webs; tables no longer generate EMC. */
@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PassiveEmcGenerationService {
    private PassiveEmcGenerationService() {}

    public static void offer(ServerPlayer player, int coreLevel) {
        // Intentionally empty: neither Transmutation Table creates passive EMC.
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        DragonfireCloudManager.tick(server);
        TemporaryWebManager.tick(server);
        ArcaneAchievementTracker.tick(server);
    }
}
