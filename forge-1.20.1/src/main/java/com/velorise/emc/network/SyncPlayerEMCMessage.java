package com.velorise.emc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncPlayerEMCMessage {
    private final long emc;
    private final List<ResourceLocation> learned;
    private final int sourceMode;
    private final long safetyThreshold;
    private final boolean hudEnabled;
    private final long inventoryFuelBuffer;
    private final long transmutationUsage;
    private final long transmutationUsageDecayMillis;
    private final long transmutationTaxRemainder;
    private final long totalMaterializedItems;
    private final long totalMaterializationTaxPaid;
    private final long[] worldProgressCounts;
    private final int[] worldProgressContributors;

    public SyncPlayerEMCMessage(long emc, List<ResourceLocation> learned, int sourceMode,
                                long safetyThreshold, boolean hudEnabled, long inventoryFuelBuffer,
                                long transmutationUsage, long transmutationUsageDecayMillis,
                                long transmutationTaxRemainder, long totalMaterializedItems,
                                long totalMaterializationTaxPaid, long[] worldProgressCounts,
                                int[] worldProgressContributors) {
        this.emc = emc;
        this.learned = learned;
        this.sourceMode = sourceMode;
        this.safetyThreshold = safetyThreshold;
        this.hudEnabled = hudEnabled;
        this.inventoryFuelBuffer = inventoryFuelBuffer;
        this.transmutationUsage = transmutationUsage;
        this.transmutationUsageDecayMillis = transmutationUsageDecayMillis;
        this.transmutationTaxRemainder = transmutationTaxRemainder;
        this.totalMaterializedItems = totalMaterializedItems;
        this.totalMaterializationTaxPaid = totalMaterializationTaxPaid;
        this.worldProgressCounts = worldProgressCounts == null ? new long[0] : worldProgressCounts.clone();
        this.worldProgressContributors = worldProgressContributors == null ? new int[0] : worldProgressContributors.clone();
    }

    public long emc() { return emc; }
    public List<ResourceLocation> learned() { return learned; }
    public int sourceMode() { return sourceMode; }
    public long safetyThreshold() { return safetyThreshold; }
    public boolean hudEnabled() { return hudEnabled; }
    public long inventoryFuelBuffer() { return inventoryFuelBuffer; }
    public long transmutationUsage() { return transmutationUsage; }
    public long transmutationUsageDecayMillis() { return transmutationUsageDecayMillis; }
    public long transmutationTaxRemainder() { return transmutationTaxRemainder; }
    public long totalMaterializedItems() { return totalMaterializedItems; }
    public long totalMaterializationTaxPaid() { return totalMaterializationTaxPaid; }
    public long[] worldProgressCounts() { return worldProgressCounts.clone(); }
    public int[] worldProgressContributors() { return worldProgressContributors.clone(); }

    public static void encode(SyncPlayerEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.emc);
        buf.writeVarInt(msg.learned.size());
        for (ResourceLocation id : msg.learned) buf.writeResourceLocation(id);
        buf.writeVarInt(msg.sourceMode);
        buf.writeLong(msg.safetyThreshold);
        buf.writeBoolean(msg.hudEnabled);
        buf.writeLong(msg.inventoryFuelBuffer);
        buf.writeLong(msg.transmutationUsage);
        buf.writeLong(msg.transmutationUsageDecayMillis);
        buf.writeLong(msg.transmutationTaxRemainder);
        buf.writeLong(msg.totalMaterializedItems);
        buf.writeLong(msg.totalMaterializationTaxPaid);
        buf.writeVarInt(msg.worldProgressCounts.length);
        for (long value : msg.worldProgressCounts) buf.writeVarLong(value);
        buf.writeVarInt(msg.worldProgressContributors.length);
        for (int value : msg.worldProgressContributors) buf.writeVarInt(value);
    }

    public static SyncPlayerEMCMessage decode(FriendlyByteBuf buf) {
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
        return new SyncPlayerEMCMessage(
            emc, learned, sourceMode, safetyThreshold, hudEnabled, inventoryFuelBuffer,
            transmutationUsage, transmutationUsageDecayMillis, transmutationTaxRemainder,
            totalMaterializedItems, totalMaterializationTaxPaid, worldProgressCounts,
            worldProgressContributors
        );
    }

    public static void handle(SyncPlayerEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientMessageHandler.handleSyncPlayerEMC(msg)));
        ctx.get().setPacketHandled(true);
    }
}
