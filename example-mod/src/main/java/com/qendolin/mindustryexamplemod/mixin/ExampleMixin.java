package com.qendolin.mindustryexamplemod.mixin;

import com.qendolin.mindustryexamplemod.MindustryExampleMod;
import mindustry.ui.fragments.MenuFragment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuFragment.class)
public class ExampleMixin {
    @Inject(at = @At("HEAD"), method = "build(Larc/scene/Group;)V")
    private void init(CallbackInfo info) {
        MindustryExampleMod.LOGGER.info("This line is printed by an example mod mixin!");
    }
}
