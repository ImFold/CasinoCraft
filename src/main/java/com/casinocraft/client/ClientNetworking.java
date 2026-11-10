package com.casinocraft.client;

import com.casinocraft.network.ModNetworking;
import com.casinocraft.economy.PriceSnapshot;
import com.casinocraft.network.SlotResultPacket;
import com.casinocraft.screen.CasinoScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.PRICES, (client, handler, buf, sender) -> {
            int syncId = buf.readVarInt();
            PriceSnapshot prices = ModNetworking.readPrices(buf);
            client.execute(() -> {
                if (client.player != null
                        && client.player.currentScreenHandler instanceof CasinoScreenHandler casino
                        && casino.syncId == syncId) {
                    casino.setPrices(prices);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SLOTS_RESULT, (client, handler, buf, sender) -> {
            SlotResultPacket result = SlotResultPacket.read(buf);
            client.execute(() -> {
                CasinoScreen screen = CasinoScreen.current();
                if (screen != null && client.player != null
                        && client.player.currentScreenHandler.syncId == result.syncId()) {
                    screen.onSlotsResult(result.grid(), result.payout(), result.stake(), result.balance());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ROULETTE_RESULT, (client, handler, buf, sender) -> {
            int number = buf.readVarInt();
            long payout = buf.readLong();
            long stake = buf.readLong();
            long newBalance = buf.readLong();
            client.execute(() -> {
                CasinoScreen screen = CasinoScreen.current();
                if (screen != null) {
                    screen.onRouletteResult(number, payout, stake, newBalance);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.NOTICE, (client, handler, buf, sender) -> {
            String key = buf.readString(256);
            long amount = buf.readLong();
            client.execute(() -> {
                CasinoScreen screen = CasinoScreen.current();
                if (screen != null) {
                    screen.onNotice(key, amount);
                }
            });
        });
    }
}

