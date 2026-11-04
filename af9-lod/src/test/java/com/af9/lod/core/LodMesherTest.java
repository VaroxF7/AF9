package com.af9.lod.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LodMesherTest {
    @Test void singleVoxelHasSixFaces() {
        LodGrid g = new LodGrid();
        g.set(5, 5, 5, 7);
        assertEquals(6, LodMesher.quadCount(new LodMesher().mesh(g, null)));
    }

    @Test void adjacentVoxelsHideSharedFaces() {
        LodGrid g = new LodGrid();
        g.set(5, 5, 5, 7);
        g.set(6, 5, 5, 7);
        assertEquals(10, LodMesher.quadCount(new LodMesher().mesh(g, null)));
    }

    @Test void neighbourGridHidesBorderFace() {
        LodGrid g = new LodGrid(), east = new LodGrid();
        g.set(15, 0, 0, 1);
        east.set(0, 0, 0, 1);
        LodGrid[] nb = new LodGrid[6];
        nb[1] = east;
        assertEquals(5, LodMesher.quadCount(new LodMesher().mesh(g, nb)));
    }

    @Test void packingRoundTrips() {
        LodGrid g = new LodGrid();
        g.set(3, 9, 12, 42);
        int[] m = new LodMesher().mesh(g, null);
        boolean found = false;
        for (int i = 0; i < m.length; i += 2) {
            int a = m[i];
            if ((a & 31) == 3 && (a >> 5 & 31) == 9 && (a >> 10 & 31) == 12 && (a >> 15) == 3) {
                found = true;
                assertEquals(42, m[i + 1]);
            }
        }
        assertTrue(found);
    }

    @Test void downsampleKeepsThinSolidAndPicksMajority() {
        LodGrid[] kids = new LodGrid[8];
        LodGrid k = new LodGrid();
        k.set(0, 0, 0, 5);
        k.set(1, 0, 0, 5);
        k.set(0, 1, 0, 9);
        kids[0] = k;
        LodGrid out = LodGrid.downsample(kids);
        assertEquals(5, out.get(0, 0, 0));
        assertEquals(0, out.get(1, 0, 0));
        assertTrue(LodGrid.downsample(new LodGrid[8]).isEmpty());
    }
}
