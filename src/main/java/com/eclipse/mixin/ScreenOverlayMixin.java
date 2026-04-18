package com.eclipse.mixin;

import eclipse.gui.EclipseToastOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void eclipse$renderToastOverlay(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        EclipseToastOverlay.render(context);
    }
}
