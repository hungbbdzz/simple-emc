package com.velorise.emc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Operator-only alpha helpers. They intentionally bypass World Tier and normal proximity generation. */
public final class LastEchoAdminCommands {
    private LastEchoAdminCommands() {}

    public static int placeCitadel(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (!Level.END.equals(level.dimension())) {
            source.sendFailure(Component.literal("Citadel of the Last Echo can only be placed in the End."));
            return 0;
        }
        BlockPos center = StructureCommandPlacement.ahead(source, 80);
        int columns = LastEchoCitadelService.placeNow(level, center);
        source.sendSuccess(() -> Component.literal("Placed Citadel of the Last Echo at " + center.toShortString()
            + " (" + columns + " generated columns)."), true);
        return columns;
    }

    public static int summonBoss(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (!Level.END.equals(level.dimension())) {
            source.sendFailure(Component.literal("The Echobound Dragon can only be summoned in the End."));
            return 0;
        }
        BlockPos arena = BlockPos.containing(source.getPosition());
        EchoboundDragonEntity boss = LastEchoCitadelService.summonAt(level, arena);
        if (boss == null) {
            source.sendFailure(Component.literal("Could not create The Echobound Dragon."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Summoned The Echobound Dragon above " + arena.toShortString() + "."), true);
        return 1;
    }
}
