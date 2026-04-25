package com.eclipse.mixin;

import eclipse.modules.utility.LitematicaPrinter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {
    @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true)
    private void eclipse$renderPrinterProgressBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!LitematicaPrinter.shouldOverrideExperienceBar()) return;

        LitematicaPrinter.renderExperienceProgressBar(context);
        ci.cancel();
    }

    @Inject(method = "renderAddons", at = @At("HEAD"))
    private void eclipse$renderPrinterProgressText(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (LitematicaPrinter.shouldOverrideExperienceBar()) {
            LitematicaPrinter.renderExperienceProgressText(context, MinecraftClient.getInstance().textRenderer);
        }
    }
}
