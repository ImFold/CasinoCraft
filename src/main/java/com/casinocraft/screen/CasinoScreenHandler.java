package com.casinocraft.screen;

import com.casinocraft.CasinoLayout;
import com.casinocraft.economy.PriceSnapshot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Контейнер казино: 6 слотов кассы + инвентарь игрока.
 * Баланс передаётся через свойства контейнера (2 int = один long),
 * поэтому он всегда синхронизирован с сервером.
 */
public class CasinoScreenHandler extends ScreenHandler {
    public static final int DEPOSIT_SLOTS = 6;

    private final Inventory deposit = new SimpleInventory(DEPOSIT_SLOTS);
    private final ArrayPropertyDelegate balanceData = new ArrayPropertyDelegate(2);
    private PriceSnapshot prices = PriceSnapshot.EMPTY;

    public PriceSnapshot getPrices() {
        return prices;
    }

    public void setPrices(PriceSnapshot prices) {
        this.prices = prices;
    }

    public CasinoScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.CASINO, syncId);
        this.addProperties(balanceData);

        for (int index = 0; index < DEPOSIT_SLOTS; index++) {
            int column = index % 3;
            int row = index / 3;
            this.addSlot(new Slot(deposit, index,
                    CasinoLayout.DEPOSIT_X + column * 18,
                    CasinoLayout.DEPOSIT_Y + row * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory,
                        column + row * 9 + 9,
                        CasinoLayout.INV_X + column * 18,
                        CasinoLayout.INV_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column,
                    CasinoLayout.INV_X + column * 18,
                    CasinoLayout.HOTBAR_Y));
        }
    }

    public Inventory getDeposit() {
        return deposit;
    }

    public long getBalance() {
        long low = Integer.toUnsignedLong(balanceData.get(0));
        long high = Integer.toUnsignedLong(balanceData.get(1));
        return (high << 32) | low;
    }

    public void setBalanceLocal(long value) {
        balanceData.set(0, (int) (value & 0xFFFFFFFFL));
        balanceData.set(1, (int) (value >>> 32));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < DEPOSIT_SLOTS) {
                if (!this.insertItem(stack, DEPOSIT_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(stack, 0, DEPOSIT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return result;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        for (int i = 0; i < deposit.size(); i++) {
            ItemStack stack = deposit.removeStack(i);
            if (!stack.isEmpty()) {
                player.getInventory().offerOrDrop(stack);
            }
        }
    }
}

