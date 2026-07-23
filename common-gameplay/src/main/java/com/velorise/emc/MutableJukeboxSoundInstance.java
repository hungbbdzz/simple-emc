package com.velorise.emc;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Positional record sound whose volume can be changed without restarting playback. */
public final class MutableJukeboxSoundInstance extends AbstractTickableSoundInstance {
    public MutableJukeboxSoundInstance(SoundEvent sound, BlockPos pos, float volume) {
        super(sound, SoundSource.RECORDS, RandomSource.create());
        this.volume = Mth.clamp(volume, 0.0F, 1.0F);
        this.pitch = 1.0F;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    public void setPlaybackVolume(float volume) {
        this.volume = Mth.clamp(volume, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        // The underlying record sound ends naturally. Tickability is required so
        // SoundEngine reapplies the mutable volume to the active channel.
    }
}
