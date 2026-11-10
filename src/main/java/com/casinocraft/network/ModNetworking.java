package com.casinocraft.network;

import com.casinocraft.CasinoCraftMod;
import com.casinocraft.economy.CasinoEconomy;
import com.casinocraft.economy.ItemPrices;
import com.casinocraft.economy.PriceSnapshot;
import com.casinocraft.screen.CasinoScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class ModNetworking {
    public static final Identifier SELL = CasinoCraftMod.id("sell");
    public static final Identifier WITHDRAW = CasinoCraftMod.id("withdraw");
    public static final Identifier SPIN_SLOTS = CasinoCraftMod.id("spin_slots");
    public static final Identifier SPIN_ROULETTE = CasinoCraftMod.id("spin_roulette");
    public static final Identifier BUY = CasinoCraftMod.id("buy");

    public static final Identifier SLOTS_RESULT = CasinoCraftMod.id("slots_result");
    public static final Identifier ROULETTE_RESULT = CasinoCraftMod.id("roulette_result");
    public static final Identifier NOTICE = CasinoCraftMod.id("notice");
    public static final Identifier PRICES = CasinoCraftMod.id("prices");

    private ModNetworking() {
    }

    public static void sendPrices(ServerPlayerEntity player) {
        if (!(player.currentScreenHandler instanceof CasinoScreenHandler handler)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(handler.syncId);
        writePrices(buf, ItemPrices.snapshot(player.getWorld()));
        ServerPlayNetworking.send(player, PRICES, buf);
    }

    public static void refreshPrices(MinecraftServer server) {
        ItemPrices.ensureRecipes(server.getOverworld());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendPrices(player);
        }
    }

    public static void writePrices(PacketByteBuf buf, PriceSnapshot prices) {
        buf.writeVarInt(prices.entries().size());
        prices.entries().forEach((id, entry) -> {
            buf.writeString(id);
            buf.writeLong(entry.sell());
            buf.writeLong(entry.shop());
        });
    }

    public static PriceSnapshot readPrices(PacketByteBuf buf) {
        int count = buf.readVarInt();
        // Every entry has at least a string length byte and two longs.
        if (count < 0 || count > buf.readableBytes() / 17) {
            throw new IllegalArgumentException("Invalid price snapshot size");
        }
        Map<String, PriceSnapshot.Entry> entries = new HashMap<>();
        for (int i = 0; i < count; i++) {
            entries.put(buf.readString(), new PriceSnapshot.Entry(buf.readLong(), buf.readLong()));
        }
        return new PriceSnapshot(entries);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SELL, (server, player, handler, buf, sender) ->
                server.execute(() -> CasinoEconomy.sellDeposit(player)));

        ServerPlayNetworking.registerGlobalReceiver(WITHDRAW, (server, player, handler, buf, sender) ->
                server.execute(() -> CasinoEconomy.withdraw(player)));

        ServerPlayNetworking.registerGlobalReceiver(SPIN_SLOTS, (server, player, handler, buf, sender) -> {
            int bet = buf.readVarInt();
            server.execute(() -> CasinoEconomy.spinSlots(player, bet));
        });

        ServerPlayNetworking.registerGlobalReceiver(SPIN_ROULETTE, (server, player, handler, buf, sender) -> {
            int bet = buf.readVarInt();
            int betType = buf.readVarInt();
            server.execute(() -> CasinoEconomy.spinRoulette(player, bet, betType));
        });

        ServerPlayNetworking.registerGlobalReceiver(BUY, (server, player, handler, buf, sender) -> {
            Identifier itemId = buf.readIdentifier();
            int count = buf.readVarInt();
            long quotedPrice = buf.readLong();
            server.execute(() -> CasinoEconomy.buy(player, itemId, count, quotedPrice));
        });
    }
}

