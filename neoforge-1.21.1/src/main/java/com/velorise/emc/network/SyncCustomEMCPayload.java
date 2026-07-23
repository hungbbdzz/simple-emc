package com.velorise.emc.network;

import com.velorise.emc.VeloriseEMC;
import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcValueMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncCustomEMCPayload(
    Map<ResourceLocation, Long> overrides,
    EmcValueMode valueMode,
    Map<ResourceLocation, EmcPolicyOverride> policyOverrides
) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 16_384;

    public SyncCustomEMCPayload {
        overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        valueMode = valueMode == null ? EmcValueMode.DEFAULT : valueMode;
        policyOverrides = policyOverrides == null ? Map.of() : Map.copyOf(policyOverrides);
    }

    public static final Type<SyncCustomEMCPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "sync_custom_emc")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncCustomEMCPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.valueMode().id());
            buf.writeVarInt(payload.overrides().size());
            payload.overrides().forEach((rl, val) -> {
                buf.writeResourceLocation(rl);
                buf.writeLong(val);
            });
            RequestUpdateEMCPayload.writePolicies(buf, payload.policyOverrides());
        },
        buf -> {
            EmcValueMode valueMode = EmcValueMode.byId(buf.readVarInt());
            int size = checkedSize(buf.readVarInt(), "EMC overrides");
            Map<ResourceLocation, Long> map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                map.put(buf.readResourceLocation(), buf.readLong());
            }
            return new SyncCustomEMCPayload(map, valueMode, RequestUpdateEMCPayload.readPolicies(buf));
        }
    );

    private static int checkedSize(int size, String label) {
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException(label + " count out of range: " + size);
        }
        return size;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
