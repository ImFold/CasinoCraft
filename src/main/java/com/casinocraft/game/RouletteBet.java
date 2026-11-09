package com.casinocraft.game;

public enum RouletteBet {
    RED("gui.casinocraft.roulette.red", 2),
    BLACK("gui.casinocraft.roulette.black", 2),
    EVEN("gui.casinocraft.roulette.even", 2),
    ODD("gui.casinocraft.roulette.odd", 2),
    ZERO("gui.casinocraft.roulette.zero", 36);

    public final String translationKey;
    public final int multiplier;

    RouletteBet(String translationKey, int multiplier) {
        this.translationKey = translationKey;
        this.multiplier = multiplier;
    }
}
