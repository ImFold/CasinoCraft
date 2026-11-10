package com.casinocraft.game;

import net.minecraft.util.math.random.Random;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.casinocraft.game.SlotSymbol.*;
import static org.junit.jupiter.api.Assertions.*;

class SlotMachineTest {
    @Test
    void exhaustiveWeightedLinesMatchDocumentedReturn() {
        SlotSymbol[] symbols = SlotSymbol.values();
        long weighted = 0L;
        long outcomes = 0L;
        for (int code = 0; code < 16807; code++) {
            int n = code;
            long weight = 1L;
            SlotSymbol[] line = new SlotSymbol[5];
            for (int i = 0; i < line.length; i++) {
                line[i] = symbols[n % symbols.length];
                n /= symbols.length;
                weight *= line[i].weight;
            }
            weighted += weight * SlotMachine.evaluateLine(line, 0).multiplier();
            outcomes += weight;
        }
        assertEquals(10_000_000_000L, outcomes);
        assertEquals(9_618_307_680L, weighted);
        assertEquals(0.961830768, (double) weighted / outcomes, 1e-12);
    }

    @Test
    void wildChoosesOneBestLeftToRightAward() {
        assertEquals(20, SlotMachine.evaluateLine(new SlotSymbol[]{WILD, CHERRY, WILD, CHERRY, LEMON}, 0).multiplier());
        // Three leading Wilds as sevens pay 60, more than five cherries (50).
        var best = SlotMachine.evaluateLine(new SlotSymbol[]{WILD, WILD, WILD, CHERRY, CHERRY}, 0);
        assertEquals(SEVEN, best.symbol());
        assertEquals(3, best.matches());
        assertEquals(60, best.multiplier());
        assertEquals(1500, SlotMachine.evaluateLine(new SlotSymbol[]{WILD, WILD, WILD, WILD, WILD}, 0).multiplier());
    }

    @Test
    void pairsGapsAndRightHandMatchesDoNotPay() {
        assertEquals(0, SlotMachine.evaluateLine(new SlotSymbol[]{CHERRY, CHERRY, LEMON, CHERRY, CHERRY}, 0).multiplier());
        assertEquals(0, SlotMachine.evaluateLine(new SlotSymbol[]{CHERRY, LEMON, SEVEN, SEVEN, SEVEN}, 0).multiplier());
        assertEquals(0, SlotMachine.evaluateLine(new SlotSymbol[]{WILD, LEMON, CHERRY, LEMON, LEMON}, 0).multiplier());
    }

    @Test
    void fiveLinesShareOneTotalStakeWithoutRounding() {
        SlotSymbol[] grid = new SlotSymbol[15];
        Arrays.fill(grid, CHERRY);
        assertEquals(5, SlotMachine.winningLines(grid).size());
        assertEquals(5000L, SlotMachine.payout(grid, 100));
        assertEquals(1250L, SlotMachine.payout(grid, 25));
        Arrays.fill(grid, WILD);
        assertEquals(1_500_000_000L, SlotMachine.payout(grid, 1_000_000));
    }

    @Test
    void diagonalUsesColumnMajorGridAndPaysOnce() {
        SlotSymbol[] grid = new SlotSymbol[15];
        for (int reel = 0; reel < 5; reel++) {
            for (int row = 0; row < 3; row++) {
                grid[reel * 3 + row] = SlotSymbol.values()[(reel + row) % 6];
            }
            grid[reel * 3 + SlotMachine.rowOnLine(3, reel)] = SEVEN;
        }
        var wins = SlotMachine.winningLines(grid);
        assertEquals(1, wins.size());
        assertEquals(3, wins.get(0).line());
        assertEquals(30000L, SlotMachine.payout(grid, 100));
    }

    @Test
    void randomSpinProducesFifteenValidCellsAndRejectsInvalidInput() {
        SlotSymbol[] grid = SlotMachine.spin(Random.create(42L));
        assertEquals(15, grid.length);
        for (SlotSymbol symbol : grid) {
            assertNotNull(symbol);
        }
        assertThrows(IllegalArgumentException.class, () -> SlotMachine.payout(grid, 7));
        assertThrows(IllegalArgumentException.class, () -> SlotMachine.payout(grid, 0));
        assertThrows(IllegalArgumentException.class, () -> SlotMachine.payout(new SlotSymbol[3], 100));
    }
}
