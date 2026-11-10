package com.casinocraft;

import com.casinocraft.command.CasinoCommands;
import com.casinocraft.economy.ItemPrices;
import com.casinocraft.network.ModNetworking;
import com.casinocraft.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasinoCraftMod implements ModInitializer {
    public static final String MOD_ID = "casinocraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("CasinoCraft");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.register();
        ModScreenHandlers.register();
        ModNetworking.registerServerReceivers();
        CasinoCommands.register();
        ItemPrices.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ItemPrices.invalidateCache();
            ItemPrices.ensureRecipes(server.getOverworld());
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                ItemPrices.invalidateCache();
                ModNetworking.refreshPrices(server);
            }
        });

        LOGGER.info("CasinoCraft loaded: касса, слоты и рулетка на месте.");
    }
}

