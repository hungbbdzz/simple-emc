package com.velorise.emc;

import com.velorise.emc.common.EmcPolicyOverride;
import com.velorise.emc.common.EmcValueMode;
import com.velorise.emc.network.ModMessages;
import com.velorise.emc.network.RequestUpdateEMCMessage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class EMCConfigScreen extends AbstractEMCConfigScreen {
    public EMCConfigScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    protected void sendServerUpdate(Map<ResourceLocation, Long> emcOverrides,
                                    EmcValueMode valueMode,
                                    Map<ResourceLocation, EmcPolicyOverride> policyOverrides) {
        ModMessages.CHANNEL.sendToServer(
            new RequestUpdateEMCMessage(emcOverrides, true, valueMode, policyOverrides));
    }
}
