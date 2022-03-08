package com.qendolin.mindustryloader.gameprovider.services;

import com.qendolin.mindustryloader.gameprovider.MindustryVersion;
import com.qendolin.mindustryloader.gameprovider.patch.BrandingPatch;
import com.qendolin.mindustryloader.gameprovider.patch.MindustryEntrypointPatch;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MindustryGameProvider implements GameProvider {

    public static final String CLIENT_ENTRYPOINT = "mindustry.ClientLauncher";
    public static final String CLIENT_MAIN = "mindustry.desktop.DesktopLauncher";
    public static final String SERVER_ENTRYPOINT = "mindustry.server.ServerLauncher";
    public static final String SERVER_MAIN = "mindustry.server.ServerLauncher";
    private static final String[] ENTRYPOINTS = new String[]{
            CLIENT_ENTRYPOINT, SERVER_ENTRYPOINT
    };
    private static final Set<String> SENSITIVE_ARGS = new HashSet<>(Arrays.asList(
            // all lowercase without --
            "savedir",
            "debug",
            "localclient"));

    private Arguments arguments;
    private String entrypoint;
    private Path gameJar;
    private MindustryVersion gameVersion;

    private static final GameTransformer TRANSFORMER = new GameTransformer(
            new MindustryEntrypointPatch(),
            new BrandingPatch());

    public MindustryVersion getGameVersion() {
        return gameVersion;
    }

    @Override
    public String getGameId() {
        return "mindustry";
    }

    @Override
    public String getGameName() {
        return "Mindustry";
    }

    @Override
    public String getRawGameVersion() {
        if (gameVersion == null) return "0.0.0";
        return gameVersion.toString();
    }

    @Override
    public String getNormalizedGameVersion() {
        if (gameVersion == null) return "0.0.0";
        return gameVersion.toStringVersion().getFriendlyString();
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        HashMap<String, String> contactInfo = new HashMap<>();
        contactInfo.put("homepage", "https://mindustrygame.github.io/");

        BuiltinModMetadata.Builder mindustryMetaData =
                new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                        .setName(getGameName())
                        .addAuthor("Anuke", contactInfo)
                        .setContact(new ContactInformationImpl(contactInfo))
                        .setDescription("A sandbox tower-defense game.");

        return Collections.singletonList(new BuiltinMod(Collections.singletonList(gameJar), mindustryMetaData.build()));
    }

    @Override
    public String getEntrypoint() {
        return entrypoint;
    }

    @Override
    public Path getLaunchDirectory() {
        if (arguments == null) {
            return Paths.get(".");
        }

        return getLaunchDirectory(arguments);
    }

    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        arguments = new Arguments();
        arguments.parse(args);

        List<String> gameLocations = new ArrayList<>();
        if(System.getProperty(SystemProperties.GAME_JAR_PATH) != null) {
            gameLocations.add(System.getProperty(SystemProperties.GAME_JAR_PATH));
        }
        gameLocations.add("./jre/desktop.jar");
        gameLocations.add("./Mindustry.jar");
        gameLocations.add("./desktop-release.jar");
        gameLocations.add("./desktop.jar");

        List<Path> jarPaths = gameLocations.stream()
                .map(path -> Paths.get(path).toAbsolutePath().normalize())
                .filter(Files::exists).toList();
        GameProviderHelper.FindResult result = GameProviderHelper.findFirst(jarPaths, new HashMap<>(), true, ENTRYPOINTS);

        if(result == null || result.path == null) {
            Log.error(LogCategory.GAME_PROVIDER, "Could not locate game. Looked at: \n" + gameLocations.stream()
                    .map(path -> " - " + Paths.get(path).toAbsolutePath().normalize())
                    .collect(Collectors.joining("\n")));
            return false;
        }

        entrypoint = result.name;
        gameJar = result.path;
        gameVersion = MindustryVersionLookup.getVersionFromGameJar(gameJar);
        processArgumentMap(arguments);
        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        TRANSFORMER.locateEntrypoints(launcher, gameJar);
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        launcher.addToClassPath(gameJar);
    }

    @Override
    public void launch(ClassLoader loader) {
        String targetClass;

        if(entrypoint.equals(CLIENT_ENTRYPOINT)) {
            targetClass = CLIENT_MAIN;
        } else if(entrypoint.equals(SERVER_ENTRYPOINT)) {
            targetClass = SERVER_MAIN;
        } else {
            throw new RuntimeException("Unknown entrypoint " + entrypoint + ".");
        }

        try {
            Class<?> c = loader.loadClass(targetClass);
            Method m = c.getMethod("main", String[].class);
            m.invoke(null, (Object) arguments.toArray());
        }
        catch(InvocationTargetException e) {
            throw new FormattedException("Mindustry has crashed!", e.getCause());
        }
        catch(ReflectiveOperationException e) {
            throw new FormattedException("Failed to start Mindustry", e);
        }
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        if (arguments == null) return new String[0];

        String[] ret = arguments.toArray();
        if (!sanitize) return ret;

        int writeIdx = 0;

        for (int i = 0; i < ret.length; i++) {
            String arg = ret[i];

            if (i + 1 < ret.length
                    && arg.startsWith("--")
                    && SENSITIVE_ARGS.contains(arg.substring(2).toLowerCase(Locale.ENGLISH))) {
                i++; // skip value
            } else {
                ret[writeIdx++] = arg;
            }
        }

        if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);
        return ret;
    }

    private void processArgumentMap(Arguments arguments) {
        if (!arguments.containsKey("gameDir")) {
            arguments.put("gameDir", getLaunchDirectory(arguments).toAbsolutePath().normalize().toString());
        }

        Path launchDir = Path.of(arguments.get("gameDir"));
        Log.debug(LogCategory.GAME_PROVIDER, "Launch directory is " + launchDir);
    }

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault("gameDir", "."));
    }
}
