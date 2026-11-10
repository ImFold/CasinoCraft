package com.casinocraft.client;

/** Deterministic, time-based coin arcs, clipped to the result panel. No per-frame particle allocation. */
public final class BigWinVfx {
    private BigWinVfx() { }

    public static int tint(int rgb, float opacity) {
        return (Math.round(Math.max(0.0f, Math.min(1.0f, opacity)) * 255.0f) << 24) | (rgb & 0xFFFFFF);
    }

    public static void draw(CasinoVfx.Canvas canvas, long elapsed, int requestedCoins, float opacity,
                            int left, int top, int right, int bottom, int color) {
        if (elapsed < 0L || opacity <= 0.0f || requestedCoins <= 0) {
            return;
        }
        int count = Math.min(64, requestedCoins);
        double center = (left + right) / 2.0;
        for (int i = 0; i < count; i++) {
            // A single staggered shower; no frame-dependent spawning and no endless emitter.
            long age = elapsed - (i % 16) * 45L;
            if (age < 0L || age >= 2000L) {
                continue;
            }
            double t = age / 1000.0;
            double spread = ((i * 37 % 101) - 50) / 50.0;
            double speed = 22.0 + i * 19 % 49;
            int x = (int) Math.round(center + spread * (12.0 + 42.0 * t));
            int y = (int) Math.round(bottom - 3 - speed * t + 32.0 * t * t);
            int halfWidth = Math.max(1, (int) Math.round(2.5 * Math.abs(Math.cos(t * 7.0 + i))));
            float fade = opacity * (float) Math.min(1.0, (2000L - age) / 450.0);
            fill(canvas, x - halfWidth - 1, y - 3, x + halfWidth + 2, y + 4,
                    tint(color, fade * 0.16f), left, top, right, bottom);
            fill(canvas, x - halfWidth, y - 2, x + halfWidth + 1, y + 3,
                    tint(0xBC771E, fade), left, top, right, bottom);
            fill(canvas, x - halfWidth, y - 2, x + halfWidth + 1, y + 1,
                    tint(0xFFD66B, fade), left, top, right, bottom);
            fill(canvas, x, y - 1, x + 1, y + 2,
                    tint(0xFFF3BD, fade), left, top, right, bottom);
        }
    }

    private static void fill(CasinoVfx.Canvas canvas, int l, int t, int r, int b, int color,
                             int left, int top, int right, int bottom) {
        l = Math.max(l, left);
        t = Math.max(t, top);
        r = Math.min(r, right);
        b = Math.min(b, bottom);
        if (l < r && t < b) {
            canvas.fill(l, t, r, b, color);
        }
    }
}
