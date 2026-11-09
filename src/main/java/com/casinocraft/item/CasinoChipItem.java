package com.casinocraft.item;

import com.casinocraft.economy.CasinoEconomy;
import com.casinocraft.screen.CasinoScreenHandler;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Жетон казино: ПКМ открывает интерфейс.
 */
public class CasinoChipItem extends Item {
    public CasinoChipItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, player) -> new CasinoScreenHandler(syncId, inventory),
                    Text.translatable("gui.casinocraft.title")));

            if (serverPlayer.currentScreenHandler instanceof CasinoScreenHandler handler) {
                long balance = CasinoEconomy.balance(serverPlayer);
                handler.setBalanceLocal(balance);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.casinocraft.casino_chip.tooltip").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.casinocraft.casino_chip.tooltip2").formatted(Formatting.DARK_GRAY));
    }
}
