package com.qendolin.mindustryloader.services;

import com.qendolin.mindustryloader.MindustryVersion;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.FileSystemUtil;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MindustryVersionLookup {
    public static MindustryVersion getVersionFromGameJar(Path jarPath) {
        MindustryVersion.Builder builder = new MindustryVersion.Builder();
        try (FileSystemUtil.FileSystemDelegate fs = FileSystemUtil.getJarFileSystem(jarPath, false)) {
            getVersionFromProperties(fs.get(), builder);
            return builder.build();
        } catch (IOException e) {
            throw ExceptionUtil.wrap(e);
        }
    }

    private static void getVersionFromProperties(FileSystem fs, MindustryVersion.Builder builder) throws IOException {
        Path file = fs.getPath("version.properties");
        if(!Files.isRegularFile(file)) {
            throw new RuntimeException("File version.properties is invalid.");
        }
        Properties props = new Properties();
        props.load(Files.newInputStream(file));

        builder.setType(props.getProperty("type"))
                .setModifier(props.getProperty("modifier"))
                .setNumber(Integer.parseInt(props.getProperty("number")));

        String build = props.getProperty("build");
        if(build.contains(".")){
            String[] parts = build.split("\\.");
            try{
                builder.setBuild(Integer.parseInt(parts[0]));
                builder.setRevision(Integer.parseInt(parts[1]));
            }catch(Throwable e){
                e.printStackTrace();
            }
        } else {
            builder.setBuild(Integer.parseInt(build));
        }
    }
}
