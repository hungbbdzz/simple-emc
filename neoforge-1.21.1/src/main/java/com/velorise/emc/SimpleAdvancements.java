package com.velorise.emc;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Loader-specific bridge for awarding programmatic advancement criteria. */
public final class SimpleAdvancements {
    private SimpleAdvancements() {}

    public static void award(ServerPlayer player, String path) {
        if (player == null || path == null || path.isBlank()) return;
        AdvancementHolder advancement = player.server.getAdvancements()
            .get(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, path));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
