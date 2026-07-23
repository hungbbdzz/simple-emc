package com.velorise.emc;

import com.velorise.emc.common.WorldProgressionObjective;
import com.velorise.emc.common.WorldProgressionProfile;
import com.velorise.emc.common.WorldProgressionState;
import com.velorise.emc.common.WorldTierStage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

/** Read-only world progression command plus an operator reset for alpha testing. */
public final class WorldProgressCommands {
    private WorldProgressCommands() {}

    public static int show(CommandSourceStack source) {
        WorldProgressionState state = WorldProgressionData.get(source.getServer()).snapshot();
        WorldTierStage active = state.activeStage();
        source.sendSuccess(() -> Component.literal("Velorise EMC World Tier: " + state.worldTier() + " / 4"), false);
        if (active == null) {
            source.sendSuccess(() -> Component.literal("All World Tier objectives are complete. Core IV is unlocked."), false);
            return 1;
        }

        int contributors = state.contributorCount(active);
        String scale = String.format(Locale.ROOT, "%.2f", WorldProgressionProfile.scale(contributors));
        source.sendSuccess(() -> Component.literal(
            active.displayName() + " | Contributors: " + contributors + " | Scale: x" + scale), false);
        for (WorldProgressionObjective objective : WorldProgressionObjective.values()) {
            if (objective.stage() != active) continue;
            long required = state.required(objective);
            String status = required <= 0L || state.count(objective) >= required ? "[Complete] " : "[ ] ";
            source.sendSuccess(() -> Component.literal(status + objective.displayName() + ": "
                + state.count(objective) + " / " + required), false);
        }
        source.sendSuccess(() -> Component.literal(
            "Reward: unlock Core " + roman(active.unlockedWorldTier())), false);
        return 1;
    }

    public static int reset(CommandSourceStack source) {
        WorldProgressionData.get(source.getServer()).reset();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            VeloriseEMC.syncPlayerData(player);
        }
        source.sendSuccess(() -> Component.literal("Reset Velorise EMC World Tier progress."), true);
        return 1;
    }

    public static int grant(CommandSourceStack source, int tier) {
        var entity = source.getEntity();
        UUID contributor = entity == null ? new UUID(0L, 0L) : entity.getUUID();
        WorldProgressionData.get(source.getServer()).grantTier(contributor, tier);
        ResonantOreRetrogen.enqueueAroundPlayers(source.getServer(), 8);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) VeloriseEMC.syncPlayerData(player);
        if (tier >= 4) LastEchoCitadelService.ensureLocated(source.getServer());
        source.sendSuccess(() -> Component.literal("Granted Velorise EMC World Tier " + Math.max(0, Math.min(4, tier)) + "."), true);
        return 1;
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "I";
        };
    }
}
