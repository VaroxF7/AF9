package com.af9.core.bus;

import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The CWU/t each bus has carried this tick, against its {@link BusNetwork#MAX_CWUT}: every machine on a bus draws from
 * the same budget, and computation passing from one bus to another through a Bus Controller counts on both. Kept per
 * level by the bus's identity ({@link BusNetwork.Bus#id}); a new tick starts from zero.
 */
public final class BusLoad {

    /** Per level: bus id -> { game time, CWU/t used then }. */
    private static final Map<Level, Map<Long, long[]>> LOAD = new WeakHashMap<>();

    private BusLoad() {}

    /** The CWU/t the bus can still carry this tick. */
    public static int remaining(Level level, long bus) {
        long[] use = load(level).get(bus);
        if (use == null || use[0] != level.getGameTime()) return BusNetwork.MAX_CWUT;
        return (int) Math.max(0, BusNetwork.MAX_CWUT - use[1]);
    }

    /** Counts CWU/t the bus carried this tick. */
    public static void use(Level level, long bus, int cwut) {
        if (cwut <= 0) return;
        Map<Long, long[]> load = load(level);
        long now = level.getGameTime();
        if (load.size() > 256) load.values().removeIf(use -> use[0] < now - 20);
        long[] use = load.computeIfAbsent(bus, id -> new long[2]);
        if (use[0] != now) {
            use[0] = now;
            use[1] = 0;
        }
        use[1] += cwut;
    }

    private static Map<Long, long[]> load(Level level) {
        return LOAD.computeIfAbsent(level, l -> new HashMap<>());
    }
}
