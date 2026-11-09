package com.casinocraft.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Фишка казино — физические деньги. Номинал задаётся в коде и совпадает
 * с ценой приёма в кассу, поэтому обмен «баланс <-> фишки» всегда 1:1.
 */
public class CasinoChipMoneyItem extends Item {
    private final long faceValue;

    public CasinoChipMoneyItem(long faceValue, Settings settings) {
        super(settings);
        this.faceValue = faceValue;
    }

    public long getFaceValue() {
        return faceValue;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.casinocraft.chip.value", faceValue).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("item.casinocraft.chip.tooltip").formatted(Formatting.DARK_GRAY));
    }
}
