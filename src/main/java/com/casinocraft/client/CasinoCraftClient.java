package com.casinocraft.client;

import com.casinocraft.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class CasinoCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.CASINO, CasinoScreen::new);
        ClientNetworking.register();
    }
}
