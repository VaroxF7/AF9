package com.af9.core.machine;

import com.af9.core.common.AF9Sounds;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

import net.minecraft.sounds.SoundEvent;

/**
 * A machine with a glowing light ring while it works, like GT's Fusion Reactor
 * ({@link com.af9.core.client.render.LightRingRender}). Both methods run on the client, from synced state.
 */
public interface ILightRingMachine extends IMachineFeature {

    /** Whether the ring is lit now (it fades out over a few seconds after this turns false). */
    boolean isRingLit();

    /** Ring colour (RGB; the ring pulses between it and white). */
    int getRingColor();

    /** Played once when the ring lights up; null for none. */
    default SoundEvent ringIgniteSound() {
        return AF9Sounds.ORBITAL_IGNITE;
    }

    /** Played on every white flash of the ring; null for none. */
    default SoundEvent ringPulseSound() {
        return AF9Sounds.ORBITAL_PULSE;
    }
}
