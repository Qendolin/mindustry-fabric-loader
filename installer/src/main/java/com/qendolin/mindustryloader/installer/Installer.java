package com.qendolin.mindustryloader.installer;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Installer extends SwingWorker<Exception, String> {
    private static final String FABRIC_LOADER_VERSION = "0.13.3";
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
        } catch (Exception ignored) {}
    }

    private void log(String s) {
        publish(s);
    }

    private void install() throws Exception {
        log(" === Installing fabric " + FABRIC_LOADER_VERSION + " === ");

        Path gamePath = locateGame();
        if(gamePath == null) {
            log("Could not find game in specified directory.");
            return;
        } else {
            log("Found game at " + gamePath.toAbsolutePath().normalize());
        }

        log("Loading library definitions");
        InputStream resource = getClass().getClassLoader().getResourceAsStream("fabric-dependencies."+FABRIC_LOADER_VERSION+".json");
        JsonObject json = JsonParser.object().from(resource);
        resource.close();
        if(json.getInt("version") != 1) throw new AssertionError("version must be 1");

        log("Using " + appdataDir);
        Files.createDirectories(Path.of(appdataDir));

        List<String> classpath = installDependencies(json);
        File argFile = Path.of(appdataDir, FABRIC_LOADER_VERSION + ".args.txt").toFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(argFile, StandardCharsets.UTF_8));
        writer.write("-cp " + String.join(System.getProperty("path.separator"), classpath));
        writer.close();

        copyStartScript(argFile.getAbsolutePath(), json.getObject("mainClass").getString("client"));

        log("Done!");
    }

    private void copyStartScript(String argFile, String mainClass) {
        try {
            String startScriptFile = isWindows() ? "mindustry_fabric.cmd" : "mindustry_fabric.sh";
            InputStream reader = getClass().getClassLoader().getResourceAsStream(startScriptFile);
            String content = new String(reader.readAllBytes(), StandardCharsets.UTF_8);

            if(isWindows()) content = content.replaceAll("\r?\n", "\r\n");
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

    private List<String> installDependencies(JsonObject json) throws Exception {
        List<String> classpath = new ArrayList<>();

        List<URL> urls = new ArrayList<>();
        JsonObject libraries = json.getObject("libraries");
        JsonArray deps = libraries.getArray("common");

        for (int i = 0; i < deps.size(); i++) {
            JsonObject dep = deps.getObject(i);
            String name = dep.getString("name");
            String repo = dep.getString("url");

            String[] parts = name.split(":");
            String group = parts[0];
            String artifact = parts[1];
            String version = parts[2];
            String base = group.replace(".", "/") + "/" + artifact + "/" + version;
            String file = artifact + "-" + version + ".jar";

            URL url = new URL(repo + base + "/" + file);
            log("Downloading dependency " + name + " from " + url);
            base = group.replace(".", "/") + "/" + artifact;
            Files.createDirectories(Path.of(appdataDir, "libraries", base));
            Path dest = Paths.get(appdataDir, "libraries", base, file);
            Files.copy(url.openStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            classpath.add(dest.toAbsolutePath().normalize().toString());
        }

        return classpath;
    }
}
