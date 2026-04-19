package com.eclipse.mixin;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Unique
    private static final int LOGO_W = 1213;

    @Unique
    private static final int LOGO_H = 587;

    @Unique
    private static final Identifier LOGO = Identifier.of("eclipse", "textures/gui/title/eclipse_logo.png");

    @Inject(method = "render", at = @At("TAIL"))
    private void eclipse$renderPauseLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int sw = context.getScaledWindowWidth();
        int logoW = Math.min(210, Math.max(128, sw / 4));
        int logoH = logoW * LOGO_H / LOGO_W;
        int x = 12;
        int y = 10;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            LOGO,
            x, y,
            0.0F, 0.0F,
            logoW, logoH,
            LOGO_W, LOGO_H,
            LOGO_W, LOGO_H
        );
    }
}
