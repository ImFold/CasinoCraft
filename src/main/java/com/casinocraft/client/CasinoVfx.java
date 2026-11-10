package com.casinocraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Screen-local decoration. Uses elapsed time, never the game's outcome RNG. */
public final class CasinoVfx {
    private static final int MAX_PARTICLES = 64;
    private final List<Spark> sparks = new ArrayList<>();
    private final Random random = new Random();

    @FunctionalInterface
    public interface Canvas {
        void fill(int left, int top, int right, int bottom, int color);
    }

    private record Spark(double x, double y, double vx, double vy,
                         long born, int lifetime, int size, int color) { }

    public void clear() {
        sparks.clear();
    }

    public void expire(long now) {
        sparks.removeIf(spark -> now - spark.born >= spark.lifetime);
    }

    public void burst(int x, int y, int count, boolean bigWin, long now) {
        expire(now);
        int amount = Math.max(0, Math.min(count, MAX_PARTICLES));
        while (sparks.size() + amount > MAX_PARTICLES) {
            sparks.remove(0);
        }
        for (int i = 0; i < amount; i++) {
            double angle = -Math.PI + random.nextDouble() * Math.PI;
            double speed = (bigWin ? 28.0 : 18.0) + random.nextDouble() * 45.0;
            int color = i % 3 == 0 ? 0xFFF4CE : (i % 3 == 1 ? 0xFFC94A : 0xE89B32);
            sparks.add(new Spark(x + random.nextDouble() * 12.0 - 6.0, y,
                    Math.cos(angle) * speed, Math.sin(angle) * speed,
                    now, 650 + random.nextInt(bigWin ? 850 : 450), i % 4 == 0 ? 2 : 1, color));
        }
    }

    /** Bounds are in GUI coordinates and keep sparks off labels and controls. */
    public void render(Canvas canvas, long now, int left, int top, int right, int bottom) {
        expire(now);
        for (Spark spark : sparks) {
            long age = now - spark.born;
            if (age < 0) {
                continue;
            }
            double seconds = age / 1000.0;
            int x = (int) Math.round(spark.x + spark.vx * seconds);
            int y = (int) Math.round(spark.y + spark.vy * seconds + 28.0 * seconds * seconds);
            float fade = 1.0f - (float) age / spark.lifetime;
            clipped(canvas, x - 1, y - 1, x + spark.size + 1, y + spark.size + 1,
                    alpha(spark.color, fade * 0.18f), left, top, right, bottom);
            clipped(canvas, x, y, x + spark.size, y + spark.size,
                    alpha(spark.color, fade), left, top, right, bottom);
        }
    }

    public static float envelope(long now, long started, int duration) {
        if (started < 0L || now < started || duration <= 0) {
            return 0.0f;
        }
        float remaining = Math.max(0.0f, 1.0f - (float) (now - started) / duration);
        return remaining * remaining;
    }

    /** Soft, one-shot halo: no full-screen flashes or repeated blinking. */
    public static void glow(Canvas canvas, int x, int y, int w, int h, int color, float strength) {
        if (strength <= 0.0f) {
            return;
        }
        for (int ring = 3; ring >= 1; ring--) {
            int tint = alpha(color, strength * (4 - ring) * 0.10f);
            canvas.fill(x - ring, y - ring, x + w + ring, y - ring + 1, tint);
            canvas.fill(x - ring, y + h + ring - 1, x + w + ring, y + h + ring, tint);
            canvas.fill(x - ring, y, x - ring + 1, y + h, tint);
            canvas.fill(x + w + ring - 1, y, x + w + ring, y + h, tint);
        }
        int edge = alpha(color, strength * 0.85f);
        canvas.fill(x, y, x + w, y + 1, edge);
        canvas.fill(x, y + h - 1, x + w, y + h, edge);
        canvas.fill(x, y, x + 1, y + h, edge);
        canvas.fill(x + w - 1, y, x + w, y + h, edge);
    }

    private static int alpha(int rgb, float opacity) {
        int alpha = Math.round(Math.max(0.0f, Math.min(1.0f, opacity)) * 255.0f);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static void clipped(Canvas canvas, int x1, int y1, int x2, int y2, int color,
                                int left, int top, int right, int bottom) {
        int l = Math.max(x1, left);
        int t = Math.max(y1, top);
        int r = Math.min(x2, right);
        int b = Math.min(y2, bottom);
        if (l < r && t < b) {
            canvas.fill(l, t, r, b, color);
        }
    }
}
