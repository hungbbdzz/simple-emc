package com.velorise.emc.network;

import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcValueMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RequestUpdateEMCMessage {
    private static final int MAX_ENTRIES = 16_384;
    private final Map<ResourceLocation, Long> overrides;
    private final Map<ResourceLocation, EmcPolicyOverride> policyOverrides;
    private final boolean replaceAll;
    private final EmcValueMode valueMode;

    public RequestUpdateEMCMessage(Map<ResourceLocation, Long> overrides, EmcValueMode valueMode) {
        this(overrides, false, valueMode, Map.of());
    }

    public RequestUpdateEMCMessage(Map<ResourceLocation, Long> overrides, EmcValueMode valueMode,
                                   Map<ResourceLocation, EmcPolicyOverride> policyOverrides) {
        this(overrides, false, valueMode, policyOverrides);
    }

    public RequestUpdateEMCMessage(Map<ResourceLocation, Long> overrides, boolean replaceAll,
                                   EmcValueMode valueMode,
                                   Map<ResourceLocation, EmcPolicyOverride> policyOverrides) {
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        this.policyOverrides = policyOverrides == null ? Map.of() : Map.copyOf(policyOverrides);
        this.replaceAll = replaceAll;
        this.valueMode = valueMode == null ? EmcValueMode.DEFAULT : valueMode;
    }

    public Map<ResourceLocation, Long> getOverrides() { return overrides; }
    public Map<ResourceLocation, EmcPolicyOverride> getPolicyOverrides() { return policyOverrides; }
    public boolean isReplaceAll() { return replaceAll; }
    public EmcValueMode getValueMode() { return valueMode; }

    public static void encode(RequestUpdateEMCMessage msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.replaceAll);
        buf.writeVarInt(msg.valueMode.id());
        buf.writeVarInt(msg.overrides.size());
        msg.overrides.forEach((rl, val) -> {
            buf.writeResourceLocation(rl);
            buf.writeLong(val);
        });
        writePolicies(buf, msg.policyOverrides);
    }

    public static RequestUpdateEMCMessage decode(FriendlyByteBuf buf) {
        boolean replaceAll = buf.readBoolean();
        EmcValueMode valueMode = EmcValueMode.byId(buf.readVarInt());
        int size = checkedSize(buf.readVarInt(), "EMC overrides");
        Map<ResourceLocation, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readResourceLocation(), buf.readLong());
        }
        return new RequestUpdateEMCMessage(map, replaceAll, valueMode, readPolicies(buf));
    }

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

    public static void handle(RequestUpdateEMCMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ServerMessageHandler.handleUpdateEMC(msg, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }
}
