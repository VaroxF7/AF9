package com.af9.lod.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LodLevelTest {
    @Test void cellSizeDoubles() {
        assertEquals(16, LodLevel.cellSize(0));
        assertEquals(512, LodLevel.cellSize(5));
        assertThrows(IllegalArgumentException.class, () -> LodLevel.cellSize(6));
    }

    @Test void levelGrowsWithDistance() {
        assertEquals(0, LodLevel.forDistance(100, 8));
        assertTrue(LodLevel.forDistance(2000, 8) > LodLevel.forDistance(500, 8));
        assertEquals(LodLevel.MAX_LEVEL, LodLevel.forDistance(1e9, 8));
    }

    @Test void negativeCellIndex() {
        assertEquals(-1, LodLevel.cellIndex(-1, 0));
        assertEquals(-1, LodLevel.cellIndex(-16, 0));
        assertEquals(-2, LodLevel.cellIndex(-17, 0));
        assertEquals(1, LodLevel.cellIndex(32, 1));
    }
}
