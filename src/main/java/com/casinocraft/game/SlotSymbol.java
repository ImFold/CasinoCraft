package com.casinocraft.game;

/** Weighted video-slot symbols. Multipliers apply to one fifth of the total stake. */
public enum SlotSymbol {
    CHERRY("*", 0xFFE05555, 28, 6, 20, 50),
    LEMON("%", 0xFFF2D34A, 24, 8, 25, 80),
    DOLLAR("$", 0xFF6FD66F, 18, 10, 35, 110),
    BELL("@", 0xFFF0A93B, 15, 15, 50, 160),
    DIAMOND("#", 0xFF6FE3FF, 8, 30, 110, 400),
    SEVEN("7", 0xFFFFD34A, 5, 60, 250, 1500),
    WILD("W", 0xFFDE9AFF, 2, 0, 0, 0);

    public final String glyph;
    public final int color;
    public final int weight;
    private final int three;
    private final int four;
    private final int five;

    SlotSymbol(String glyph, int color, int weight, int three, int four, int five) {
        this.glyph = glyph;
        this.color = color;
        this.weight = weight;
        this.three = three;
        this.four = four;
        this.five = five;
    }

    public int multiplier(int matches) {
        return switch (matches) {
            case 3 -> three;
            case 4 -> four;
            case 5 -> five;
            default -> 0;
        };
    }
}
