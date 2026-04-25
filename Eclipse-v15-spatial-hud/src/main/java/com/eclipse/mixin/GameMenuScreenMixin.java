package com.eclipse.mixin;

import eclipse.EclipseConfig;
import eclipse.gui.client.EclipseClientScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    
    @Inject(method = "init", at = @At("TAIL"))
    private void eclipse$addWorkspaceButton(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        ((ScreenAccessor) self).eclipse$addDrawableChild(ButtonWidget.builder(net.minecraft.text.Text.literal("Eclipse"), button -> net.minecraft.client.MinecraftClient.getInstance().setScreen(new EclipseClientScreen(self)))
            .dimensions(self.width - 110, 18, 92, 20)
            .build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void eclipse$renderPauseLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EclipseConfig.titleLogo()) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int logoW = Math.min(210, Math.max(128, sw / 4));
        int logoH = logoW * LOGO_H / LOGO_W;
        int x = (sw - logoW) / 2;
        int y = Math.max(8, sh / 4 - logoH - 14);

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
