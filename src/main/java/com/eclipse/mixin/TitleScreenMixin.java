package com.eclipse.mixin;

import eclipse.EclipseConfig;
import eclipse.gui.ConstellationLogoRenderer;
import eclipse.gui.EclipseCustomizationScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique
    private static final int TEX_W = 960;

    @Unique
    private static final int TEX_H = 540;

    @Unique
    private static final Identifier[] FRAMES = new Identifier[] {
        Identifier.of("eclipse", "textures/gui/title/planet_0.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_1.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_2.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_3.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_4.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_5.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_6.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_7.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_8.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_9.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_10.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_11.png"),
        Identifier.of("eclipse", "textures/gui/title/planet_12.png")
    };

    @Inject(method = "init", at = @At("TAIL"))
    private void eclipse$addCustomizationButton(CallbackInfo ci) {
        TitleScreen self = (TitleScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        ((ScreenAccessor) self).eclipse$addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.menu.customization"), button ->
            client.setScreen(new EclipseCustomizationScreen(self))
        ).dimensions(width / 2 - 100, height / 4 + 156, 200, 20).build());
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void eclipse$renderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EclipseConfig.titleBackground()) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        int frameA = 0;
        int frameB = 1;
        float fade = 0.0F;

        if (EclipseConfig.titleAnimation()) {
            int frameTime = EclipseConfig.frameTimeMs();
            long now = System.currentTimeMillis();
            float phase = (now % (FRAMES.length * frameTime)) / (float) frameTime;
            frameA = ((int) phase) % FRAMES.length;
            frameB = (frameA + 1) % FRAMES.length;
            fade = phase - (int) phase;
            fade = fade * fade * (3.0F - 2.0F * fade);
        }

        int colorA = EclipseConfig.titleCrossfade()
            ? ((((int) ((1.0F - fade) * 255.0F)) << 24) | 0x00FFFFFF)
            : 0xFFFFFFFF;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            FRAMES[frameA],
            0, 0,
            0.0F, 0.0F,
            sw, sh,
            TEX_W, TEX_H,
            TEX_W, TEX_H,
            colorA
        );

        if (EclipseConfig.titleCrossfade()) {
            int colorB = (((int) (fade * 255.0F)) << 24) | 0x00FFFFFF;

            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                FRAMES[frameB],
                0, 0,
                0.0F, 0.0F,
                sw, sh,
                TEX_W, TEX_H,
                TEX_W, TEX_H,
                colorB
            );
        }

        int dim = EclipseConfig.backgroundDim();
        if (dim > 0) {
            context.fill(0, 0, sw, sh, (Math.min(220, dim) << 24) | 0x00000610);
        }

        ci.cancel();
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
        )
    )
    private void eclipse$skipMinecraftLogo(LogoDrawer logoDrawer, DrawContext context, int screenWidth, float alpha) {
        if (!EclipseConfig.titleLogo()) {
            logoDrawer.draw(context, screenWidth, alpha);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void eclipse$renderLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EclipseConfig.titleLogo()) return;

        int sw = context.getScaledWindowWidth();
        int logoW = Math.min(EclipseConfig.logoWidth(), Math.max(180, sw - 40));
        int logoH = ConstellationLogoRenderer.height(logoW);
        int x = (sw - logoW) / 2;
        int y = EclipseConfig.logoY();

        ConstellationLogoRenderer.render(
            context,
            "ECLIPSE",
            x,
            y + Math.max(0, 36 - logoH / 3),
            logoW,
            EclipseConfig.logoColor(),
            EclipseConfig.logoGlowColor(),
            EclipseConfig.logoStarSize(),
            EclipseConfig.logoLineAlpha(),
            EclipseConfig.logoTwinkle()
        );
    }
}
