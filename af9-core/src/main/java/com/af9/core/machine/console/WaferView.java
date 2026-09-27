package com.af9.core.machine.console;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The wafer the lithography consoles show being printed (the Orbital Lithography Station's and the Photolithography
 * Scanner's): a silicon disc with its flat seen from above, the die grid exposed die by die in serpentine order in the
 * node's colour, the scan slit crossing the die being exposed, and a progress ring round it with a glint.
 */
public final class WaferView {

    private final int radius;
    private final int die;
    private final int[][] dies;

    public WaferView(int radius, int die) {
        this.radius = radius;
        this.die = die;
        this.dies = dieOrder(radius, die);
    }

    public int radius() {
        return radius;
    }

    /**
     * Draws the wafer centred at (cx, cy) with {@code fraction} of the print done.
     *
     * @return the centre of the die being exposed, or null when nothing is exposed
     */
    @OnlyIn(Dist.CLIENT)
    public int[] draw(GuiGraphics graphics, int cx, int cy, double fraction, boolean running, boolean offline,
                      int color, long time) {
        for (int dy = -radius; dy <= radius - 3; dy++) {
            int half = (int) Math.sqrt((double) radius * radius - dy * dy);
            int shade = 0x1A2233 + ((radius - dy) / 12) * 0x020202;
            graphics.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0xFF000000 | shade);
        }
        int exposed = running ? (int) (fraction * dies.length) : 0;
        for (int i = 0; i < dies.length; i++) {
            int dx = cx + dies[i][0], dy = cy + dies[i][1];
            int fill;
            if (i < exposed) fill = ConsoleWidget.withAlpha(color, 0x70);
            else if (i == exposed && running) fill = ConsoleWidget.withAlpha(color, 0x90 + (int) (0x60 * pulse(time, 300)));
            else fill = offline ? 0x10FFFFFF : 0x18FFFFFF;
            graphics.fill(dx + 1, dy + 1, dx + die, dy + die, fill);
        }
        int[] current = null;
        if (running && exposed < dies.length) {
            // the scan slit crossing the die being exposed
            int dx = cx + dies[exposed][0], dy = cy + dies[exposed][1];
            double within = fraction * dies.length - exposed;
            int slit = dy + 1 + (int) (within * (die - 1));
            graphics.fill(dx, slit, dx + die + 1, slit + 1, 0xFFFFFFFF);
            current = new int[] { dx + die / 2, dy + die / 2 };
        }
        // progress ring, a glint running round it
        int ring = radius + 6;
        int dots = 96;
        for (int i = 0; i < dots; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / dots;
            int px = cx + (int) Math.round(Math.cos(angle) * ring), py = cy + (int) Math.round(Math.sin(angle) * ring);
            boolean lit = running && i < fraction * dots;
            graphics.fill(px - 1, py - 1, px + 1, py + 1, lit ? color : ConsoleWidget.TRACK);
        }
        if (running) {
            double angle = (time % 2000) / 2000.0 * 2 * Math.PI;
            int px = cx + (int) Math.round(Math.cos(angle) * ring), py = cy + (int) Math.round(Math.sin(angle) * ring);
            graphics.fill(px - 2, py - 2, px + 2, py + 2, ConsoleWidget.withAlpha(0xFFFFFF, 0xC0));
        }
        return current;
    }

    /** A soft beam of the node's colour from (fromX, fromY) down to the die being exposed. */
    @OnlyIn(Dist.CLIENT)
    public static void drawBeam(GuiGraphics graphics, int fromX, int fromY, int toX, int toY, int color, long time) {
        graphics.fill(fromX - 6, fromY - 2, fromX + 6, fromY, ConsoleWidget.withAlpha(color, 0xD0));
        int steps = Math.max(1, toY - fromY);
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int bx = Math.round(Mth.lerp(t, fromX, toX)), by = Math.round(Mth.lerp(t, fromY, toY));
            int alpha = (int) (0x30 + 0x40 * pulse(time + i * 20L, 400));
            graphics.fill(bx - 1, by, bx + 2, by + 1, ConsoleWidget.withAlpha(color, alpha));
            graphics.fill(bx, by, bx + 1, by + 1, ConsoleWidget.withAlpha(0xFFFFFF, alpha));
        }
    }

    /** 0 ... 1 ... 0 over the period. */
    public static float pulse(long time, int period) {
        return (float) (0.5 + 0.5 * Math.sin(time * 2 * Math.PI / period));
    }

    /** Die offsets from the wafer centre inside the disc (clear of the flat), in serpentine exposure order. */
    private static int[][] dieOrder(int radius, int die) {
        List<int[]> dies = new ArrayList<>();
        int cells = radius / die + 1;
        for (int row = -cells; row < cells; row++) {
            List<int[]> line = new ArrayList<>();
            for (int col = -cells; col < cells; col++) {
                int dx = col * die, dy = row * die;
                boolean inside = true;
                for (int[] corner : new int[][] { { dx, dy }, { dx + die, dy }, { dx, dy + die },
                        { dx + die, dy + die } }) {
                    if (corner[0] * corner[0] + corner[1] * corner[1] > (radius - 1) * (radius - 1)) inside = false;
                }
                if (dy + die > radius - 3) inside = false;
                if (inside) line.add(new int[] { dx, dy });
            }
            if ((row & 1) == 1) Collections.reverse(line);
            dies.addAll(line);
        }
        return dies.toArray(new int[0][]);
    }
}
