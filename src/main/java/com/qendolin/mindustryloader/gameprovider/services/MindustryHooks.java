package com.qendolin.mindustryloader.gameprovider.services;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class MindustryHooks {
    public static final String INTERNAL_NAME = MindustryHooks.class.getName().replace('.', '/');

    public static void initClient() {
        Path runDir = Paths.get(".");
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.prepareModInit(runDir, FabricLoaderImpl.INSTANCE.getGameInstance());
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        loader.invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
    }

    public static void initServer() {
        Path runDir = Paths.get(".");
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.prepareModInit(runDir, FabricLoaderImpl.INSTANCE.getGameInstance());
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        loader.invokeEntrypoints("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
    }

    public static String insertBranding(final String brand) {
        String fabricBrand = "Modded (fabric " + FabricLoaderImpl.VERSION + ")";
        if (brand == null || brand.isEmpty()) {
            Log.warn(LogCategory.GAME_PROVIDER, "Null or empty branding found!", new IllegalStateException());
            return fabricBrand;
        }

        return brand + "\n" + fabricBrand;
    }
}
