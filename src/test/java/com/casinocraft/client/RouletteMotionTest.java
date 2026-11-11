package com.casinocraft.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RouletteMotionTest {
    @Test
    void freeRunAdvancesOneCellPerStep() {
        assertEquals(0.0, RouletteMotion.free(5000, 5000, 55.0), 1e-12);
        assertEquals(10.0, RouletteMotion.free(5550, 5000, 55.0), 1e-12);
        assertEquals(0.0, RouletteMotion.free(4000, 5000, 55.0), 1e-12);
    }

    @Test
    void landsExactlyOnTargetWithoutOvershootOrBacktracking() {
        double from = 3.4;
        double to = 41.0;
        long decel = 700L;
        long duration = RouletteMotion.duration(to - from, 1.0 / 55.0, decel);
        assertEquals(from, RouletteMotion.position(0L, 0L, decel, 1.0 / 55.0, from, to), 1e-12);

        double previous = from;
        for (long now = 0; now <= duration + 200; now += 5) {
            double position = RouletteMotion.position(now, 0L, decel, 1.0 / 55.0, from, to);
            assertTrue(position >= previous - 1e-9, "position must never move backwards");
            assertTrue(position <= to + 1e-9, "position must never overshoot");
            previous = position;
        }
        assertEquals(to, previous, 1e-9);
        assertEquals(to, RouletteMotion.position(duration + 5000, 0L, decel, 1.0 / 55.0, from, to), 1e-12);
    }

    @Test
    void velocityIsContinuousAtTheBrakingPoint() {
        double speed = 1.0 / 55.0;
        long decel = 700L;
        double cruise = RouletteMotion.cruiseMs(50.0, speed, decel);
        double before = RouletteMotion.position((long) (cruise - 1), 0L, decel, speed, 0.0, 50.0);
        double after = RouletteMotion.position((long) (cruise + 1), 0L, decel, speed, 0.0, 50.0);
        assertEquals(speed * 2.0, after - before, speed * 0.2);
    }

    @Test
    void landingTargetShowsTheNumberAndIsFarEnoughAhead() {
        for (int number = 0; number < 37; number++) {
            double target = RouletteMotion.landingTarget(0.0, number, 37, 16);
            assertEquals(number, Math.floorMod((long) target, 37));
            assertTrue(target >= 16.0, "wheel must travel a minimum distance");
            assertTrue(target < 16.0 + 37.0, "wheel must not spin an extra full cycle");

            double later = RouletteMotion.landingTarget(123.75, number, 37, 16);
            assertEquals(number, Math.floorMod((long) later, 37));
            assertTrue(later - 123.75 >= 16.0);
        }
    }
}
