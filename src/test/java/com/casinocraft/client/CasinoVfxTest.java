package com.casinocraft.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CasinoVfxTest {
    private record Fill(int left, int top, int right, int bottom, int color) { }

    private static List<Fill> render(CasinoVfx effects, long now) {
        List<Fill> fills = new ArrayList<>();
        effects.render((l, t, r, b, color) -> fills.add(new Fill(l, t, r, b, color)),
                now, -1000, -1000, 1000, 1000);
        return fills;
    }

    @Test
    void particlesUseElapsedTimeAndExpireWithoutRendering() {
        CasinoVfx effects = new CasinoVfx();
        effects.burst(200, 100, 24, true, 1000L);
        List<Fill> frame = render(effects, 1300L);
        assertFalse(frame.isEmpty());
        assertEquals(frame, render(effects, 1300L));
        effects.expire(3000L);
        assertTrue(render(effects, 3000L).isEmpty());
    }

    @Test
    void repeatedBurstsRemainBoundedAndClearForTheNextSpin() {
        CasinoVfx effects = new CasinoVfx();
        for (int i = 0; i < 10; i++) {
            effects.burst(200, 100, 1000, true, 1000L);
        }
        assertEquals(128, render(effects, 1000L).size()); // Two fills per spark; at most 64 sparks.
        effects.clear();
        assertTrue(render(effects, 1000L).isEmpty());
        effects.burst(200, 100, 0, false, 1000L);
        assertTrue(render(effects, 1000L).isEmpty());
    }

    @Test
    void particlesCannotDrawOverControlsOutsideTheirBounds() {
        CasinoVfx effects = new CasinoVfx();
        effects.burst(243, 111, 64, true, 1000L);
        for (long now = 1000; now < 2600; now += 40) {
            effects.render((l, t, r, b, color) -> {
                assertTrue(l >= 155 && r <= 333 && t >= 65 && b <= 116);
                assertTrue(l < r && t < b);
            }, now, 155, 65, 333, 116);
        }
    }

    @Test
    void glowHasOneDecayAndNoEffectBeforeTheResult() {
        assertEquals(0.0f, CasinoVfx.envelope(999, 1000, 1000));
        assertEquals(0.0f, CasinoVfx.envelope(1000, -1, 1000));
        assertEquals(1.0f, CasinoVfx.envelope(1000, 1000, 1000));
        assertEquals(0.25f, CasinoVfx.envelope(1500, 1000, 1000));
        assertEquals(0.0f, CasinoVfx.envelope(2000, 1000, 1000));
    }
}
