package com.velorise.emc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-bound command for one positional Resonance Jukebox sound. */
public record JukeboxAudioMessage(BlockPos pos, Action action, ResourceLocation soundId, float volume) {
    public enum Action {
        STOP,
        PLAY,
        VOLUME;

        private static Action byId(int id) {
            Action[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public static JukeboxAudioMessage play(BlockPos pos, ResourceLocation soundId, float volume) {
        return new JukeboxAudioMessage(pos.immutable(), Action.PLAY, soundId, volume);
    }

    public static JukeboxAudioMessage stop(BlockPos pos) {
        return new JukeboxAudioMessage(pos.immutable(), Action.STOP, null, 0.0F);
    }

    public static JukeboxAudioMessage volume(BlockPos pos, float volume) {
        return new JukeboxAudioMessage(pos.immutable(), Action.VOLUME, null, volume);
    }

    public static void encode(JukeboxAudioMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos());
        buffer.writeByte(message.action().ordinal());
        if (message.action() == Action.PLAY) {
            buffer.writeResourceLocation(message.soundId());
            buffer.writeFloat(message.volume());
        } else if (message.action() == Action.VOLUME) {
            buffer.writeFloat(message.volume());
        }
    }

    public static JukeboxAudioMessage decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Action action = Action.byId(buffer.readUnsignedByte());
        if (action == Action.PLAY) {
            return play(pos, buffer.readResourceLocation(), buffer.readFloat());
        }
        if (action == Action.VOLUME) {
            return volume(pos, buffer.readFloat());
        }
        return stop(pos);
    }

    public static void handle(JukeboxAudioMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> ClientMessageHandler.handleJukeboxAudio(message)
        ));
        context.setPacketHandled(true);
    }
}
