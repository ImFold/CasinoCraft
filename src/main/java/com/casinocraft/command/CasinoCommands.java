package com.casinocraft.command;

import com.casinocraft.economy.CasinoEconomy;
import com.casinocraft.economy.ItemPrices;
import com.casinocraft.economy.PlayerBalances;
import com.casinocraft.network.ModNetworking;
import com.casinocraft.util.Money;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CasinoCommands {
    private CasinoCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("casino")
                        .then(CommandManager.literal("balance")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    long balance = CasinoEconomy.balance(player);
                                    context.getSource().sendFeedback(
                                            () -> Text.translatable("command.casinocraft.balance",
                                                    player.getEntityName(), Money.format(balance)), false);
                                    return (int) Math.min(Integer.MAX_VALUE, balance);
                                })
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                            long balance = CasinoEconomy.balance(target);
                                            context.getSource().sendFeedback(
                                                    () -> Text.translatable("command.casinocraft.balance_other",
                                                            target.getEntityName(), Money.format(balance)), false);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("top")
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    List<Map.Entry<UUID, Long>> top = PlayerBalances.get(source.getServer()).top(5);
                                    if (top.isEmpty()) {
                                        source.sendFeedback(() -> Text.translatable("command.casinocraft.top_empty"), false);
                                        return 0;
                                    }
                                    source.sendFeedback(() -> Text.translatable("command.casinocraft.top_header"), false);
                                    int place = 1;
                                    for (Map.Entry<UUID, Long> entry : top) {
                                        String name = source.getServer().getUserCache()
                                                .getByUuid(entry.getKey())
                                                .map(profile -> profile.getName())
                                                .orElse(entry.getKey().toString().substring(0, 8));
                                        int current = place++;
                                        source.sendFeedback(() -> Text.translatable("command.casinocraft.top_line",
                                                current, name, Money.format(entry.getValue())), false);
                                    }
                                    return top.size();
                                }))
                        .then(CommandManager.literal("give")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .then(CommandManager.argument("amount", LongArgumentType.longArg(1L))
                                                .executes(context -> {
                                                    Collection<ServerPlayerEntity> players =
                                                            EntityArgumentType.getPlayers(context, "players");
                                                    long amount = LongArgumentType.getLong(context, "amount");
                                                    for (ServerPlayerEntity player : players) {
                                                        CasinoEconomy.addBalance(player, amount);
                                                    }
                                                    context.getSource().sendFeedback(
                                                            () -> Text.translatable("command.casinocraft.give",
                                                                    players.size(), Money.format(amount)), true);
                                                    return players.size();
                                                }))))
                        .then(CommandManager.literal("set")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .then(CommandManager.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(context -> {
                                                    Collection<ServerPlayerEntity> players =
                                                            EntityArgumentType.getPlayers(context, "players");
                                                    long amount = LongArgumentType.getLong(context, "amount");
                                                    for (ServerPlayerEntity player : players) {
                                                        CasinoEconomy.setBalance(player, amount);
                                                    }
                                                    context.getSource().sendFeedback(
                                                            () -> Text.translatable("command.casinocraft.set",
                                                                    players.size(), Money.format(amount)), true);
                                                    return players.size();
                                                }))))
                        .then(CommandManager.literal("reload")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    ItemPrices.load();
                                    ModNetworking.refreshPrices(context.getSource().getServer());
                                    context.getSource().sendFeedback(
                                            () -> Text.translatable("command.casinocraft.reload"), true);
                                    return 1;
                                }))));
    }
}

