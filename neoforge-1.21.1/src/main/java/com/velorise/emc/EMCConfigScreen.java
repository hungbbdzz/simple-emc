package com.velorise.emc;

import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcValueMode;
import com.velorise.emc.network.RequestUpdateEMCPayload;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class EMCConfigScreen extends AbstractEMCConfigScreen {
    public EMCConfigScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    protected void sendServerUpdate(Map<ResourceLocation, Long> emcOverrides,
                                    EmcValueMode valueMode,
                                    Map<ResourceLocation, EmcPolicyOverride> policyOverrides) {
        PacketDistributor.sendToServer(
            new RequestUpdateEMCPayload(emcOverrides, true, valueMode, policyOverrides));
    }
}
