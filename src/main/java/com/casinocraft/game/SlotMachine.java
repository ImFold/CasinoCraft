package com.casinocraft.game;

import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 5 x 3 independent weighted cells, five fixed lines, left-to-right Wild substitution.
 * Exact theoretical return: 96.1830768%, with a total stake divisible by five.
 */
public final class SlotMachine {
    public static final int REELS = 5;
    public static final int ROWS = 3;
    public static final int CELL_COUNT = REELS * ROWS;
    public static final int LINE_COUNT = 5;
    private static final int[][] LINES = {
            {1, 1, 1, 1, 1}, {0, 0, 0, 0, 0}, {2, 2, 2, 2, 2},
            {0, 1, 2, 1, 0}, {2, 1, 0, 1, 2}
    };
    private static final SlotSymbol[] SYMBOLS = SlotSymbol.values();
    private static final int TOTAL_WEIGHT = java.util.Arrays.stream(SYMBOLS).mapToInt(s -> s.weight).sum();

    public record LineWin(int line, SlotSymbol symbol, int matches, int multiplier) { }

    private SlotMachine() { }

    public static int rowOnLine(int line, int reel) {
        return LINES[line][reel];
    }

    public static SlotSymbol[] spin(Random random) {
        SlotSymbol[] result = new SlotSymbol[CELL_COUNT];
        for (int i = 0; i < result.length; i++) {
            int roll = random.nextInt(TOTAL_WEIGHT);
            for (SlotSymbol symbol : SYMBOLS) {
                roll -= symbol.weight;
                if (roll < 0) {
                    result[i] = symbol;
                    break;
                }
            }
        }
        return result;
    }

    /** One best award per line, even when leading Wilds allow several interpretations. */
    public static LineWin evaluateLine(SlotSymbol[] symbols, int line) {
        validate(symbols, REELS);
        LineWin best = new LineWin(line, SlotSymbol.CHERRY, 0, 0);
        for (SlotSymbol target : SYMBOLS) {
            if (target == SlotSymbol.WILD) {
                continue;
            }
            int matches = 0;
            while (matches < REELS && (symbols[matches] == target || symbols[matches] == SlotSymbol.WILD)) {
                matches++;
            }
            int multiplier = target.multiplier(matches);
            if (multiplier > best.multiplier()) {
                best = new LineWin(line, target, matches, multiplier);
            }
        }
        return best;
    }

    public static List<LineWin> winningLines(SlotSymbol[] grid) {
        validate(grid, CELL_COUNT);
        List<LineWin> wins = new ArrayList<>();
        for (int line = 0; line < LINE_COUNT; line++) {
            SlotSymbol[] symbols = new SlotSymbol[REELS];
            for (int reel = 0; reel < REELS; reel++) {
                symbols[reel] = grid[reel * ROWS + LINES[line][reel]];
            }
            LineWin win = evaluateLine(symbols, line);
            if (win.multiplier() > 0) {
                wins.add(win);
            }
        }
        return List.copyOf(wins);
    }

    public static long payout(SlotSymbol[] grid, long totalStake) {
        if (totalStake <= 0L || totalStake % LINE_COUNT != 0L) {
            throw new IllegalArgumentException("Total slot stake must be positive and divisible by five");
        }
        long total = 0L;
        for (LineWin win : winningLines(grid)) {
            total = Math.addExact(total, Math.multiplyExact(totalStake / LINE_COUNT, win.multiplier()));
        }
        return total;
    }

    private static void validate(SlotSymbol[] symbols, int length) {
        if (symbols == null || symbols.length != length) {
            throw new IllegalArgumentException("Unexpected slot grid size");
        }
        for (SlotSymbol symbol : symbols) {
            Objects.requireNonNull(symbol, "Missing slot symbol");
        }
    }
}
