package com.eclipse.mixin;

import eclipse.EclipseConfig;
import eclipse.gui.EclipseCustomizationScreen;
import eclipse.gui.TitleLogoLayout;
import eclipse.skins.SkinCustomizationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique
    private static final int TEX_W = 960;

    @Unique
    private static final int TEX_H = 540;

    @Unique
    private static final int LOGO_W = TitleLogoLayout.LOGO_TEXTURE_WIDTH;

    @Unique
    private static final int LOGO_H = TitleLogoLayout.LOGO_TEXTURE_HEIGHT;

    @Unique
    private static final Identifier LOGO = Identifier.of("eclipse", "textures/gui/title/eclipse_logo.png");

    @Shadow
    private SplashTextRenderer splashText;

    @Unique
    private PlayerSkinWidget eclipse$skinWidget;

    @Unique
    private int eclipse$titleButtonRows = 6;

    @Unique
    private static final Identifier TRANSITION_GLOW = Identifier.of("eclipse", "textures/gui/title/transition_glow.png");

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
        SkinCustomizationManager.load();
        if (EclipseConfig.titleLogo()) splashText = null;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        if (EclipseConfig.titleLogo()) {
            eclipse$layoutTitleButtons(self);
        }

        int modelW = 76;
        int modelH = 116;
        int modelX = 22;
        int modelY = Math.max(42, height / 4 + 4);
        eclipse$skinWidget = new PlayerSkinWidget(modelW, modelH, client.getLoadedEntityModels(), SkinCustomizationManager::currentSkinTextures);
        eclipse$skinWidget.setX(modelX);
        eclipse$skinWidget.setY(modelY);
        ((ScreenAccessor) self).eclipse$addDrawableChild(eclipse$skinWidget);

        ((ScreenAccessor) self).eclipse$addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.menu.customization"), button ->
            client.setScreen(new EclipseCustomizationScreen(self))
        ).dimensions(modelX - 4, modelY + modelH + 8, modelW + 8, 20).build());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void eclipse$disableVanillaSplash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (EclipseConfig.titleLogo()) splashText = null;
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void eclipse$renderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EclipseConfig.titleBackground()) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int parallaxX = EclipseConfig.titleParallax() ? (mouseX - sw / 2) / 90 : 0;
        int parallaxY = EclipseConfig.titleParallax() ? (mouseY - sh / 2) / 120 : 0;

        if (EclipseConfig.customTitleBackground()) {
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                EclipseConfig.customTitleBackgroundTexture(),
                -Math.abs(parallaxX), -Math.abs(parallaxY),
                0.0F, 0.0F,
                sw + Math.abs(parallaxX) * 2, sh + Math.abs(parallaxY) * 2,
                EclipseConfig.customTitleBackgroundWidth(),
                EclipseConfig.customTitleBackgroundHeight(),
                EclipseConfig.customTitleBackgroundWidth(),
                EclipseConfig.customTitleBackgroundHeight()
            );
            eclipse$renderTitleColorOverlays(context);
            eclipse$renderTransitionGlow(context);
            ci.cancel();
            return;
        }

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
            if (EclipseConfig.titleSmoothInterpolation()) fade = EclipseConfig.ease(fade);
        }

        int colorA = EclipseConfig.titleCrossfade()
            ? ((((int) ((1.0F - fade) * 255.0F)) << 24) | 0x00FFFFFF)
            : 0xFFFFFFFF;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            FRAMES[frameA],
            -Math.abs(parallaxX), -Math.abs(parallaxY),
            0.0F, 0.0F,
            sw + Math.abs(parallaxX) * 2, sh + Math.abs(parallaxY) * 2,
            TEX_W, TEX_H,
            TEX_W, TEX_H,
            colorA
        );

        if (EclipseConfig.titleCrossfade()) {
            int colorB = (((int) (fade * 255.0F)) << 24) | 0x00FFFFFF;

            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                FRAMES[frameB],
                -Math.abs(parallaxX), -Math.abs(parallaxY),
                0.0F, 0.0F,
                sw + Math.abs(parallaxX) * 2, sh + Math.abs(parallaxY) * 2,
                TEX_W, TEX_H,
                TEX_W, TEX_H,
                colorB
            );
        }

        eclipse$renderTitleColorOverlays(context);
        eclipse$renderTransitionGlow(context);
        ci.cancel();
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void eclipse$renderDisabledBackgroundGlow(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (EclipseConfig.titleBackground()) return;
        eclipse$renderTransitionGlow(context);
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
        eclipse$renderSkinDisplayName(context);

        TitleLogoLayout.Bounds layout = TitleLogoLayout.calculate(
            context.getScaledWindowWidth(),
            context.getScaledWindowHeight(),
            EclipseConfig.logoWidth(),
            eclipse$titleButtonRows
        );

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            LOGO,
            eclipse$animatedLogoX(layout), eclipse$animatedLogoY(layout),
            0.0F, 0.0F,
            eclipse$animatedLogoWidth(layout), eclipse$animatedLogoHeight(layout),
            LOGO_W, LOGO_H,
            LOGO_W, LOGO_H,
            EclipseConfig.withAlpha(0x00FFFFFF, eclipse$logoAlpha())
        );

    }

    @Unique
    private void eclipse$renderSkinDisplayName(DrawContext context) {
        if (eclipse$skinWidget == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        String name = client.textRenderer.trimToWidth(SkinCustomizationManager.displayName(), Math.max(80, eclipse$skinWidget.getWidth() + 48));
        int textWidth = client.textRenderer.getWidth(name);
        int centerX = eclipse$skinWidget.getX() + eclipse$skinWidget.getWidth() / 2;
        centerX = Math.max(textWidth / 2 + 6, Math.min(context.getScaledWindowWidth() - textWidth / 2 - 6, centerX));
        int y = Math.max(8, eclipse$skinWidget.getY() - 14);
        context.drawCenteredTextWithShadow(client.textRenderer, name, centerX, y, 0xFFEAF7F2);
    }

    @Unique
    private int eclipse$animatedLogoWidth(TitleLogoLayout.Bounds layout) {
        return layout.logoWidth() + eclipse$logoPulsePixels(layout.logoWidth());
    }

    @Unique
    private int eclipse$animatedLogoHeight(TitleLogoLayout.Bounds layout) {
        return layout.logoHeight() + eclipse$logoPulsePixels(layout.logoHeight());
    }

    @Unique
    private int eclipse$animatedLogoX(TitleLogoLayout.Bounds layout) {
        return layout.logoX() - eclipse$logoPulsePixels(layout.logoWidth()) / 2;
    }

    @Unique
    private int eclipse$animatedLogoY(TitleLogoLayout.Bounds layout) {
        return layout.logoY() - eclipse$logoPulsePixels(layout.logoHeight()) / 2;
    }

    @Unique
    private int eclipse$logoPulsePixels(int base) {
        if (EclipseConfig.logoAnimation() != EclipseConfig.LogoAnimation.Pulse) return 0;
        double wave = (Math.sin(System.currentTimeMillis() / 520.0 * Math.max(0.1, EclipseConfig.globalAnimationSpeed())) + 1.0) * 0.5;
        return (int) Math.round(base * 0.025 * wave);
    }

    @Unique
    private int eclipse$logoAlpha() {
        int alpha = EclipseConfig.logoOpacity();
        if (EclipseConfig.logoAnimation() == EclipseConfig.LogoAnimation.Fade) {
            double wave = (Math.sin(System.currentTimeMillis() / 650.0 * Math.max(0.1, EclipseConfig.globalAnimationSpeed())) + 1.0) * 0.5;
            alpha = (int) Math.round(alpha * (0.72 + wave * 0.28));
        }
        return Math.max(0, Math.min(255, alpha));
    }

    @Unique
    private void eclipse$layoutTitleButtons(TitleScreen screen) {
        List<ClickableWidget> widgets = new ArrayList<>();
        for (Element element : screen.children()) {
            if (element instanceof ClickableWidget widget && eclipse$isCentralTitleButton(widget)) {
                widgets.add(widget);
            }
        }

        if (widgets.isEmpty()) return;

        TreeMap<Integer, List<ClickableWidget>> rows = new TreeMap<>();
        for (ClickableWidget widget : widgets) {
            rows.computeIfAbsent(widget.getY(), ignored -> new ArrayList<>()).add(widget);
        }

        eclipse$titleButtonRows = rows.size();
        TitleLogoLayout.Bounds layout = TitleLogoLayout.calculate(
            screen.width,
            screen.height,
            EclipseConfig.logoWidth(),
            eclipse$titleButtonRows
        );

        int row = 0;
        for (List<ClickableWidget> rowWidgets : rows.values()) {
            rowWidgets.sort(Comparator.comparingInt(ClickableWidget::getX));
            int y = layout.rowY(row++);
            for (ClickableWidget widget : rowWidgets) {
                widget.setY(y);
            }
        }
    }

    @Unique
    private boolean eclipse$isCentralTitleButton(ClickableWidget widget) {
        int center = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
        int widgetCenter = widget.getX() + widget.getWidth() / 2;
        return Math.abs(widgetCenter - center) <= 130 && widget.getY() >= 70 && widget.getY() < MinecraftClient.getInstance().getWindow().getScaledHeight() - 24;
    }

    @Unique
    private void eclipse$renderTransitionGlow(DrawContext context) {
        float transition = EclipseConfig.titleBackgroundTransition();
        if (transition <= 0.0F) return;

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int size = (int) (Math.max(sw, sh) * (0.72F + 0.18F * transition));
        int x = (int) (sw * 0.505F) - size / 2;
        int y = (int) (sh * 0.225F) - size / 2;
        int alpha = Math.min(135, Math.max(0, (int) (135.0F * transition)));
        int color = (alpha << 24) | 0x00FFFFFF;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            TRANSITION_GLOW,
            x, y,
            0.0F, 0.0F,
            size, size,
            1024, 1024,
            1024, 1024,
            color
        );
    }

    @Unique
    private void eclipse$renderTitleColorOverlays(DrawContext context) {
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        int brightness = EclipseConfig.titleBackgroundBrightness();
        if (brightness < 100) {
            int alpha = Math.min(180, (100 - brightness) * 2);
            context.fill(0, 0, sw, sh, alpha << 24);
        } else if (brightness > 100) {
            int alpha = Math.min(100, brightness - 100);
            context.fill(0, 0, sw, sh, (alpha << 24) | 0x00FFFFFF);
        }

        int contrast = EclipseConfig.titleBackgroundContrast();
        if (contrast > 100) {
            int alpha = Math.min(90, contrast - 100);
            context.fillGradient(0, 0, sw, sh / 2, alpha << 24, 0x00000000);
            context.fillGradient(0, sh / 2, sw, sh, 0x00000000, alpha << 24);
        } else if (contrast < 100) {
            int alpha = Math.min(90, 100 - contrast);
            context.fill(0, 0, sw, sh, (alpha << 24) | 0x007F7F7F);
        }

        if (EclipseConfig.titleVignette()) {
            int alpha = Math.max(0, Math.min(180, EclipseConfig.titleVignetteStrength()));
            int color = alpha << 24;
            int bandX = Math.max(16, sw / 7);
            int bandY = Math.max(16, sh / 7);
            context.fillGradient(0, 0, bandX, sh, color, 0x00000000);
            context.fillGradient(sw - bandX, 0, sw, sh, 0x00000000, color);
            context.fillGradient(0, 0, sw, bandY, color, 0x00000000);
            context.fillGradient(0, sh - bandY, sw, sh, 0x00000000, color);
        }
    }
}
