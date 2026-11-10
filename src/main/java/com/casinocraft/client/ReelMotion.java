package com.casinocraft.client;

/** Elapsed-time cubic easing lands exactly on the server-selected symbol row. */
public final class ReelMotion {
    private ReelMotion() { }

    public static double position(long now, long started, long stopped, double from, int target) {
        if (now >= stopped || stopped <= started) {
            return target;
        }
        double progress = Math.max(0.0, (double) (now - started) / (stopped - started));
        double remaining = 1.0 - progress;
        return from + (target - from) * (1.0 - remaining * remaining * remaining);
    }
}
