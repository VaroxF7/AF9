package com.af9.core.startup.layout;

/**
 * The Atmospheric Simulation Room's structure as aisles (no GregTech in here: plain characters). A 31 x 6 x 31 room (a
 * step past the cleanroom's 15 max), aisles back -> front (the controller's last), rows bottom -> top. The middle is open
 * air; stainless casing floor and ceiling, tempered glass one block in from the side walls, a closed back wall (aisle 0),
 * the controller ceiling-centre in the front wall, which has a 1 x 2 door front-bottom-centre. The cell function is the
 * one the script had ({@code simCell}); the preview page shows the same room 9 x 9.
 */
public final class AtmosphereLayout {

    private AtmosphereLayout() {}

    public static final int HEIGHT = 6;
    public static final int WIDTH = 31;
    public static final int DEPTH = 31;

    /** The block of the cell (x, y, z) of a w x d room: X casing, G glass, # air, S controller. */
    static char cell(int x, int y, int z, int w, int d) {
        int c = (w - 1) / 2;
        if (x == c && y == HEIGHT - 1 && z == d - 1) return 'S';
        if (y == 0 || y == HEIGHT - 1) return 'X';
        if (z == 0) {
            if (y == HEIGHT - 2) return 'X';
            if (x == 0 || x == w - 1) return 'X';
            if (x == 1 || x == w - 2) return 'G';
            return 'X';
        }
        if (z == d - 1) {
            if (y == 1 || y == 2) return x == c ? '#' : 'X';
            return 'X';
        }
        if (y == HEIGHT - 2) {
            if (x == 0 || x == w - 1) return 'X';
            return '#';
        }
        if (x == 0 || x == w - 1) return 'X';
        if (x == 1 || x == w - 2) return 'G';
        return '#';
    }

    /** Aisles front to back (the controller's last), each with its rows bottom to top. */
    public static String[][] aisles(int w, int d) {
        String[][] aisles = new String[d][HEIGHT];
        for (int z = 0; z < d; z++) {
            for (int y = 0; y < HEIGHT; y++) {
                StringBuilder row = new StringBuilder(w);
                for (int x = 0; x < w; x++) row.append(cell(x, y, z, w, d));
                aisles[z][y] = row.toString();
            }
        }
        return aisles;
    }
}
