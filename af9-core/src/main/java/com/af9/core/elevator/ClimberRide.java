package com.af9.core.elevator;

import net.minecraft.util.Mth;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * The rides of the Space Elevator's climber on its cable, as GTNH's climber makes them (gtnhintergalactic's
 * TileEntitySpaceElevatorCable): when the tower forms, the climber comes down from orbit ({@link #FORMATION}); while the
 * elevator is switched on it rides up to orbit, waits there and comes back, every {@link #DELIVERY_INTERVAL} ticks
 * ({@link #DELIVERY}). It moves a block a tick, slower over the first and the last {@link #EASE} blocks of the way, and
 * turns {@link #TURN} degrees a tick while a ride is on. Between rides it stands still.
 * <p>
 * A ride is a table of heights, one a tick, made once by playing it through: where the climber is depends only on the
 * ride and the ticks since it began, so the server sends the start and every client draws the same ride.
 */
public final class ClimberRide {

    public static final int NONE = 0, FORMATION = 1, DELIVERY = 2;

    /** How far above its rest the climber goes: "orbit" (blocks). */
    public static final float ORBIT = 250F;
    /** The blocks at both ends of the way over which it speeds up and slows down. */
    public static final float EASE = 30F;
    /** Degrees it turns a tick while a ride is on. */
    public static final float TURN = 0.5F;
    /** Ticks it waits in orbit on a delivery. */
    public static final int WAIT = 200;
    /** Ticks between two deliveries of an elevator that is switched on. */
    public static final int DELIVERY_INTERVAL = 2000;

    private static final float[] DOWN = down();
    private static final float[] UP_AND_DOWN = upAndDown();

    private ClimberRide() {}

    /** One tick's way at a height: a block, less near both ends (never less than a thirtieth of a block). */
    private static float step(float height, boolean nearOrbit) {
        if (height < EASE) return Math.max(height, 1F) / EASE;
        if (nearOrbit && height >= ORBIT - EASE) return Math.max(ORBIT - height, 1F) / EASE;
        return 1F;
    }

    /** From orbit down to the rest, without easing at the top: the climber appears there already moving. */
    private static float[] down() {
        FloatArrayList heights = new FloatArrayList();
        float height = ORBIT;
        heights.add(height);
        while (height > 0F) {
            height -= step(height, false);
            heights.add(Math.max(height, 0F));
        }
        return heights.toFloatArray();
    }

    /** Up to orbit, the wait, and down again. */
    private static float[] upAndDown() {
        FloatArrayList heights = new FloatArrayList();
        float height = 0F;
        heights.add(height);
        while (height < ORBIT) {
            height += step(height, true);
            heights.add(Math.min(height, ORBIT));
        }
        for (int tick = 0; tick < WAIT; tick++) heights.add(ORBIT);
        height = ORBIT;
        while (height > 0F) {
            height -= step(height, true);
            heights.add(Math.max(height, 0F));
        }
        return heights.toFloatArray();
    }

    private static float[] table(int ride) {
        return ride == FORMATION ? DOWN : ride == DELIVERY ? UP_AND_DOWN : null;
    }

    /** How many ticks a ride takes; 0 for no ride. */
    public static int duration(int ride) {
        float[] table = table(ride);
        return table == null ? 0 : table.length - 1;
    }

    /** The climber's height above its rest, {@code ticks} into a ride (0 once the ride is over). */
    public static float height(int ride, float ticks) {
        float[] table = table(ride);
        if (table == null || ticks >= table.length - 1) return 0F;
        if (ticks <= 0F) return table[0];
        int tick = (int) ticks;
        return Mth.lerp(ticks - tick, table[tick], table[tick + 1]);
    }

    /** The degrees the climber has turned since a ride began, {@code ticks} into it. */
    public static float turn(int ride, float ticks) {
        return TURN * Mth.clamp(ticks, 0F, duration(ride));
    }
}
