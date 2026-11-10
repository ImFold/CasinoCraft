package com.casinocraft.game;

import net.minecraft.util.math.random.Random;

/**
 * Логика слотов и таблица выплат.
 * <p>
 * Три одинаковых символа платят bet * tripleMultiplier, два одинаковых возвращают ставку.
 * Математическое ожидание при текущих весах = 0.845946 от ставки
 * (теоретический возврат 84.5946%, перевес казино 15.4054%).
 */
public final class SlotMachine {
    private static final SlotSymbol[] SYMBOLS = SlotSymbol.values();
    private static final int TOTAL_WEIGHT;

    static {
        int total = 0;
        for (SlotSymbol symbol : SYMBOLS) {
            total += symbol.weight;
        }
        TOTAL_WEIGHT = total;
    }

    private SlotMachine() {
    }

    public static SlotSymbol[] spin(Random random) {
        SlotSymbol[] result = new SlotSymbol[3];
        for (int i = 0; i < 3; i++) {
            result[i] = pick(random);
        }
        return result;
    }

    private static SlotSymbol pick(Random random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        for (SlotSymbol symbol : SYMBOLS) {
            roll -= symbol.weight;
            if (roll < 0) {
                return symbol;
            }
        }
        return SYMBOLS[0];
    }

    public static long payout(SlotSymbol a, SlotSymbol b, SlotSymbol c, long bet) {
        if (a == b && b == c) {
            return bet * a.tripleMultiplier;
        }
        if (a == b || b == c || a == c) {
            return bet;
        }
        return 0L;
    }
}

