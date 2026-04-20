package com.eclipse.mixin;

import eclipse.EclipseConfig;
import eclipse.gui.TitleLogoLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
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

    @Unique
    private static final EntityModelLayer eclipse$slimPlayerLayer = new EntityModelLayer(Identifier.ofVanilla("player_slim"), "main");

    @Unique
    private PlayerEntityModel eclipse$wideSkinModel;

    @Unique
    private PlayerEntityModel eclipse$slimSkinModel;

    @Unique
    private int eclipse$skinModelX;

    @Unique
    private int eclipse$skinModelY;

    @Unique
    private int eclipse$skinModelW;

    @Unique
    private int eclipse$skinModelH;

    @Unique
    private float eclipse$headPitch;

    @Unique
    private float eclipse$headYaw;

    @Unique
    private float eclipse$bodyPitch;

    @Unique
    private float eclipse$bodyYaw;

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
    private void eclipse$addProfileModel(CallbackInfo ci) {
        TitleScreen self = (TitleScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        if (EclipseConfig.titleLogo()) {
            eclipse$layoutTitleButtons(self);
        }

        eclipse$skinModelW = 56;
        eclipse$skinModelH = 90;
        eclipse$skinModelX = Math.max(12, Math.min(width - eclipse$skinModelW - 56, width - width / 5 - eclipse$skinModelW / 2));
        eclipse$skinModelY = Math.max(50, height / 2 + 22);

        eclipse$wideSkinModel = new PlayerEntityModel(client.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
        eclipse$slimSkinModel = new PlayerEntityModel(client.getLoadedEntityModels().getModelPart(eclipse$slimPlayerLayer), true);
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

    @Inject(method = "render", at = @At("HEAD"))
    private void eclipse$updateProfileHeadTracking(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (eclipse$wideSkinModel == null || eclipse$slimSkinModel == null) return;

        float centerX = eclipse$skinModelX + eclipse$skinModelW / 2.0F;
        float centerY = eclipse$skinModelY + eclipse$skinModelH * 0.28F;
        float dx = MathHelper.clamp((mouseX - centerX) / Math.max(1.0F, eclipse$skinModelW * 1.15F), -1.0F, 1.0F);
        float dy = MathHelper.clamp((mouseY - centerY) / Math.max(1.0F, eclipse$skinModelH * 0.85F), -1.0F, 1.0F);

        eclipse$headYaw = dx * 0.72F;
        eclipse$headPitch = dy * 0.42F;
        eclipse$bodyYaw = eclipse$headYaw * 0.18F;
        eclipse$bodyPitch = eclipse$headPitch * 0.08F;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void eclipse$renderLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        eclipse$renderProfileModel(context);
        eclipse$renderProfileName(context);

        if (!EclipseConfig.titleLogo()) return;

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
    private SkinTextures eclipse$currentSessionSkin() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getSkinProvider().supplySkinTextures(client.getGameProfile(), false).get();
    }

    @Unique
    private void eclipse$renderProfileName(DrawContext context) {
        if (eclipse$wideSkinModel == null || eclipse$slimSkinModel == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        String username = client.getSession().getUsername();
        int x = eclipse$skinModelX + eclipse$skinModelW / 2;
        int y = Math.max(8, eclipse$skinModelY - 21);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(username), x, y, 0xFFFFFFFF);
    }

    @Unique
    private void eclipse$renderProfileModel(DrawContext context) {
        if (eclipse$wideSkinModel == null || eclipse$slimSkinModel == null) return;

        SkinTextures textures = eclipse$currentSessionSkin();
        PlayerEntityModel model = textures.model() == PlayerSkinType.SLIM ? eclipse$slimSkinModel : eclipse$wideSkinModel;
        eclipse$poseProfileModel(model);

        float scale = 0.97F * eclipse$skinModelH / 2.125F;
        context.addPlayerSkin(
            model,
            textures.body().texturePath(),
            scale,
            0.0F,
            0.0F,
            -1.0625F,
            eclipse$skinModelX,
            eclipse$skinModelY,
            eclipse$skinModelX + eclipse$skinModelW,
            eclipse$skinModelY + eclipse$skinModelH
        );
    }

    @Unique
    private void eclipse$poseProfileModel(PlayerEntityModel model) {
        model.head.resetTransform();
        model.hat.resetTransform();
        model.body.resetTransform();
        model.rightArm.resetTransform();
        model.leftArm.resetTransform();
        model.rightLeg.resetTransform();
        model.leftLeg.resetTransform();
        model.jacket.resetTransform();
        model.rightSleeve.resetTransform();
        model.leftSleeve.resetTransform();
        model.rightPants.resetTransform();
        model.leftPants.resetTransform();

        model.head.setAngles(eclipse$headPitch, eclipse$headYaw, 0.0F);
        model.hat.setAngles(eclipse$headPitch, eclipse$headYaw, 0.0F);
        model.body.setAngles(eclipse$bodyPitch, eclipse$bodyYaw, 0.0F);
        model.rightArm.setAngles(eclipse$bodyPitch * 0.45F, eclipse$bodyYaw * 0.70F, 0.0F);
        model.leftArm.setAngles(eclipse$bodyPitch * 0.45F, eclipse$bodyYaw * 0.70F, 0.0F);
        model.rightLeg.setAngles(0.0F, eclipse$bodyYaw * 0.35F, 0.0F);
        model.leftLeg.setAngles(0.0F, eclipse$bodyYaw * 0.35F, 0.0F);

        eclipse$copyModelPartTransform(model.hat, model.head);
        eclipse$copyModelPartTransform(model.jacket, model.body);
        eclipse$copyModelPartTransform(model.rightSleeve, model.rightArm);
        eclipse$copyModelPartTransform(model.leftSleeve, model.leftArm);
        eclipse$copyModelPartTransform(model.rightPants, model.rightLeg);
        eclipse$copyModelPartTransform(model.leftPants, model.leftLeg);
    }

    @Unique
    private void eclipse$copyModelPartTransform(ModelPart target, ModelPart source) {
        target.originX = source.originX;
        target.originY = source.originY;
        target.originZ = source.originZ;
        target.pitch = source.pitch;
        target.yaw = source.yaw;
        target.roll = source.roll;
        target.xScale = source.xScale;
        target.yScale = source.yScale;
        target.zScale = source.zScale;
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
