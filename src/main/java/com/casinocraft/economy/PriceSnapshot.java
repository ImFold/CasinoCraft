package com.casinocraft.economy;

import java.util.Map;

/** Immutable server prices for one open casino screen; never reads a client config. */
public record PriceSnapshot(Map<String, Entry> entries) {
    public static final PriceSnapshot EMPTY = new PriceSnapshot(Map.of());

    public PriceSnapshot {
        entries = Map.copyOf(entries);
    }

    public record Entry(long sell, long shop) {
        public Entry {
            if (sell < 0L || shop < 0L) {
                throw new IllegalArgumentException("Prices must be non-negative");
            }
        }
    }

    public long shopPrice(String itemId) {
        Entry entry = entries.get(itemId);
        return entry == null ? 0L : entry.shop();
    }

    public long totalSellPrice(String itemId, int count, int damage, int maxDamage) {
        Entry entry = entries.get(itemId);
        return entry == null ? 0L : totalPrice(entry.sell(), count, damage, maxDamage);
    }

    /** Shared by server settlement and client preview, including durability rounding. */
    public static long totalPrice(long unitPrice, int count, int damage, int maxDamage) {
        long value = unitPrice * count;
        if (maxDamage > 0) {
            double remaining = 1.0 - (double) damage / maxDamage;
            return Math.round(value * Math.max(0.0, Math.min(1.0, remaining)));
        }
        return value;
    }
}
