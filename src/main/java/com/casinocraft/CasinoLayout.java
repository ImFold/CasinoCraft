package com.casinocraft;

/**
 * Общая геометрия интерфейса. Используется и обработчиком слотов (сервер),
 * и экраном (клиент), чтобы позиции слотов всегда совпадали.
 */
public final class CasinoLayout {
    public static final int WIDTH = 340;
    public static final int HEIGHT = 252;

    /** Слоты кассы: 3 колонки x 2 ряда. */
    public static final int DEPOSIT_X = 52;
    public static final int DEPOSIT_Y = 44;

    /** Инвентарь игрока. */
    public static final int INV_X = 89;
    public static final int INV_Y = 158;
    public static final int HOTBAR_Y = 220;

    /** Панели. */
    public static final int LEFT_X = 6;
    public static final int LEFT_Y = 26;
    public static final int LEFT_W = 142;
    public static final int LEFT_H = 124;

    public static final int RIGHT_X = 152;
    public static final int RIGHT_Y = 26;
    public static final int RIGHT_W = 182;
    public static final int RIGHT_H = 124;

    /** Строка уведомлений внизу. */
    public static final int NOTICE_Y = 240;

    private CasinoLayout() {
    }
}
