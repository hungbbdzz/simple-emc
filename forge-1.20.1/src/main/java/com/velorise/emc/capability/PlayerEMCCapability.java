package com.velorise.emc.capability;

import com.velorise.emc.PlayerEMC;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class PlayerEMCCapability {
    public static final Capability<PlayerEMC> PLAYER_EMC_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerEMC.class);
    }
}
