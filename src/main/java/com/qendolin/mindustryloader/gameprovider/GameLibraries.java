package com.qendolin.mindustryloader.gameprovider;

import com.qendolin.mindustryloader.gameprovider.services.MindustryGameProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier;

/**
 * Contains important / identifying classes.
 * Not sure about the full extent of its usages.
 */
public enum GameLibraries implements LibClassifier.LibraryType {
    MINDUSTRY_CLIENT(EnvType.CLIENT, MindustryGameProvider.CLIENT_ENTRYPOINT.replace(".", "/") + ".class"),
    MINDUSTRY_SERVER(EnvType.SERVER, MindustryGameProvider.SERVER_ENTRYPOINT.replace(".", "/") + ".class");

    private final EnvType env;
    private final String[] paths;

    GameLibraries(String path) {
        this(null, new String[] { path });
    }

    GameLibraries(String... paths) {
        this(null, paths);
    }

    GameLibraries(EnvType env, String... paths) {
        this.paths = paths;
        this.env = env;
    }

    @Override
    public boolean isApplicable(EnvType env) {
        return this.env == null || this.env == env;
    }

    @Override
    public String[] getPaths() {
        return paths;
    }
}
