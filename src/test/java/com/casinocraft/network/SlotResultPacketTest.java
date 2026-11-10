package com.casinocraft.network;

import com.casinocraft.game.SlotSymbol;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlotResultPacketTest {
    @Test
    void fifteenCellsWildsAndLargeBalancesRoundTripWithoutAliasing() {
        SlotSymbol[] grid = new SlotSymbol[15];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = SlotSymbol.values()[i % SlotSymbol.values().length];
        }
        var sent = new SlotResultPacket(7, grid, 1_500_000_000L, 1_000_000L, 999_999_999_999L);
        grid[0] = SlotSymbol.WILD;
        assertEquals(SlotSymbol.CHERRY, sent.grid()[0]);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            sent.write(buf);
            var received = SlotResultPacket.read(buf);
            assertEquals(7, received.syncId());
            assertArrayEquals(sent.grid(), received.grid());
            assertEquals(sent.payout(), received.payout());
            assertEquals(sent.stake(), received.stake());
            assertEquals(sent.balance(), received.balance());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void malformedSymbolAndWrongGridSizeAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SlotResultPacket(1, new SlotSymbol[3], 0, 100, 0));
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(1);
            buf.writeVarInt(99);
            assertThrows(IllegalArgumentException.class, () -> SlotResultPacket.read(buf));
        } finally {
            buf.release();
        }
    }
}
