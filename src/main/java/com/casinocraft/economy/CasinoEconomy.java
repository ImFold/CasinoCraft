package com.casinocraft.economy;

import com.casinocraft.CasinoCraftMod;
import com.casinocraft.ModItems;
import com.casinocraft.game.Roulette;
import com.casinocraft.game.RouletteBet;
import com.casinocraft.game.SlotMachine;
import com.casinocraft.game.SlotSymbol;
import com.casinocraft.network.ModNetworking;
import com.casinocraft.screen.CasinoScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Вся арифметика денег: продажа предметов, ставки, выплаты.
 * Вычисления только на сервере — клиент не может "нарисовать" себе выигрыш.
 */
public final class CasinoEconomy {
    public static final long MIN_BET = 10L;
    public static final long MAX_BET = 1_000_000L;
    public static final long MAX_BALANCE = 1_000_000_000_000L;
    /** За один клик выводим не больше 6 стаков фишек — чтобы не заваливать мир дропом. */
    public static final long MAX_WITHDRAW_VALUE = 38_400L;

    /** Не даём спамить пакетами и держим темп анимации. */
    private static final long SPIN_COOLDOWN_MS = 900L;
    private static final Map<UUID, Long> BUSY_UNTIL = new ConcurrentHashMap<>();

    private CasinoEconomy() {
    }

    public static long balance(ServerPlayerEntity player) {
        return PlayerBalances.get(player.server).get(player.getUuid());
    }

    public static void setBalance(ServerPlayerEntity player, long value) {
        long clamped = Math.max(0L, Math.min(MAX_BALANCE, value));
        PlayerBalances.get(player.server).set(player.getUuid(), clamped);
        if (player.currentScreenHandler instanceof CasinoScreenHandler handler) {
            handler.setBalanceLocal(clamped);
        }
    }

    public static void addBalance(ServerPlayerEntity player, long delta) {
        setBalance(player, balance(player) + delta);
    }

    // ------------------------------------------------------------------ касса

    public static void sellDeposit(ServerPlayerEntity player) {
        if (!(player.currentScreenHandler instanceof CasinoScreenHandler handler)) {
            return;
        }
        ItemPrices.ensureRecipes(player.getWorld());
        Inventory deposit = handler.getDeposit();
        long earned = 0L;
        int sold = 0;
        boolean skipped = false;

        for (int i = 0; i < deposit.size(); i++) {
            ItemStack stack = deposit.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            long value = ItemPrices.totalPrice(stack);
            if (value <= 0L) {
                skipped = true;
                continue;
            }
            earned = Math.min(MAX_BALANCE, earned + value);
            sold += stack.getCount();
            deposit.setStack(i, ItemStack.EMPTY);
        }

        if (sold == 0) {
            sendNotice(player, skipped ? "message.casinocraft.nothing_priced" : "message.casinocraft.nothing_to_sell", 0L);
            return;
        }

        setBalance(player, balance(player) + earned);
        sendNotice(player, skipped ? "message.casinocraft.sold_partial" : "message.casinocraft.sold", earned);
    }

    /**
     * Вывод баланса в фишки. Курс жёсткий: 1 фишка = ровно 100 (или 10) баланса
     * и в кассе, и при выводе. Арбитраж «сдал-вывел» невозможен.
     */
    public static void withdraw(ServerPlayerEntity player) {
        if (!(player.currentScreenHandler instanceof CasinoScreenHandler)) {
            return;
        }
        long available = balance(player);
        long amount = Math.min(available, MAX_WITHDRAW_VALUE);
        long chipHundreds = amount / 100L;
        long chipTens = (amount % 100L) / 10L;
        long value = chipHundreds * 100L + chipTens * 10L;

        if (value <= 0L) {
            sendNotice(player, "message.casinocraft.withdraw_nothing", 0L);
            return;
        }

        give(player, ModItems.CHIP_100, (int) chipHundreds);
        give(player, ModItems.CHIP_10, (int) chipTens);
        setBalance(player, available - value);
        sendNotice(player, "message.casinocraft.withdrawn", value);
    }

    private static void give(ServerPlayerEntity player, Item item, int count) {
        while (count > 0) {
            int stackSize = Math.min(item.getMaxCount(), count);
            player.getInventory().offerOrDrop(new ItemStack(item, stackSize));
            count -= stackSize;
        }
    }

    /**
     * Покупка предмета из магазина. Цена приходит из конфига, но всегда
     * проверяется на сервере и не может быть ниже цены скупки с наценкой.
     */
    public static void buy(ServerPlayerEntity player, Identifier itemId, int count) {
        if (!(player.currentScreenHandler instanceof CasinoScreenHandler)) {
            return;
        }
        Item item = Registries.ITEM.get(itemId);
        ItemPrices.ensureRecipes(player.getWorld());
        if (!ItemPrices.isBuyable(item)) {
            return;
        }
        long price = ItemPrices.shopPrice(item);
        if (price <= 0L || count <= 0) {
            return;
        }
        int safeCount = Math.min(count, 64);
        long cost = price * safeCount;
        long available = balance(player);

        if (cost > available) {
            sendNotice(player, "message.casinocraft.not_enough", available);
            return;
        }

        give(player, item, safeCount);
        setBalance(player, available - cost);
        sendNotice(player, "message.casinocraft.bought", cost);
    }

    // ------------------------------------------------------------------ слоты
    public static void spinSlots(ServerPlayerEntity player, int bet) {
        if (!beginSpin(player, bet)) {
            return;
        }
        SlotSymbol[] reels = SlotMachine.spin(player.getRandom());
        long payout = SlotMachine.payout(reels[0], reels[1], reels[2], bet);
        long newBalance = settle(player, bet, payout);

        PacketByteBuf buf = PacketByteBufs.create();
        for (SlotSymbol symbol : reels) {
            buf.writeVarInt(symbol.ordinal());
        }
        buf.writeLong(payout);
        buf.writeLong(bet);
        buf.writeLong(newBalance);
        ServerPlayNetworking.send(player, ModNetworking.SLOTS_RESULT, buf);
    }

    // ---------------------------------------------------------------- рулетка

    public static void spinRoulette(ServerPlayerEntity player, int bet, int betType) {
        RouletteBet[] types = RouletteBet.values();
        if (betType < 0 || betType >= types.length) {
            return;
        }
        if (!beginSpin(player, bet)) {
            return;
        }
        RouletteBet type = types[betType];
        int number = Roulette.spin(player.getRandom());
        long payout = Roulette.payout(type, number, bet);
        long newBalance = settle(player, bet, payout);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(number);
        buf.writeLong(payout);
        buf.writeLong(bet);
        buf.writeLong(newBalance);
        ServerPlayNetworking.send(player, ModNetworking.ROULETTE_RESULT, buf);
    }

    // ---------------------------------------------------------------- служебное

    private static boolean beginSpin(ServerPlayerEntity player, long bet) {
        long now = System.currentTimeMillis();
        Long busyUntil = BUSY_UNTIL.get(player.getUuid());
        if (busyUntil != null && busyUntil > now) {
            sendNotice(player, "message.casinocraft.spin_too_fast", 0L);
            return false;
        }
        if (bet < MIN_BET) {
            sendNotice(player, "message.casinocraft.bet_too_small", MIN_BET);
            return false;
        }
        if (bet > MAX_BET) {
            sendNotice(player, "message.casinocraft.bet_too_big", MAX_BET);
            return false;
        }
        long available = balance(player);
        if (available < bet) {
            sendNotice(player, "message.casinocraft.not_enough", available);
            return false;
        }
        BUSY_UNTIL.put(player.getUuid(), now + SPIN_COOLDOWN_MS);
        return true;
    }

    private static long settle(ServerPlayerEntity player, long bet, long payout) {
        long newBalance = balance(player) - bet + payout;
        setBalance(player, newBalance);
        return Math.max(0L, Math.min(MAX_BALANCE, newBalance));
    }

    private static void sendNotice(ServerPlayerEntity player, String key, long amount) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(key);
        buf.writeLong(amount);
        ServerPlayNetworking.send(player, ModNetworking.NOTICE, buf);
    }
}
