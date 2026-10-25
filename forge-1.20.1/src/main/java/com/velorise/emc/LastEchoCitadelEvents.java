package com.velorise.emc;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LastEchoCitadelEvents {
    private LastEchoCitadelEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) EchoboundArmorFlight.update(player);
        EchoboundWeaponEffects.tick(event.getServer());
        ArenaSummonService.tick(event.getServer());
        BossArenaProgressionService.tick(event.getServer());
        LastEchoCitadelService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
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
