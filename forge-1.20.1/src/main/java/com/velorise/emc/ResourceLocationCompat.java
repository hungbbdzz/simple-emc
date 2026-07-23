package com.velorise.emc;

import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1 ResourceLocation construction bridge. */
public final class ResourceLocationCompat {
    private ResourceLocationCompat() {}

    public static ResourceLocation minecraft(String path) {
        return new ResourceLocation("minecraft", path);
    }
}
