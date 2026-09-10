package com.velorise.emc;

import com.velorise.emc.common.WorldProgressionObjective;
import com.velorise.emc.common.WorldProgressionState;
import com.velorise.emc.common.WorldTierStage;
import com.velorise.emc.common.LastEchoProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Records player-credited kills for the active conquest stage and advances World Tier 0-IV. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = VeloriseEMC.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldProgressionEvents {
    private WorldProgressionEvents() {}

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        ServerPlayer player = creditedPlayer(event);
        if (player == null) return;

        MinecraftServer server = player.server;
        WorldProgressionData data = WorldProgressionData.get(server);
        WorldProgressionState before = data.snapshot();
        WorldTierStage activeStage = before.activeStage();
        if (activeStage == null) return;

        WorldProgressionObjective[] objectives = objectivesFor(event.getEntity(), activeStage);
        if (objectives.length == 0 || !data.record(player.getUUID(), objectives)) return;

        WorldProgressionState after = data.snapshot();
        announceCompletedObjectives(player, before, after, objectives);
        if (after.worldTier() > before.worldTier()) {
            announceTier(server, after.worldTier());
            ThreatScalingEvents.refreshLoadedMobs(server);
            ResonantOreRetrogen.enqueueAroundPlayers(server, 2);
            BossArenaProgressionService.ensureUnlockedArenas(server, after.worldTier());
            if (before.worldTier() < LastEchoProfile.REQUIRED_WORLD_TIER && after.worldTier() >= LastEchoProfile.REQUIRED_WORLD_TIER) {
                BlockPos citadel = LastEchoCitadelService.ensureLocated(server);
                if (citadel != null) {
                    // Construction is proximity-triggered and incrementally budgeted by
                    // LastEchoCitadelService; never place the complete citadel inline here.
                    broadcast(server, "§5The Citadel of the Last Echo now awaits in the outer End.");
                }
            }
        }
        syncProgress(server);
    }

    private static WorldProgressionObjective[] objectivesFor(LivingEntity defeated, WorldTierStage stage) {
        List<WorldProgressionObjective> result = new ArrayList<>(3);
        Level level = defeated.level();

        if (stage == WorldTierStage.OVERWORLD_HUNT) {
            if (level.dimension() != Level.OVERWORLD || !(defeated instanceof Monster)) return new WorldProgressionObjective[0];
            result.add(WorldProgressionObjective.OVERWORLD_TOTAL);
            if (defeated instanceof Zombie) result.add(WorldProgressionObjective.ZOMBIE_FAMILY);
            else if (defeated instanceof AbstractSkeleton) result.add(WorldProgressionObjective.SKELETON_FAMILY);
            else if (defeated instanceof Spider) result.add(WorldProgressionObjective.SPIDER_FAMILY);
            else if (defeated instanceof Creeper) result.add(WorldProgressionObjective.CREEPER);
            else if (defeated instanceof EnderMan) result.add(WorldProgressionObjective.ENDERMAN_OVERWORLD);
            else if (defeated instanceof Witch) result.add(WorldProgressionObjective.WITCH);
            else if (defeated instanceof AbstractIllager || defeated instanceof Ravager) result.add(WorldProgressionObjective.ILLAGER);
            return result.toArray(WorldProgressionObjective[]::new);
        }

        if (stage == WorldTierStage.NETHER_CONQUEST) {
            if (defeated instanceof InfernalCrucibleEntity) return new WorldProgressionObjective[]{WorldProgressionObjective.INFERNAL_CRUCIBLE};
            if (defeated instanceof WitherBoss) return new WorldProgressionObjective[]{WorldProgressionObjective.WITHER};
            if (level.dimension() != Level.NETHER) return new WorldProgressionObjective[0];
            if (!(defeated instanceof Monster) && !(defeated instanceof Ghast)) return new WorldProgressionObjective[0];
            result.add(WorldProgressionObjective.NETHER_TOTAL);
            if (defeated instanceof Piglin || defeated instanceof PiglinBrute) result.add(WorldProgressionObjective.PIGLIN_FAMILY);
            else if (defeated instanceof ZombifiedPiglin) result.add(WorldProgressionObjective.ZOMBIFIED_PIGLIN);
            else if (defeated instanceof Blaze) result.add(WorldProgressionObjective.BLAZE);
            else if (defeated instanceof MagmaCube) result.add(WorldProgressionObjective.MAGMA_CUBE);
            else if (defeated instanceof Ghast) result.add(WorldProgressionObjective.GHAST);
            else if (defeated instanceof WitherSkeleton) result.add(WorldProgressionObjective.WITHER_SKELETON);
            return result.toArray(WorldProgressionObjective[]::new);
        }

        if (stage == WorldTierStage.END_CONQUEST) {
            if (defeated instanceof AethericBulwarkEntity) return new WorldProgressionObjective[]{WorldProgressionObjective.AETHERIC_BULWARK};
            if (defeated instanceof EnderDragon) return new WorldProgressionObjective[]{WorldProgressionObjective.ENDER_DRAGON};
            if (level.dimension() != Level.END) return new WorldProgressionObjective[0];
            if (defeated instanceof EnderMan) return new WorldProgressionObjective[]{WorldProgressionObjective.ENDERMAN_END};
            if (defeated instanceof Shulker) return new WorldProgressionObjective[]{WorldProgressionObjective.SHULKER};
            return new WorldProgressionObjective[0];
        }

        if (stage == WorldTierStage.DEEP_WORLD_CONQUEST && level.dimension() == Level.OVERWORLD) {
            if (defeated instanceof DeepboundWardenEntity) return new WorldProgressionObjective[]{WorldProgressionObjective.DEEPBOUND_WARDEN};
            if (defeated instanceof ElderGuardian) {
                return new WorldProgressionObjective[]{WorldProgressionObjective.GUARDIAN, WorldProgressionObjective.ELDER_GUARDIAN};
            }
            if (defeated instanceof Guardian) return new WorldProgressionObjective[]{WorldProgressionObjective.GUARDIAN};
            if (defeated instanceof Warden) return new WorldProgressionObjective[]{WorldProgressionObjective.WARDEN};
        }
        return new WorldProgressionObjective[0];
    }

    private static ServerPlayer creditedPlayer(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        Entity direct = event.getSource().getEntity();
        if (direct instanceof ServerPlayer player) return player;
        return event.getEntity().getKillCredit() instanceof ServerPlayer credited ? credited : null;
    }

    private static void announceCompletedObjectives(ServerPlayer player, WorldProgressionState before,
                                                     WorldProgressionState after,
                                                     WorldProgressionObjective[] touched) {
        for (WorldProgressionObjective objective : touched) {
            if (!before.objectiveComplete(objective) && after.objectiveComplete(objective)) {
                player.sendSystemMessage(Component.literal("§6Velorise EMC: §aObjective complete: §e"
                    + objective.displayName() + "§7."));
            }
        }
    }

    private static void announceTier(MinecraftServer server, int worldTier) {
        String color = switch (worldTier) {
            case 1 -> "§a";
            case 2 -> "§6";
            case 3 -> "§5";
            default -> "§d";
        };
        broadcast(server, "§6Velorise EMC: " + color + "World Tier " + roman(worldTier)
            + " §7completed. Wireless Core " + roman(worldTier) + " is now unlocked.");
        broadcast(server, "§4World Threat has risen to Tier " + roman(worldTier)
            + ". §7Hostile creatures have grown stronger.");
    }

    private static String roman(int tier) {
        return switch (tier) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "I"; };
    }

    private static void syncProgress(MinecraftServer server) {
        for (ServerPlayer online : server.getPlayerList().getPlayers()) VeloriseEMC.syncPlayerData(online);
    }

    private static void broadcast(MinecraftServer server, String message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) player.sendSystemMessage(Component.literal(message));
    }
}
