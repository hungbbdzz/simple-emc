package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncPlayerEMCPayload(long emc, List<ResourceLocation> learned, int sourceMode,
                                   long safetyThreshold, boolean hudEnabled, long inventoryFuelBuffer,
                                   long transmutationUsage, long transmutationUsageDecayMillis,
                                   long transmutationTaxRemainder, long totalMaterializedItems,
                                   long totalMaterializationTaxPaid, long[] worldProgressCounts,
                                   int[] worldProgressContributors)
    implements CustomPacketPayload {
    public static final Type<SyncPlayerEMCPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "sync_player_emc"));
    public static final StreamCodec<FriendlyByteBuf, SyncPlayerEMCPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeLong(payload.emc());
            buf.writeVarInt(payload.learned().size());
            for (ResourceLocation id : payload.learned()) buf.writeResourceLocation(id);
            buf.writeVarInt(payload.sourceMode());
            buf.writeLong(payload.safetyThreshold());
            buf.writeBoolean(payload.hudEnabled());
            buf.writeLong(payload.inventoryFuelBuffer());
            buf.writeLong(payload.transmutationUsage());
            buf.writeLong(payload.transmutationUsageDecayMillis());
            buf.writeLong(payload.transmutationTaxRemainder());
            buf.writeLong(payload.totalMaterializedItems());
            buf.writeLong(payload.totalMaterializationTaxPaid());
            buf.writeVarInt(payload.worldProgressCounts().length);
            for (long value : payload.worldProgressCounts()) buf.writeVarLong(value);
            buf.writeVarInt(payload.worldProgressContributors().length);
            for (int value : payload.worldProgressContributors()) buf.writeVarInt(value);
        },
        buf -> {
            long emc = buf.readLong();
            int size = buf.readVarInt();
            List<ResourceLocation> learned = new ArrayList<>(size);
            for (int i = 0; i < size; i++) learned.add(buf.readResourceLocation());
            int sourceMode = buf.readVarInt();
            long safetyThreshold = buf.readLong();
            boolean hudEnabled = buf.readBoolean();
            long inventoryFuelBuffer = buf.readLong();
            long transmutationUsage = buf.readLong();
            long transmutationUsageDecayMillis = buf.readLong();
            long transmutationTaxRemainder = buf.readLong();
            long totalMaterializedItems = buf.readLong();
            long totalMaterializationTaxPaid = buf.readLong();
            int countSize = buf.readVarInt();
            long[] worldProgressCounts = new long[countSize];
            for (int i = 0; i < countSize; i++) worldProgressCounts[i] = buf.readVarLong();
            int contributorSize = buf.readVarInt();
            int[] worldProgressContributors = new int[contributorSize];
            for (int i = 0; i < contributorSize; i++) worldProgressContributors[i] = buf.readVarInt();
            return new SyncPlayerEMCPayload(
                emc, learned, sourceMode, safetyThreshold, hudEnabled, inventoryFuelBuffer,
                transmutationUsage, transmutationUsageDecayMillis, transmutationTaxRemainder,
                totalMaterializedItems, totalMaterializationTaxPaid, worldProgressCounts,
                worldProgressContributors
            );
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
