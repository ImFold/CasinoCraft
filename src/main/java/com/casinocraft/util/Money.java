package com.casinocraft.util;

import java.util.Locale;

public final class Money {
    private Money() {
    }

    /** 1234567 -> "1 234 567" */
    public static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
