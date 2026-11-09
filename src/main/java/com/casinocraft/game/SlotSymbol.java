package com.casinocraft.game;

/**
 * Символы барабанов. Вес задаёт частоту выпадения, multiplier — выплату за три одинаковых.
 */
public enum SlotSymbol {
    CHERRY("*", 0xFFE05555, 5, 30),
    LEMON("%", 0xFFF2D34A, 8, 25),
    DOLLAR("$", 0xFF6FD66F, 4, 14),
    BELL("@", 0xFFF0A93B, 12, 18),
    DIAMOND("#", 0xFF6FE3FF, 25, 8),
    SEVEN("7", 0xFFFFD34A, 60, 5);

    public final String glyph;
    public final int color;
    public final int tripleMultiplier;
    public final int weight;

    SlotSymbol(String glyph, int color, int tripleMultiplier, int weight) {
        this.glyph = glyph;
        this.color = color;
        this.tripleMultiplier = tripleMultiplier;
        this.weight = weight;
    }
}
