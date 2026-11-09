package com.casinocraft.screen;

import com.casinocraft.CasinoCraftMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
    public static final ScreenHandlerType<CasinoScreenHandler> CASINO = Registry.register(
            Registries.SCREEN_HANDLER,
            CasinoCraftMod.id("casino"),
            new ScreenHandlerType<>(CasinoScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    private ModScreenHandlers() {
    }

    public static void register() {
        // вызов нужен, чтобы класс инициализировался при старте мода
    }
}
