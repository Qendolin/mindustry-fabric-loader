package com.qendolin.mindustryloader.installer;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class Installer extends SwingWorker<Exception, String> {
    private static final int VERSION = 1;
    private final String appdataDir;
    private final String gameDir;

    public Consumer<String> onLog;
    public Consumer<Exception> onDone;

    public Installer(String gameDir) {
        this.gameDir = gameDir;
        AppDirs appDirs = AppDirsFactory.getInstance();
        appdataDir = Path.of(appDirs.getUserDataDir("Mindustry_Fabric", null, null)).toString();
    }

    @Override
    protected Exception doInBackground() {
        try {
            install();
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    private void install() throws Exception {
        log(" === Installing fabric === ");

        Path gamePath = locateGame();
        if (gamePath == null) {
            log("Could not find game in specified directory.");
            return;
        } else {
            log("Found game at " + gamePath.toAbsolutePath().normalize());
        }

        log("Checking out stable version");
        InstallConfig config = loadConfig();
        log("Loader version " + config.fabricVersion + ", Provider version" + config.providerVersion);

        log("Using " + appdataDir);
        Files.createDirectories(Path.of(appdataDir));

        List<String> classpath = installDependencies(new ArrayList<>(config.clientDependencies.values()));
        File argFile = Path.of(appdataDir, config.providerVersion + ".args.txt").toFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(argFile, StandardCharsets.UTF_8));
        writer.write("-cp " + String.join(System.getProperty("path.separator"), classpath));
        writer.close();

        log("Copying start script to " + argFile.getAbsolutePath());
        copyStartScript(argFile.getAbsolutePath(), config.mainClientClass);

        log("Done!");
    }

    private void log(String s) {
        publish(s);
    }

    private Path locateGame() {
        List<String> locations = new ArrayList<>();
        locations.add("./jre/desktop.jar");
        locations.add("./Mindustry.jar");
        locations.add("./desktop-release.jar");
        locations.add("./desktop.jar");
        return locations.stream().map(path -> Path.of(gameDir, path))
                .filter(Files::isRegularFile)
                .findFirst().orElse(null);
    }

    private InstallConfig loadConfig() throws JsonParserException, IOException {
        String rawJson;
        if(MainView.overrideConfigPath != null && !MainView.overrideConfigPath.isBlank()) {
            rawJson = Files.readString(Paths.get(MainView.overrideConfigPath));
        } else {
            rawJson = IOUtils.toString(new URL("https://raw.githubusercontent.com/Qendolin/mindustry-fabric-loader/stable/installer/src/main/resources/fabric-dependencies.json"), StandardCharsets.UTF_8);
        }
        JsonObject json = JsonParser.object().from(rawJson);

        return InstallConfig.from(json);
    }

    private List<String> installDependencies(List<Dependency> dependencies) throws Exception {
        List<String> classpath = new ArrayList<>();

        for (Dependency dep : dependencies) {
            log("Downloading dependency " + dep.definition + " from " + dep.url);
            Files.createDirectories(Path.of(appdataDir, "libraries", dep.dir));
            Path dest = Paths.get(appdataDir, "libraries", dep.dir, dep.artifact);

            FileUtils.copyURLToFile(new URL(dep.url), dest.toFile());
            classpath.add(dest.toAbsolutePath().normalize().toString());
        }

        return classpath;
    }

    private void copyStartScript(String argFile, String mainClass) {
        try {
            String startScriptFile = isWindows() ? "mindustry_fabric.cmd" : "mindustry_fabric.sh";
            InputStream reader = getClass().getClassLoader().getResourceAsStream(startScriptFile);
            String content = new String(reader.readAllBytes(), StandardCharsets.UTF_8);

            if (isWindows()) content = content.replaceAll("\r?\n", "\r\n");
            else content = content.replaceAll("\r?\n", "\n");

            content = content.replace("{{ARG_FILE}}", argFile).
                    replace("{{MAIN_CLASS}}", mainClass)
                    .replace("{{ENV_SIDE}}", "client");

            BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(gameDir, startScriptFile).toFile()));
            writer.write(content);
            writer.close();
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    @Override
    protected void process(List<String> chunks) {
        super.process(chunks);
        for (String chunk : chunks) {
            onLog.accept(chunk);
        }
    }

    @Override
    protected void done() {
        super.done();
        try {
            onDone.accept(get());
        } catch (Exception ignored) {
        }
    }

    static class InstallConfig {
        public final String mainClientClass;
        public final String mainServerClass;

        public final String fabricVersion;
        public final String providerVersion;

        public final Map<String, Dependency> clientDependencies;
        public final Map<String, Dependency> serverDependencies;

        InstallConfig(String mainClientClass, String mainServerClass, String fabricVersion, String providerVersion, Map<String, Dependency> clientDependencies, Map<String, Dependency> serverDependencies) {
            this.mainClientClass = mainClientClass;
            this.mainServerClass = mainServerClass;
            this.fabricVersion = fabricVersion;
            this.providerVersion = providerVersion;
            this.clientDependencies = clientDependencies;
            this.serverDependencies = serverDependencies;
        }

        public static InstallConfig from(JsonObject json) {
            int version = json.getInt("version");

            if (json.getInt("version") > VERSION)
                throw new AssertionError("The installer version is too old");
            if (json.getInt("version") < VERSION)
                throw new AssertionError("The installer version is too new");

            JsonObject mainClasses = json.getObject("mainClass");
            String mainClientClass = mainClasses.getString("client");
            String mainServerClass = mainClasses.getString("server");

            Map<String, Dependency> clientDepMap = new HashMap<>();
            Map<String, Dependency> serverDepMap = new HashMap<>();

            JsonObject libraries = json.getObject("libraries");
            JsonArray commonDeps = libraries.getArray("common");
            JsonArray clientDeps = libraries.getArray("client");
            JsonArray serverDeps = libraries.getArray("server");

            loadDeps(commonDeps, clientDepMap);
            loadDeps(clientDeps, clientDepMap);
            loadDeps(commonDeps, serverDepMap);
            loadDeps(serverDeps, serverDepMap);

            Dependency serverFabricDep = serverDepMap.get("net.fabricmc:fabric-loader");
            Dependency clientFabricDep = clientDepMap.get("net.fabricmc:fabric-loader");
            if(!serverFabricDep.equals(clientFabricDep))
                throw new AssertionError("Client and Server fabric-loader dependency mismatch");

            Dependency serverProviderDep = serverDepMap.get("com.github.Qendolin:mindustry-fabric-loader");
            Dependency clientProviderDep = clientDepMap.get("com.github.Qendolin:mindustry-fabric-loader");
            if(!serverProviderDep.equals(clientProviderDep))
                throw new AssertionError("Client and Server mindustry-fabric-loader dependency mismatch");

            return new InstallConfig(mainClientClass, mainServerClass, serverFabricDep.version, serverProviderDep.version, clientDepMap, serverDepMap);
        }

        protected static void loadDeps(JsonArray deps, Map<String, Dependency> depMap) {
            for (int i = 0; i < deps.size(); i++) {
                JsonObject object = deps.getObject(i);
                String def = object.getString("name");
                String repo = object.getString("url");
                Dependency dep = new Dependency(repo, def);
                depMap.put(dep.group + ":" + dep.name, dep);
            }
        }
    }

    static class Dependency {
        public final String definition;
        public final String repo;
        public final String group;
        public final String name;
        public final String version;

        public final String artifact;
        public final String url;
        public final String dir;

        Dependency(String repo, String definition) {
            this.repo = repo;
            this.definition = definition;

            String[] parts = definition.split(":");
            this.group = parts[0];
            this.name = parts[1];
            this.version = parts[2];

            this.artifact = name + "-" + version + ".jar";
            this.url = repo + Path.of(group.replace(".", "/"), name, version, artifact)
                    .toString().replace("\\", "/");
            this.dir = Path.of(group.replace(".", "/"), name).toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return definition.equals(that.definition) && repo.equals(that.repo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(definition, repo);
        }
    }
}
