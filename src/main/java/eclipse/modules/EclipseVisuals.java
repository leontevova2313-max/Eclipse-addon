package eclipse.modules;

import eclipse.Eclipse;
import eclipse.EclipseConfig;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class EclipseVisuals extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTitle = settings.createGroup("Title Screen");
    private final SettingGroup sgCrosshair = settings.createGroup("Crosshair");

    private final Setting<Boolean> titleBackground = sgTitle.add(new BoolSetting.Builder()
        .name("title-background")
        .description("Replaces the vanilla panorama with Eclipse's lightweight animated title background.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleBackground)
        .build()
    );

    private final Setting<Boolean> titleAnimation = sgTitle.add(new BoolSetting.Builder()
        .name("title-animation")
        .description("Animates the title background using compact minimal frames.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleAnimation)
        .build()
    );

    private final Setting<Boolean> titleCrossfade = sgTitle.add(new BoolSetting.Builder()
        .name("title-crossfade")
        .description("Blends adjacent background frames for smoother motion.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleCrossfade)
        .build()
    );

    private final Setting<Integer> frameTime = sgTitle.add(new IntSetting.Builder()
        .name("frame-time")
        .description("Time between title background frames in milliseconds.")
        .defaultValue(1800)
        .range(500, 8000)
        .sliderRange(500, 5000)
        .onChanged(EclipseConfig::frameTimeMs)
        .build()
    );

    private final Setting<Boolean> titleLogo = sgTitle.add(new BoolSetting.Builder()
        .name("title-logo")
        .description("Replaces the vanilla Minecraft logo with the Eclipse constellation title mark.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleLogo)
        .build()
    );

    private final Setting<Integer> logoWidth = sgTitle.add(new IntSetting.Builder()
        .name("logo-width")
        .description("Width of the Eclipse title logo.")
        .defaultValue(360)
        .range(180, 640)
        .sliderRange(180, 520)
        .onChanged(EclipseConfig::logoWidth)
        .build()
    );

    private final Setting<Integer> logoY = sgTitle.add(new IntSetting.Builder()
        .name("logo-y")
        .description("Vertical position of the Eclipse title logo.")
        .defaultValue(24)
        .range(0, 120)
        .sliderRange(0, 80)
        .onChanged(EclipseConfig::logoY)
        .build()
    );

    private final Setting<Integer> logoStarSize = sgTitle.add(new IntSetting.Builder()
        .name("logo-star-size")
        .description("Base star size in the constellation title.")
        .defaultValue(3)
        .range(1, 10)
        .sliderRange(1, 7)
        .onChanged(EclipseConfig::logoStarSize)
        .build()
    );

    private final Setting<Integer> logoLineAlpha = sgTitle.add(new IntSetting.Builder()
        .name("logo-line-alpha")
        .description("Opacity of the constellation line strokes.")
        .defaultValue(210)
        .range(0, 255)
        .sliderRange(40, 255)
        .onChanged(EclipseConfig::logoLineAlpha)
        .build()
    );

    private final Setting<Boolean> logoTwinkle = sgTitle.add(new BoolSetting.Builder()
        .name("logo-twinkle")
        .description("Animates small star pulses in the title mark.")
        .defaultValue(true)
        .onChanged(EclipseConfig::logoTwinkle)
        .build()
    );

    private final Setting<SettingColor> logoColor = sgTitle.add(new ColorSetting.Builder()
        .name("logo-color")
        .description("Main constellation title color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .onChanged(color -> EclipseConfig.logoColor(color.getPacked()))
        .build()
    );

    private final Setting<SettingColor> logoGlowColor = sgTitle.add(new ColorSetting.Builder()
        .name("logo-glow-color")
        .description("Glow color behind title stars and lines.")
        .defaultValue(new SettingColor(41, 214, 255, 136))
        .onChanged(color -> EclipseConfig.logoGlowColor(color.getPacked()))
        .build()
    );

    private final Setting<Boolean> screenBackgrounds = sgGeneral.add(new BoolSetting.Builder()
        .name("screen-backgrounds")
        .description("Applies Eclipse backgrounds to supported Minecraft screens.")
        .defaultValue(true)
        .onChanged(EclipseConfig::screenBackgrounds)
        .build()
    );

    private final Setting<Boolean> allScreenBackgrounds = sgGeneral.add(new BoolSetting.Builder()
        .name("all-screen-backgrounds")
        .description("Uses the Eclipse background on every vanilla screen that calls renderBackground.")
        .defaultValue(true)
        .onChanged(EclipseConfig::allScreenBackgrounds)
        .build()
    );

    private final Setting<Integer> backgroundDim = sgGeneral.add(new IntSetting.Builder()
        .name("background-dim")
        .description("Dark overlay strength over menu backgrounds.")
        .defaultValue(36)
        .range(0, 220)
        .sliderRange(0, 160)
        .onChanged(EclipseConfig::backgroundDim)
        .build()
    );

    private final Setting<Boolean> performanceMode = sgGeneral.add(new BoolSetting.Builder()
        .name("performance-mode")
        .description("Reduces menu rendering cost by disabling crossfade and slowing title animation.")
        .defaultValue(false)
        .onChanged(EclipseConfig::performanceMode)
        .build()
    );

    private final Setting<Boolean> crosshair = sgCrosshair.add(new BoolSetting.Builder()
        .name("crosshair")
        .description("Replaces the vanilla crosshair with a configurable Eclipse crosshair.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshair)
        .build()
    );

    private final Setting<Boolean> crosshairOutline = sgCrosshair.add(new BoolSetting.Builder()
        .name("outline")
        .description("Adds a dark outline for better visibility.")
        .defaultValue(true)
        .onChanged(EclipseConfig::crosshairOutline)
        .build()
    );

    private final Setting<Boolean> crosshairDot = sgCrosshair.add(new BoolSetting.Builder()
        .name("dot")
        .description("Draws a center dot.")
        .defaultValue(true)
        .onChanged(EclipseConfig::crosshairDot)
        .build()
    );

    private final Setting<Boolean> crosshairDynamicGap = sgCrosshair.add(new BoolSetting.Builder()
        .name("dynamic-gap")
        .description("Slightly expands the crosshair while moving.")
        .defaultValue(true)
        .onChanged(EclipseConfig::crosshairDynamicGap)
        .build()
    );

    private final Setting<Integer> crosshairGap = sgCrosshair.add(new IntSetting.Builder()
        .name("gap")
        .description("Distance from the center to each arm.")
        .defaultValue(4)
        .range(0, 24)
        .sliderRange(0, 16)
        .onChanged(EclipseConfig::crosshairGap)
        .build()
    );

    private final Setting<Integer> crosshairLength = sgCrosshair.add(new IntSetting.Builder()
        .name("length")
        .description("Length of each crosshair arm.")
        .defaultValue(6)
        .range(1, 32)
        .sliderRange(2, 20)
        .onChanged(EclipseConfig::crosshairLength)
        .build()
    );

    private final Setting<Integer> crosshairThickness = sgCrosshair.add(new IntSetting.Builder()
        .name("thickness")
        .description("Thickness of the crosshair lines.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 5)
        .onChanged(EclipseConfig::crosshairThickness)
        .build()
    );

    private final Setting<Integer> crosshairDotSize = sgCrosshair.add(new IntSetting.Builder()
        .name("dot-size")
        .description("Size of the center dot.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 5)
        .onChanged(EclipseConfig::crosshairDotSize)
        .build()
    );

    private final Setting<SettingColor> crosshairColor = sgCrosshair.add(new ColorSetting.Builder()
        .name("color")
        .description("Primary crosshair color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .onChanged(color -> EclipseConfig.crosshairColor(color.getPacked()))
        .build()
    );

    private final Setting<SettingColor> crosshairOutlineColor = sgCrosshair.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(0, 0, 0, 170))
        .onChanged(color -> EclipseConfig.crosshairOutlineColor(color.getPacked()))
        .build()
    );

    public EclipseVisuals() {
        super(Eclipse.CATEGORY, "eclipse-visuals", "Controls Eclipse menus, backgrounds, branding, and crosshair styling.");
        runInMainMenu = true;
        syncConfig();
    }

    @Override
    public void onActivate() {
        syncConfig();
    }

    private void syncConfig() {
        EclipseConfig.titleBackground(titleBackground.get());
        EclipseConfig.titleAnimation(titleAnimation.get());
        EclipseConfig.titleCrossfade(titleCrossfade.get());
        EclipseConfig.frameTimeMs(frameTime.get());
        EclipseConfig.titleLogo(titleLogo.get());
        EclipseConfig.logoWidth(logoWidth.get());
        EclipseConfig.logoY(logoY.get());
        EclipseConfig.logoStarSize(logoStarSize.get());
        EclipseConfig.logoLineAlpha(logoLineAlpha.get());
        EclipseConfig.logoTwinkle(logoTwinkle.get());
        EclipseConfig.logoColor(logoColor.get().getPacked());
        EclipseConfig.logoGlowColor(logoGlowColor.get().getPacked());
        EclipseConfig.screenBackgrounds(screenBackgrounds.get());
        EclipseConfig.allScreenBackgrounds(allScreenBackgrounds.get());
        EclipseConfig.backgroundDim(backgroundDim.get());
        EclipseConfig.performanceMode(performanceMode.get());
        EclipseConfig.crosshair(crosshair.get());
        EclipseConfig.crosshairOutline(crosshairOutline.get());
        EclipseConfig.crosshairDot(crosshairDot.get());
        EclipseConfig.crosshairDynamicGap(crosshairDynamicGap.get());
        EclipseConfig.crosshairGap(crosshairGap.get());
        EclipseConfig.crosshairLength(crosshairLength.get());
        EclipseConfig.crosshairThickness(crosshairThickness.get());
        EclipseConfig.crosshairDotSize(crosshairDotSize.get());
        EclipseConfig.crosshairColor(crosshairColor.get().getPacked());
        EclipseConfig.crosshairOutlineColor(crosshairOutlineColor.get().getPacked());
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!EclipseConfig.crosshair() || mc.options.hudHidden) return;

        int x = event.drawContext.getScaledWindowWidth() / 2;
        int y = event.drawContext.getScaledWindowHeight() / 2;
        int gap = EclipseConfig.crosshairGap();

        if (EclipseConfig.crosshairDynamicGap() && mc.player != null) {
            double speed = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ);
            gap += Math.min(8, (int) Math.round(speed * 80.0));
        }

        int length = EclipseConfig.crosshairLength();
        int thickness = EclipseConfig.crosshairThickness();

        if (EclipseConfig.crosshairOutline()) {
            drawCrosshair(event, x, y, gap, length, thickness + 2, EclipseConfig.crosshairOutlineColor(), true);
        }

        drawCrosshair(event, x, y, gap, length, thickness, EclipseConfig.crosshairColor(), false);

        if (EclipseConfig.crosshairDot()) {
            int dot = EclipseConfig.crosshairDotSize();
            if (EclipseConfig.crosshairOutline()) {
                fillCentered(event, x, y, dot + 2, dot + 2, EclipseConfig.crosshairOutlineColor());
            }
            fillCentered(event, x, y, dot, dot, EclipseConfig.crosshairColor());
        }
    }

    private void drawCrosshair(Render2DEvent event, int x, int y, int gap, int length, int thickness, int color, boolean outline) {
        int extra = outline ? 1 : 0;
        int half = thickness / 2;

        fill(event, x - gap - length - extra, y - half, x - gap + extra, y - half + thickness, color);
        fill(event, x + gap + 1 - extra, y - half, x + gap + 1 + length + extra, y - half + thickness, color);
        fill(event, x - half, y - gap - length - extra, x - half + thickness, y - gap + extra, color);
        fill(event, x - half, y + gap + 1 - extra, x - half + thickness, y + gap + 1 + length + extra, color);
    }

    private void fillCentered(Render2DEvent event, int centerX, int centerY, int width, int height, int color) {
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        fill(event, x1, y1, x1 + width, y1 + height, color);
    }

    private void fill(Render2DEvent event, int x1, int y1, int x2, int y2, int color) {
        event.drawContext.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color);
    }
}
