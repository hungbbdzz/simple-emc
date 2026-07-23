package com.velorise.emc;

import net.minecraft.resources.ResourceLocation;

/** NeoForge 1.21.1 ResourceLocation construction bridge. */
public final class ResourceLocationCompat {
    private ResourceLocationCompat() {}

    public static ResourceLocation minecraft(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
