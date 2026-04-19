package com.eclipse.mixin;

import eclipse.gui.EclipseToastOverlay;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Module.class, remap = false)
public abstract class ModuleToggleMixin {
    @Inject(method = "sendToggledMsg", at = @At("HEAD"), cancellable = true)
    private void eclipse$showCustomToggleNotification(CallbackInfo ci) {
        Module module = (Module) (Object) this;
        if (!Config.get().chatFeedback.get() || !module.chatFeedback) return;
        if (EclipseToastOverlay.showModuleToggle(module)) ci.cancel();
    }
}
