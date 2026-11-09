package com.casinocraft;

import com.casinocraft.item.CasinoChipItem;
import com.casinocraft.item.CasinoChipMoneyItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public final class ModItems {
    public static final RegistryKey<ItemGroup> GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, CasinoCraftMod.id("main"));

    public static final CasinoChipItem CASINO_CHIP =
            new CasinoChipItem(new Item.Settings().maxCount(1));

    /** Номинал фиксирован в коде: 100 и 10. Менять нельзя — иначе появится арбитраж. */
    public static final CasinoChipMoneyItem CHIP_100 =
            new CasinoChipMoneyItem(100L, new Item.Settings());
    public static final CasinoChipMoneyItem CHIP_10 =
            new CasinoChipMoneyItem(10L, new Item.Settings());

    private ModItems() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, CasinoCraftMod.id("casino_chip"), CASINO_CHIP);
        Registry.register(Registries.ITEM, CasinoCraftMod.id("chip_100"), CHIP_100);
        Registry.register(Registries.ITEM, CasinoCraftMod.id("chip_10"), CHIP_10);

        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(CASINO_CHIP))
                .displayName(Text.translatable("itemGroup.casinocraft"))
                .build();
        Registry.register(Registries.ITEM_GROUP, GROUP_KEY, group);
        ItemGroupEvents.modifyEntriesEvent(GROUP_KEY).register(entries -> {
            entries.add(CASINO_CHIP);
            entries.add(CHIP_100);
            entries.add(CHIP_10);
        });
    }
}
