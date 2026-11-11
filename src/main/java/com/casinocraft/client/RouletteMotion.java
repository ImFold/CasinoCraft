package com.casinocraft.client;

/**
 * Motion of the roulette number strip: a steady run followed by a linear deceleration
 * that comes to rest exactly on the server-selected number.
 *
 * <p>Positions are measured in cells ("numbers"). One unit equals one cell, so the
 * fractional part is the pixel offset used when drawing the strip.</p>
 */
public final class RouletteMotion {
    private RouletteMotion() { }

    /** Position while waiting for the server result: one cell every {@code stepMs} milliseconds. */
    public static double free(long now, long started, double stepMs) {
        if (stepMs <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, (now - started) / stepMs);
    }

    /** Milliseconds of constant-speed travel before braking so the strip stops after {@code decelMs}. */
    public static double cruiseMs(double distance, double speedPerMs, long decelMs) {
        if (speedPerMs <= 0.0) {
            return 0.0;
        }
        double brakingDistance = speedPerMs * decelMs / 2.0;
        return Math.max(0.0, (distance - brakingDistance) / speedPerMs);
    }

    /** Total travel time in milliseconds for the given distance. */
    public static long duration(double distance, double speedPerMs, long decelMs) {
        return Math.round(cruiseMs(distance, speedPerMs, decelMs) + decelMs);
    }

    /**
     * Position at {@code now}. Constant speed until the braking point, then a linear ramp
     * down to zero, so the velocity never jumps and the strip lands exactly on {@code to}.
     */
    public static double position(long now, long started, long decelMs, double speedPerMs,
                                  double from, double to) {
        if (to <= from) {
            return to;
        }
        double cruise = cruiseMs(to - from, speedPerMs, decelMs);
        double elapsed = now - started;
        if (elapsed >= cruise + decelMs) {
            return to;
        }
        if (elapsed <= cruise) {
            return from + speedPerMs * elapsed;
        }
        double u = (elapsed - cruise) / decelMs;
        return from + speedPerMs * cruise + speedPerMs * decelMs * (u - u * u / 2.0);
    }

    /**
     * First position at least {@code minDistance} cells ahead of {@code current} that shows
     * {@code number} in the centre window.
     */
    public static double landingTarget(double current, int number, int cycle, int minDistance) {
        long base = (long) Math.ceil(current);
        long offset = Math.floorMod(number - Math.floorMod(base, cycle), cycle);
        double end = base + offset;
        while (end - current < minDistance) {
            end += cycle;
        }
        return end;
    }
}
