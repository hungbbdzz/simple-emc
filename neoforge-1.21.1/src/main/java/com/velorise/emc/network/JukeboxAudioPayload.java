package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-bound command for one positional Resonance Jukebox sound. */
public record JukeboxAudioPayload(BlockPos pos, Action action, ResourceLocation soundId, float volume)
    implements CustomPacketPayload {

    public enum Action {
        STOP,
        PLAY,
        VOLUME;

        private static Action byId(int id) {
            Action[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public static final Type<JukeboxAudioPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "jukebox_audio")
    );

    public static final StreamCodec<FriendlyByteBuf, JukeboxAudioPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeBlockPos(payload.pos());
            buffer.writeByte(payload.action().ordinal());
            if (payload.action() == Action.PLAY) {
                buffer.writeResourceLocation(payload.soundId());
                buffer.writeFloat(payload.volume());
            } else if (payload.action() == Action.VOLUME) {
                buffer.writeFloat(payload.volume());
            }
        },
        buffer -> {
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
    );

    public static JukeboxAudioPayload play(BlockPos pos, ResourceLocation soundId, float volume) {
        return new JukeboxAudioPayload(pos.immutable(), Action.PLAY, soundId, volume);
    }

    public static JukeboxAudioPayload stop(BlockPos pos) {
        return new JukeboxAudioPayload(pos.immutable(), Action.STOP, null, 0.0F);
    }

    public static JukeboxAudioPayload volume(BlockPos pos, float volume) {
        return new JukeboxAudioPayload(pos.immutable(), Action.VOLUME, null, volume);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
