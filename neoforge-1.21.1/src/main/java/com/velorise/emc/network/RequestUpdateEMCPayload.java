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

public record RequestUpdateEMCPayload(
    Map<ResourceLocation, Long> overrides,
    boolean replaceAll,
    EmcValueMode valueMode,
    Map<ResourceLocation, EmcPolicyOverride> policyOverrides
) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 16_384;

    public RequestUpdateEMCPayload(Map<ResourceLocation, Long> overrides, EmcValueMode valueMode,
                                   Map<ResourceLocation, EmcPolicyOverride> policyOverrides) {
        this(overrides, false, valueMode, policyOverrides);
    }

    public RequestUpdateEMCPayload {
        overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        valueMode = valueMode == null ? EmcValueMode.DEFAULT : valueMode;
        policyOverrides = policyOverrides == null ? Map.of() : Map.copyOf(policyOverrides);
    }

    public static final Type<RequestUpdateEMCPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(VeloriseEMC.MODID, "request_update_emc")
    );

    public static final StreamCodec<FriendlyByteBuf, RequestUpdateEMCPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBoolean(payload.replaceAll());
            buf.writeVarInt(payload.valueMode().id());
            buf.writeVarInt(payload.overrides().size());
            payload.overrides().forEach((rl, val) -> {
                buf.writeResourceLocation(rl);
                buf.writeLong(val);
            });
            writePolicies(buf, payload.policyOverrides());
        },
        buf -> {
            boolean replaceAll = buf.readBoolean();
            EmcValueMode valueMode = EmcValueMode.byId(buf.readVarInt());
            int size = checkedSize(buf.readVarInt(), "EMC overrides");
            Map<ResourceLocation, Long> map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                map.put(buf.readResourceLocation(), buf.readLong());
            }
            return new RequestUpdateEMCPayload(map, replaceAll, valueMode, readPolicies(buf));
        }
    );

    static void writePolicies(FriendlyByteBuf buf, Map<ResourceLocation, EmcPolicyOverride> policies) {
        buf.writeVarInt(policies.size());
        policies.forEach((id, policy) -> {
            buf.writeResourceLocation(id);
            buf.writeByte(policy.burnableState());
            buf.writeByte(policy.learnableState());
            buf.writeByte(policy.materializableState());
        });
    }

    static Map<ResourceLocation, EmcPolicyOverride> readPolicies(FriendlyByteBuf buf) {
        int count = checkedSize(buf.readVarInt(), "EMC policies");
        Map<ResourceLocation, EmcPolicyOverride> policies = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            policies.put(id, new EmcPolicyOverride(buf.readByte(), buf.readByte(), buf.readByte()));
        }
        return policies;
    }

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
