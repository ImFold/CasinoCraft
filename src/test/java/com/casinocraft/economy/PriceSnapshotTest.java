package com.casinocraft.economy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PriceSnapshotTest {
    @Test
    void snapshotsOwnTheirDataAndDoNotLeakBetweenScreens() {
        Map<String, PriceSnapshot.Entry> source = new HashMap<>();
        source.put("minecraft:diamond", new PriceSnapshot.Entry(400, 600));
        PriceSnapshot first = new PriceSnapshot(source);
        source.put("minecraft:diamond", new PriceSnapshot.Entry(800, 1200));
        PriceSnapshot second = new PriceSnapshot(source);

        assertEquals(600L, first.shopPrice("minecraft:diamond"));
        assertEquals(1200L, second.shopPrice("minecraft:diamond"));
        assertEquals(0L, PriceSnapshot.EMPTY.shopPrice("minecraft:diamond"));
        assertThrows(UnsupportedOperationException.class, () -> first.entries().clear());
    }

    @Test
    void missingAndSellOnlyItemsCannotBeBought() {
        PriceSnapshot prices = new PriceSnapshot(Map.of(
                "casinocraft:chip_100", new PriceSnapshot.Entry(100, 0)));
        assertEquals(0L, prices.shopPrice("casinocraft:chip_100"));
        assertEquals(6400L, prices.totalSellPrice("casinocraft:chip_100", 64, 0, 0));
        assertEquals(0L, prices.shopPrice("minecraft:bedrock"));
        assertEquals(0L, prices.totalSellPrice("minecraft:bedrock", 64, 0, 0));
    }

    @Test
    void cashierPreviewUsesServerDurabilityAndRounding() {
        PriceSnapshot prices = new PriceSnapshot(Map.of(
                "minecraft:diamond_sword", new PriceSnapshot.Entry(801, 1002)));
        assertEquals(801L, prices.totalSellPrice("minecraft:diamond_sword", 1, 0, 100));
        assertEquals(401L, prices.totalSellPrice("minecraft:diamond_sword", 1, 50, 100));
        assertEquals(0L, prices.totalSellPrice("minecraft:diamond_sword", 1, 100, 100));
        assertEquals(0L, prices.totalSellPrice("minecraft:diamond_sword", 1, 150, 100));
    }

    @Test
    void negativePricesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PriceSnapshot.Entry(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new PriceSnapshot.Entry(10, -1));
    }
}
