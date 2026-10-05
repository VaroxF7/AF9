package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

/**
 * A lithography machine with a visible exposure chamber (the Mk1 line's stepper and the Mk2 scanner's tube):
 * a purple UV glow inside, a wafer on its stage, a laser that scans it die by die, and a robot arm on a slide
 * that loads the wafer and carries the printed one out of sight to the output.
 * <p>
 * The render ({@code LithoChamberRender}) follows the print's progress, so the laser finishes its scan when the
 * print does. All methods run on the client, from synced state.
 */
public interface ILithoChamberMachine extends IMachineFeature {

    /** Whether the chamber exists (the structure is formed). */
    boolean isChamberFormed();

    /** Whether the chamber is lit now (a print is running). */
    boolean isChamberLit();

    /** Chamber light colour (RGB): the active mode's colour. */
    int getChamberColor();

    /**
     * Print progress 0-1 for the laser scan and the arm cycle, {@code partialTick} for smoothness.
     * -1 when nothing is printing (the arm is parked, the wafer is gone, only a dim standby glow).
     */
    float getChamberProgress(float partialTick);
}
