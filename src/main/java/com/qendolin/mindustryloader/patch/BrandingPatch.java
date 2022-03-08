package com.qendolin.mindustryloader.patch;

import com.qendolin.mindustryloader.services.MindustryHooks;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class BrandingPatch extends GamePatch {

    private static final int VAR_INDEX = 3;

    @Override
    public void process(FabricLauncher launcher, Function<String, ClassReader> classSource, Consumer<ClassNode> classEmitter) {
        ClassNode menuClass = readClass(classSource.apply("mindustry.ui.fragments.MenuFragment"));
        boolean applied = false;
        for (MethodNode node : menuClass.methods) {
            if (node.name.equals("build") && node.desc.equals("(Larc/scene/Group;)V")) {
                Log.debug(LogCategory.GAME_PATCH, "Applying brand name hook to %s::%s", menuClass.name, node.name);

                ListIterator<AbstractInsnNode> it = node.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (insn.getOpcode() == Opcodes.ASTORE && insn instanceof VarInsnNode varInsn && varInsn.var == VAR_INDEX) {
                        it.add(new VarInsnNode(Opcodes.ALOAD, VAR_INDEX));
                        it.add(new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                MindustryHooks.INTERNAL_NAME,
                                "insertBranding",
                                "(Ljava/lang/String;)Ljava/lang/String;",
                                false));
                        it.add(new VarInsnNode(Opcodes.ASTORE, VAR_INDEX));
                        applied = true;
                        break;
                    }
                }
            }
        }
        if(applied) {
            classEmitter.accept(menuClass);
        } else {
            Log.warn(LogCategory.GAME_PATCH, "Failed to apply brand name. Instruction not found.");
        }
    }
}
