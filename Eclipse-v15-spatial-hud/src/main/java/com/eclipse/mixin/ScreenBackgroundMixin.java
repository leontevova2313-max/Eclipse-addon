package com.eclipse.mixin;

import eclipse.EclipseConfig;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Unique
    private static final Identifier MULTIPLAYER_BG = Identifier.of("eclipse", "textures/gui/bg_multiplayer.png");

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void eclipse$renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!EclipseConfig.screenBackgrounds()) return;

        Object self = this;

        if (self instanceof OptionsScreen) {
            return;
        }
        if (!(self instanceof MultiplayerScreen)) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                MULTIPLAYER_BG,
                0, 0,
                0.0F, 0.0F,
                sw, sh,
                1920, 1080,
                1920, 1080
        );

        int dim = Math.max(0, Math.min(220, EclipseConfig.backgroundDimStrength()));
        if (dim > 0) context.fill(0, 0, sw, sh, dim << 24);

        ci.cancel();
    }
}
