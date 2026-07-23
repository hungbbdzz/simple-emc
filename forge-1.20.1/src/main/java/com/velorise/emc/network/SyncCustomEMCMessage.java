package com.velorise.emc.network;

import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcValueMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCustomEMCMessage {
    private static final int MAX_ENTRIES = 16_384;
    private final Map<ResourceLocation, Long> overrides;
    private final Map<ResourceLocation, EmcPolicyOverride> policyOverrides;
    private final EmcValueMode valueMode;

    public SyncCustomEMCMessage(Map<ResourceLocation, Long> overrides, EmcValueMode valueMode,
                                Map<ResourceLocation, EmcPolicyOverride> policyOverrides) {
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        this.policyOverrides = policyOverrides == null ? Map.of() : Map.copyOf(policyOverrides);
        this.valueMode = valueMode == null ? EmcValueMode.DEFAULT : valueMode;
    }

    public Map<ResourceLocation, Long> getOverrides() { return overrides; }
    public Map<ResourceLocation, EmcPolicyOverride> getPolicyOverrides() { return policyOverrides; }
    public EmcValueMode getValueMode() { return valueMode; }

    public static void encode(SyncCustomEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.valueMode.id());
        buf.writeVarInt(msg.overrides.size());
        msg.overrides.forEach((rl, val) -> {
            buf.writeResourceLocation(rl);
            buf.writeLong(val);
        });
        RequestUpdateEMCMessage.writePolicies(buf, msg.policyOverrides);
    }

    public static SyncCustomEMCMessage decode(FriendlyByteBuf buf) {
        EmcValueMode valueMode = EmcValueMode.byId(buf.readVarInt());
        int size = checkedSize(buf.readVarInt(), "EMC overrides");
        Map<ResourceLocation, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readResourceLocation(), buf.readLong());
        }
        return new SyncCustomEMCMessage(map, valueMode, RequestUpdateEMCMessage.readPolicies(buf));
    }

    private static int checkedSize(int size, String label) {
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException(label + " count out of range: " + size);
        }
        return size;
    }

    public static void handle(SyncCustomEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientMessageHandler.handleSyncCustomEMC(msg)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
