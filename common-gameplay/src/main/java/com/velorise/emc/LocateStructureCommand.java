package com.velorise.emc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Operator locate commands restricted to structures created by Velorise EMC. */
public final class LocateStructureCommand {
    private LocateStructureCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locate_structure")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("infernal_crucible")
                .executes(context -> locateInfernalCrucible(context.getSource())))
            .then(Commands.literal("aetheric_bulwark")
                .executes(context -> locateAethericBulwark(context.getSource())))
            .then(Commands.literal("deepbound_warden")
                .executes(context -> locateDeepboundWarden(context.getSource())))
            .then(Commands.literal("last_echo")
                .executes(context -> locateLastEcho(context.getSource())))
        );
    }

    public static int locateInfernalCrucible(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(Level.NETHER);
        if (level == null) return unavailable(source, "The Nether");
        BlockPos center = InfernalCrucibleArenaService.locateArena(level, new BlockPos(0, 70, 0));
        return report(source, "infernal_crucible", level, center);
    }

    public static int locateAethericBulwark(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(Level.END);
        if (level == null) return unavailable(source, "The End");
        BlockPos landmark = locateVanilla(level, "end_city", new BlockPos(1024, 80, 1024), 256);
        BlockPos center = landmark == null ? null : landmark.offset(48, 0, 0);
        return report(source, "aetheric_bulwark", level, center);
    }

    public static int locateDeepboundWarden(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        BlockPos landmark = locateVanilla(level, "ancient_city", level.getSharedSpawnPos(), 256);
        BlockPos center = landmark == null ? null : landmark.offset(48, 2, 0);
        return report(source, "deepbound_warden", level, center);
    }

    public static int locateLastEcho(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(Level.END);
        if (level == null) return unavailable(source, "The End");
        BlockPos center = LastEchoCitadelService.ensureLocated(source.getServer());
        if (center == null) {
            source.sendFailure(Component.literal("The Last Echo Citadel is not unlocked yet."));
            return 0;
        }
        return report(source, "last_echo", level, center);
    }

    private static BlockPos locateVanilla(ServerLevel level, String path, BlockPos origin, int radiusChunks) {
        ResourceLocation id = ResourceLocation.tryParse("minecraft:" + path);
        if (id == null) return null;
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        java.util.Optional<Holder.Reference<Structure>> holder = registry.getHolder(
            ResourceKey.create(Registries.STRUCTURE, id));
        if (holder.isEmpty()) return null;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(holder.get()), origin, radiusChunks, false);
        return found == null ? null : found.getFirst();
    }

    private static int report(CommandSourceStack source, String name, ServerLevel level, BlockPos target) {
        if (target == null) {
            source.sendFailure(Component.literal("Could not locate Velorise EMC structure: " + name));
            return 0;
        }
        BlockPos origin = BlockPos.containing(source.getPosition());
        int distance = level == source.getLevel()
            ? Math.round((float)Math.sqrt(origin.distSqr(target))) : -1;
        String dimension = level.dimension().location().toString();
        String suffix = distance >= 0 ? " (" + distance + " blocks away)" : "";
        source.sendSuccess(() -> Component.literal("§aNearest §e" + name + " §ais at §f"
            + target.getX() + ", " + target.getY() + ", " + target.getZ()
            + " §7in " + dimension + suffix), false);
        return 1;
    }

    private static int unavailable(CommandSourceStack source, String dimension) {
        source.sendFailure(Component.literal(dimension + " is unavailable."));
        return 0;
    }
}
