package com.casinocraft.game;

import net.minecraft.util.math.random.Random;

/**
 * Европейская рулетка: 37 чисел, одно зеро.
 * Красное/чёрное/чёт/нечет платят x2, зеро — x36.
 */
public final class Roulette {
    private static final int[] RED = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};

    private Roulette() {
    }

    public static int spin(Random random) {
        return random.nextInt(37);
    }

    public static boolean isRed(int number) {
        for (int red : RED) {
            if (red == number) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlack(int number) {
        return number != 0 && !isRed(number);
    }

    public static boolean wins(RouletteBet bet, int number) {
        return switch (bet) {
            case RED -> isRed(number);
            case BLACK -> isBlack(number);
            case EVEN -> number != 0 && number % 2 == 0;
            case ODD -> number % 2 == 1;
            case ZERO -> number == 0;
        };
    }

    public static long payout(RouletteBet bet, int number, long stake) {
        if (!wins(bet, number)) {
            return 0L;
        }
        return bet == RouletteBet.ZERO ? stake * 36L : stake * 2L;
    }
}
