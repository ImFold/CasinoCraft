package com.casinocraft.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlotMachineTest {
    @Test
    void weightedEnumerationMatchesDocumentedReturn() {
        long weightedPayout = 0L;
        long totalOutcomes = 0L;
        for (SlotSymbol a : SlotSymbol.values()) {
            for (SlotSymbol b : SlotSymbol.values()) {
                for (SlotSymbol c : SlotSymbol.values()) {
                    long weight = (long) a.weight * b.weight * c.weight;
                    weightedPayout += weight * SlotMachine.payout(a, b, c, 1L);
                    totalOutcomes += weight;
                }
            }
        }
        assertEquals(1_000_000L, totalOutcomes);
        assertEquals(845_946L, weightedPayout);
        assertEquals(0.845946, (double) weightedPayout / totalOutcomes, 1e-12);
    }

    @Test
    void pairReturnsStakeInEveryPositionAndDistinctSymbolsLose() {
        assertEquals(100L, SlotMachine.payout(SlotSymbol.CHERRY, SlotSymbol.CHERRY, SlotSymbol.SEVEN, 100L));
        assertEquals(100L, SlotMachine.payout(SlotSymbol.CHERRY, SlotSymbol.SEVEN, SlotSymbol.CHERRY, 100L));
        assertEquals(100L, SlotMachine.payout(SlotSymbol.SEVEN, SlotSymbol.CHERRY, SlotSymbol.CHERRY, 100L));
        assertEquals(0L, SlotMachine.payout(SlotSymbol.CHERRY, SlotSymbol.LEMON, SlotSymbol.SEVEN, 100L));
    }
}
