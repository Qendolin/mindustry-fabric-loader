package com.qendolin.mindustryexamplemod;

import com.qendolin.mindustryloader.gameprovider.services.LoggerFactory;
import net.fabricmc.api.ModInitializer;

import java.util.logging.Logger;

public class MindustryExampleMod implements ModInitializer {
    public static final String MODID = "mindustry-example-mod";

    // This logger is used to write text to the console and the log file.
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Mindustry has finished its setup.
        LOGGER.info("Hello Fabric world!");
    }
}
