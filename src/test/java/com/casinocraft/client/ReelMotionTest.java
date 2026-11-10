package com.casinocraft.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReelMotionTest {
    @Test
    void stopsExactlyOnResultAndNeverMovesBackwardsOrOvershoots() {
        assertEquals(0.4, ReelMotion.position(900, 1000, 2000, 0.4, 22), 1e-12);
        double previous = 0.4;
        for (long now = 1000; now <= 2100; now += 10) {
            double position = ReelMotion.position(now, 1000, 2000, 0.4, 22);
            assertTrue(position >= previous && position <= 22);
            previous = position;
        }
        assertEquals(22, previous);
        assertEquals(22, ReelMotion.position(2000, 1000, 2000, 0.4, 22));
    }
}
