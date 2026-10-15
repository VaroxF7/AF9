package com.af9.core.machine;

import com.af9.core.common.AF9Sounds;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

import net.minecraft.sounds.SoundEvent;

import java.util.List;

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

    /**
     * How hard the ring glows: 1 as GT's fusion ring; above 1 its glow is added light, that many times stronger, with
     * a fatter white-hot core (and, with Shimmer, blooming).
     */
    default float ringGlow() {
        return 1F;
    }

    /** Played where each big lightning bolt lands (a ring with lightning); null for none. */
    default SoundEvent ringArcSound() {
        return null;
    }

    /**
     * Ring radius in blocks: the model's radius by default. A machine with two sizes (the Orbital Array's basic and
     * extended patterns) overrides this to return the size it is formed as, so one model serves both rings.
     *
     * @param modelRadius the radius the model was built with
     */
    default float ringRadius(float modelRadius) {
        return modelRadius;
    }

    /**
     * One ring of the machine: how far behind the controller its centre lies, its radius, and its spin in radians per
     * tick (above 0 it wears habitat modules that turn round it like a space station's wheel, below 0 they turn the
     * other way; 0 for a plain ring).
     */
    record RingSpec(float back, float radius, float spin) {}

    /**
     * All the rings the machine draws, the first the main one (its sounds, sparks and lightning). By default one ring
     * with the model's place and the size from {@link #ringRadius(float)}, not spinning. The Orbital Array Mk2 has
     * three, turning against each other.
     *
     * @param modelBack   how far behind the controller the model puts its ring
     * @param modelRadius the radius the model was built with
     */
    default List<RingSpec> ringSpecs(float modelBack, float modelRadius) {
        return List.of(new RingSpec(modelBack, ringRadius(modelRadius), 0F));
    }
}
