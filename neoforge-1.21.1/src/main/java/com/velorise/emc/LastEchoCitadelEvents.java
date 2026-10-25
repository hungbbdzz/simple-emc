package com.velorise.emc;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = VeloriseEMC.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class LastEchoCitadelEvents {
    private LastEchoCitadelEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) EchoboundArmorFlight.update(player);
        EchoboundWeaponEffects.tick(event.getServer());
        ArenaSummonService.tick(event.getServer());
        BossArenaProgressionService.tick(event.getServer());
        LastEchoCitadelService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            event.setAmount(event.getAmount() * ResonantSetBonus.incomingDamageMultiplier(player));
        }
    }

    @SubscribeEvent
    public static void onArenaBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
            && (LastEchoCitadelService.isArenaProtected(level, event.getPos())
                || PromotionBossArenaService.isBulwarkAltarProtected(level, event.getPos())
                || PromotionBossArenaService.isDeepboundAltarProtected(level, event.getPos())
                || InfernalCrucibleArenaService.isInfernalAltarProtected(level, event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onArenaExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        // Preserve entity damage and knockback; only protected arena blocks are
        // removed from the explosion's destruction list.
        event.getAffectedBlocks().removeIf(pos -> LastEchoCitadelService.isArenaProtected(level, pos)
            || PromotionBossArenaService.isBulwarkAltarProtected(level, pos)
            || PromotionBossArenaService.isDeepboundAltarProtected(level, pos)
            || InfernalCrucibleArenaService.isInfernalAltarProtected(level, pos));
    }
}
