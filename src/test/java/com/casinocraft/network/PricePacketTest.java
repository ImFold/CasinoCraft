package com.casinocraft.network;

import com.casinocraft.economy.PriceSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PricePacketTest {
    @Test
    void roundTripPreservesShopPricesSellOnlyItemsAndLongValues() {
        PriceSnapshot expected = new PriceSnapshot(Map.of(
                "minecraft:diamond", new PriceSnapshot.Entry(400, 600),
                "casinocraft:chip_100", new PriceSnapshot.Entry(100, 0),
                "example:expensive_item", new PriceSnapshot.Entry(3_000_000_000L, 4_000_000_000L)));
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            ModNetworking.writePrices(buf, expected);
            assertEquals(expected, ModNetworking.readPrices(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void emptyCatalogueRoundTrips() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            ModNetworking.writePrices(buf, PriceSnapshot.EMPTY);
            assertEquals(PriceSnapshot.EMPTY, ModNetworking.readPrices(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void truncatedSnapshotIsRejected() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> ModNetworking.readPrices(buf));
        } finally {
            buf.release();
        }
    }
}
