package com.eclipse.mixin;

import eclipse.client.ui.ClientHudRenderer;
import eclipse.gui.EclipseToastOverlay;
import eclipse.modules.utility.LitematicaPrinter;
import eclipse.modules.visuals.EclipseCrosshair;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void eclipse$renderToastOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EclipseToastOverlay.render(context);
        ClientHudRenderer.render(context, MinecraftClient.getInstance());
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void eclipse$hideVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EclipseCrosshair crosshair = Modules.get().get(EclipseCrosshair.class);
        if (crosshair != null && crosshair.isActive()) ci.cancel();
    }

    @Inject(method = "shouldShowExperienceBar", at = @At("HEAD"), cancellable = true)
    private void eclipse$showPrinterProgressBar(CallbackInfoReturnable<Boolean> cir) {
        if (LitematicaPrinter.shouldOverrideExperienceBar()) cir.setReturnValue(true);
    }
}
