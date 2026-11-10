package com.casinocraft.network;

import com.casinocraft.game.SlotMachine;
import com.casinocraft.game.SlotSymbol;
import net.minecraft.network.PacketByteBuf;

import java.util.Objects;

/** Fixed-size server result, scoped to the casino container that requested it. */
public record SlotResultPacket(int syncId, SlotSymbol[] grid, long payout, long stake, long balance) {
    public SlotResultPacket {
        if (grid.length != SlotMachine.CELL_COUNT) {
            throw new IllegalArgumentException("Expected fifteen slot cells");
        }
        grid = grid.clone();
        for (SlotSymbol symbol : grid) {
            Objects.requireNonNull(symbol, "Missing slot symbol");
        }
    }

    @Override
    public SlotSymbol[] grid() {
        return grid.clone();
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(syncId);
        for (SlotSymbol symbol : grid) {
            buf.writeVarInt(symbol.ordinal());
        }
        buf.writeLong(payout);
        buf.writeLong(stake);
        buf.writeLong(balance);
    }

    public static SlotResultPacket read(PacketByteBuf buf) {
        int syncId = buf.readVarInt();
        SlotSymbol[] symbols = SlotSymbol.values();
        SlotSymbol[] grid = new SlotSymbol[SlotMachine.CELL_COUNT];
        for (int i = 0; i < grid.length; i++) {
            int ordinal = buf.readVarInt();
            if (ordinal < 0 || ordinal >= symbols.length) {
                throw new IllegalArgumentException("Invalid slot symbol");
            }
            grid[i] = symbols[ordinal];
        }
        return new SlotResultPacket(syncId, grid, buf.readLong(), buf.readLong(), buf.readLong());
    }
}
