package com.casinocraft.economy;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Балансы игроков. Хранятся в данных мира (data/casinocraft_balances.dat),
 * поэтому переживают перезапуск сервера.
 */
public class PlayerBalances extends PersistentState {
    private static final String SAVE_KEY = "casinocraft_balances";

    private final Map<UUID, Long> balances = new HashMap<>();

    public PlayerBalances() {
    }

    public static PlayerBalances fromNbt(NbtCompound nbt) {
        PlayerBalances state = new PlayerBalances();
        NbtCompound stored = nbt.getCompound("balances");
        for (String key : stored.getKeys()) {
            try {
                state.balances.put(UUID.fromString(key), stored.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // битая запись — пропускаем
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound stored = new NbtCompound();
        balances.forEach((uuid, value) -> stored.putLong(uuid.toString(), value));
        nbt.put("balances", stored);
        return nbt;
    }

    public long get(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    public void set(UUID playerId, long value) {
        if (value <= 0L) {
            if (balances.remove(playerId) != null) {
                markDirty();
            }
            return;
        }
        Long previous = balances.put(playerId, value);
        if (previous == null || previous.longValue() != value) {
            markDirty();
        }
    }
    public List<Map.Entry<UUID, Long>> top(int limit) {
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(balances.entrySet());
        entries.sort(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()));
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public static PlayerBalances get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                PlayerBalances::fromNbt,
                PlayerBalances::new,
                SAVE_KEY);
    }
}
