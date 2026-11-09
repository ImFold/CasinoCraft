package com.casinocraft.client;

import com.casinocraft.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SLOTS_RESULT, (client, handler, buf, sender) -> {
            int a = buf.readVarInt();
            int b = buf.readVarInt();
            int c = buf.readVarInt();
            long payout = buf.readLong();
            long stake = buf.readLong();
            long newBalance = buf.readLong();
            client.execute(() -> {
                CasinoScreen screen = CasinoScreen.current();
                if (screen != null) {
                    screen.onSlotsResult(a, b, c, payout, stake, newBalance);
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
