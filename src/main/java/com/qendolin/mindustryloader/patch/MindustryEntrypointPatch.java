package com.qendolin.mindustryloader.patch;

import com.qendolin.mindustryloader.services.MindustryGameProvider;
import com.qendolin.mindustryloader.services.MindustryHooks;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.points.BeforeFinalReturn;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class MindustryEntrypointPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassReader> classSource,
                        Consumer<ClassNode> classEmitter) {
        String entrypoint = launcher.getEntrypoint();
        Log.debug(LogCategory.GAME_PATCH,"Entrypoint is " + entrypoint);
        ClassNode entrypointClazz = readClass(classSource.apply(entrypoint));
        if(entrypointClazz == null) {
            throw new LinkageError ("Could not load entrypoint class " + entrypoint + "!");
        }
        Log.debug(LogCategory.GAME_PATCH, "Entrypoint class is " + entrypointClazz);

        if(entrypoint.equals(MindustryGameProvider.CLIENT_ENTRYPOINT)) {
            injectClientHook(entrypointClazz);
        } else if(entrypoint.equals(MindustryGameProvider.SERVER_ENTRYPOINT)) {
            injectServerHook(entrypointClazz);
        } else {
            throw new IllegalArgumentException("Unknown entrypoint " + entrypoint + ".");
        }

        classEmitter.accept(entrypointClazz);
    }

    private void injectClientHook(ClassNode entrypoint) {
        MethodNode initMethod = findMethod(entrypoint, (method) -> method.name.equals("init") && method.desc.equals("()V"));
        if(initMethod == null) {
            throw new NoSuchMethodError("Could not find init method in " + entrypoint + ".");
        }

        injectTailInsn(initMethod, new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                MindustryHooks.INTERNAL_NAME,
                "initClient",
                "()V",
                false));
    }

    private void injectServerHook(ClassNode entrypoint) {
        MethodNode initMethod = findMethod(entrypoint, (method) -> method.name.equals("init") && method.desc.equals("()V"));
        if(initMethod == null) {
            throw new NoSuchMethodError("Could not find init method in " + entrypoint + ".");
        }

        injectTailInsn(initMethod, new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                MindustryHooks.INTERNAL_NAME,
                "initServer",
                "()V",
                false));
    }

    /**
     * @see org.spongepowered.asm.mixin.injection.points.BeforeFinalReturn#find
     */
    private static void injectTailInsn(MethodNode method, AbstractInsnNode injectedInsn) {
        AbstractInsnNode ret = null;

        // RETURN opcode varies based on return type, thus we calculate what opcode we're actually looking for by inspecting the target method
        int returnOpcode = Type.getReturnType(method.desc).getOpcode(Opcodes.IRETURN);
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode) {
                ret = insn;
            }
        }

        // WAT?
        if (ret == null) {
            throw new RuntimeException("TAIL could not locate a valid RETURN in the target method!");
        }

        method.instructions.insertBefore(ret, injectedInsn);
    }
}
