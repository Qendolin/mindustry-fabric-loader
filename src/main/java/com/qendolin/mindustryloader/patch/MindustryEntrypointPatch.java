package com.qendolin.mindustryloader.patch;

import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;
import java.util.function.Function;

public class MindustryEntrypointPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassReader> classSource,
                        Consumer<ClassNode> classEmitter) {
        String entrypoint = launcher.getEntrypoint();
        Log.debug(LogCategory.GAME_PATCH,"Entrypoint is " + entrypoint);
        ClassNode mainClass = readClass(classSource.apply(entrypoint));
        if(mainClass == null) {
            throw new LinkageError ("Could not load main class " + entrypoint + "!");
        }
        Log.debug(LogCategory.GAME_PATCH, "Main class is " + mainClass);

        MethodNode mainMethod = findMethod(mainClass, (method) -> method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"));
        if(mainMethod == null) {
            throw new NoSuchMethodError("Could not find main method in " + entrypoint +  "!");
        }

        classEmitter.accept(mainClass);
    }
}
