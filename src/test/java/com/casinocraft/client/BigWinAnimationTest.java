package com.casinocraft.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BigWinAnimationTest {
    @Test
    void usesGrossPayoutOverTotalStakeWithExactThresholds() {
        BigWinAnimation animation = new BigWinAnimation();
        assertFalse(animation.start(1000, 999, 100, false));
        assertTrue(animation.start(1000, 1000, 100, false));
        assertEquals(BigWinAnimation.Tier.BIG, animation.tier());
        assertTrue(animation.start(1000, 2499, 100, false));
        assertEquals(BigWinAnimation.Tier.BIG, animation.tier());
        animation.start(1000, 2500, 100, false);
        assertEquals(BigWinAnimation.Tier.MEGA, animation.tier());
        animation.start(1000, 9999, 100, false);
        assertEquals(BigWinAnimation.Tier.MEGA, animation.tier());
        animation.start(1000, 10000, 100, false);
        assertEquals(BigWinAnimation.Tier.EPIC, animation.tier());
        assertEquals(9900, animation.profit());
    }

    @Test
    void counterIsMonotonicFrameIndependentAndEndsAtExactServerPayout() {
        BigWinAnimation animation = new BigWinAnimation();
        animation.start(1000, 1_500_000_000L, 1_000_000L, true);
        long previous = 0L;
        for (long now = 1000; now <= 3400; now += 17) {
            long value = animation.displayedPayout(now);
            assertTrue(value >= previous && value <= animation.payout());
            assertEquals(value, animation.displayedPayout(now));
            previous = value;
        }
        assertEquals(1_500_000_000L, animation.displayedPayout(3400));
        assertTrue(animation.jackpot());
        assertTrue(animation.active(5999));
        assertFalse(animation.active(6000));
        assertEquals(0.0f, animation.opacity(6000));
    }

    @Test
    void clickRevealsTotalImmediatelyAndRepeatedClicksCannotExtendAnimation() {
        BigWinAnimation animation = new BigWinAnimation();
        animation.start(1000, 2500, 100, false);
        animation.finishCounting(1200);
        assertEquals(2500, animation.displayedPayout(1200));
        animation.finishCounting(1800);
        assertTrue(animation.active(2099));
        assertFalse(animation.active(2100));
    }

    @Test
    void newResultsAndDismissalCannotReuseAnOldWin() {
        BigWinAnimation animation = new BigWinAnimation();
        animation.start(1000, 10000, 100, true);
        animation.clear();
        assertFalse(animation.active(1100));
        assertEquals(0L, animation.payout());
        assertFalse(animation.jackpot());
        assertFalse(animation.start(1200, 100, 100, false));
        assertFalse(animation.start(1200, 50, 100, false));
        assertFalse(animation.start(1200, 10000, 0, false));
        assertFalse(animation.start(1200, -1, 100, false));
    }
}
