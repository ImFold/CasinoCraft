package com.casinocraft.network;

import com.casinocraft.CasinoCraftMod;
import com.casinocraft.economy.CasinoEconomy;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    public static final Identifier SELL = CasinoCraftMod.id("sell");
    public static final Identifier WITHDRAW = CasinoCraftMod.id("withdraw");
    public static final Identifier SPIN_SLOTS = CasinoCraftMod.id("spin_slots");
    public static final Identifier SPIN_ROULETTE = CasinoCraftMod.id("spin_roulette");
    public static final Identifier BUY = CasinoCraftMod.id("buy");

    public static final Identifier SLOTS_RESULT = CasinoCraftMod.id("slots_result");
    public static final Identifier ROULETTE_RESULT = CasinoCraftMod.id("roulette_result");
    public static final Identifier NOTICE = CasinoCraftMod.id("notice");

    private ModNetworking() {
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
            server.execute(() -> CasinoEconomy.buy(player, itemId, count));
        });
    }
}
