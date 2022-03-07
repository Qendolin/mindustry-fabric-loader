package com.qendolin.mindustryloader.services;

import com.qendolin.mindustryloader.MindustryVersion;
import com.qendolin.mindustryloader.patch.BrandingPatch;
import com.qendolin.mindustryloader.patch.MindustryEntrypointPatch;
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
import java.util.zip.ZipFile;

public class MindustryGameProvider implements GameProvider {

    private static final String[] ENTRYPOINTS = new String[]{"mindustry.desktop.DesktopLauncher", "mindustry.server.mindustry.server"};
    private static final Set<String> SENSITIVE_ARGS = new HashSet<>(Arrays.asList(
            // all lowercase without --
            "savedir",
            "debug",
            "localclient"));

    private Arguments arguments;
    private String entrypoint;
    private Path launchDir;
    private Path libDir;
    private Path gameJar;
    private boolean development = false;
    private final List<Path> miscGameLibraries = new ArrayList<>();
    private static MindustryVersion gameVersion;

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

        BuiltinModMetadata.Builder minicraftMetaData =
                new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                        .setName(getGameName())
                        .addAuthor("Anuke", contactInfo)
                        .setContact(new ContactInformationImpl(contactInfo))
                        .setDescription("A sandbox tower-defense game.");

        return Collections.singletonList(new BuiltinMod(Collections.singletonList(gameJar), minicraftMetaData.build()));
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
        this.arguments = new Arguments();
        arguments.parse(args);

        Map<Path, ZipFile> zipFiles = new HashMap<>();

        if(Objects.equals(System.getProperty(SystemProperties.DEVELOPMENT), "true")) {
            development = true;
        }

        try {
            String gameJarProperty = System.getProperty(SystemProperties.GAME_JAR_PATH);
            GameProviderHelper.FindResult result = null;
            if(gameJarProperty == null) {
                gameJarProperty = "./jre/desktop.jar";
            }
            if(gameJarProperty != null) {
                Path path = Paths.get(gameJarProperty);
                if (!Files.exists(path)) {
                    throw new RuntimeException("Game jar configured through " + SystemProperties.GAME_JAR_PATH + " system property doesn't exist");
                }

                result = GameProviderHelper.findFirst(Collections.singletonList(path), zipFiles, true, ENTRYPOINTS);
            }

            if(result == null) {
                return false;
            }

            entrypoint = result.name;
            gameJar = result.path;

        } catch (Exception e) {
            e.printStackTrace();
        }

        processArgumentMap(arguments);

        try {
            gameVersion = MindustryVersionLookup.getVersionFromGameJar(gameJar);
        } catch (Exception e) {
            e.printStackTrace();
        }


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

        for(Path lib : miscGameLibraries) {
            launcher.addToClassPath(lib);
        }
    }

    @Override
    public void launch(ClassLoader loader) {
        String targetClass = entrypoint;

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

        launchDir = Path.of(arguments.get("gameDir"));
        Log.debug(LogCategory.GAME_PROVIDER, "Launch directory is " + launchDir);
        libDir = launchDir.resolve(Path.of("./lib"));
    }

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault("gameDir", "."));
    }
}
