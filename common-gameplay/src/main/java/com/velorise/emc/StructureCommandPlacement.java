package com.velorise.emc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

/** Shared placement offsets for operator structure-preview commands. */
public final class StructureCommandPlacement {
    private StructureCommandPlacement() {}

    /** Returns a block position horizontally in front of the command source. */
    public static BlockPos ahead(CommandSourceStack source, int distance) {
        double yawRadians = Math.toRadians(source.getRotation().y);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        return BlockPos.containing(source.getPosition().add(
            forwardX * Math.max(1, distance), 0.0D, forwardZ * Math.max(1, distance)));
    }
}
