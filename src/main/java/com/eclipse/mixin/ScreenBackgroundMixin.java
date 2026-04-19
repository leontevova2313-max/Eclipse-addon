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
    private static final Identifier DEFAULT_BG = Identifier.of("eclipse", "textures/gui/bg.png");

    @Unique
    private static final Identifier OPTIONS_BG = Identifier.of("eclipse", "textures/gui/bg_options.png");

    @Unique
    private static final Identifier MULTIPLAYER_BG = Identifier.of("eclipse", "textures/gui/bg_multiplayer.png");

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void eclipse$renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!EclipseConfig.screenBackgrounds()) return;

        Object self = this;

        Identifier background;
        if (self instanceof MultiplayerScreen) {
            background = MULTIPLAYER_BG;
        } else if (self instanceof OptionsScreen) {
            background = OPTIONS_BG;
        } else if (EclipseConfig.allScreenBackgrounds()) {
            background = DEFAULT_BG;
        } else {
            return;
        }

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                background,
                0, 0,
                0.0F, 0.0F,
                sw, sh,
                1920, 1080,
                1920, 1080
        );

        int dim = EclipseConfig.backgroundDim();
        if (dim > 0) {
            context.fill(0, 0, sw, sh, (Math.min(220, dim) << 24) | 0x00000610);
        }

        ci.cancel();
    }
}
