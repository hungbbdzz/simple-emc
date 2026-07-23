package com.velorise.emc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server manager retained for projectile clouds and temporary webs; tables no longer generate EMC. */
@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PassiveEmcGenerationService {
    private PassiveEmcGenerationService() {}

    public static void offer(ServerPlayer player, int coreLevel) {
        // Intentionally empty: neither Transmutation Table creates passive EMC.
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        DragonfireCloudManager.tick(server);
        TemporaryWebManager.tick(server);
        ArcaneAchievementTracker.tick(server);
    }
}
