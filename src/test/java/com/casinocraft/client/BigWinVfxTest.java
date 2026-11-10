package com.casinocraft.client;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BigWinVfxTest {
    private record Fill(int left, int top, int right, int bottom, int color) { }

    private static List<Fill> frame(long elapsed, int coins) {
        List<Fill> fills = new ArrayList<>();
        BigWinVfx.draw((l, t, r, b, color) -> fills.add(new Fill(l, t, r, b, color)),
                elapsed, coins, 1.0f, 156, 64, 331, 100, 0xFFC94A);
        return fills;
    }

    @Test
    void showerStaysInsideRoulettePanelAndHasAFixedBudget() {
        for (long elapsed = 0; elapsed < 3000; elapsed += 33) {
            List<Fill> fills = frame(elapsed, 1000);
            assertTrue(fills.size() <= 64 * 4);
            for (Fill fill : fills) {
                assertTrue(fill.left >= 156 && fill.right <= 331 && fill.top >= 64 && fill.bottom <= 100);
                assertTrue(fill.left < fill.right && fill.top < fill.bottom);
            }
        }
    }

    @Test
    void reducedModeAndExpiryLeaveNoCoinsAndFramesAreRepeatable() {
        assertTrue(frame(500, 0).isEmpty());
        assertTrue(frame(3000, 64).isEmpty());
        assertFalse(frame(500, 64).isEmpty());
        assertEquals(frame(500, 64), frame(500, 64));
    }
}
