package eclipse.modules.visuals;

import eclipse.Eclipse;
import eclipse.EclipseConfig;
import eclipse.gui.EclipseDynamicTextures;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

public class EclipseVisuals extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTitle = settings.createGroup("Title");
    private final SettingGroup sgAnimations = settings.createGroup("Motion");
    private final SettingGroup sgLayout = settings.createGroup("Logo");
    private final SettingGroup sgTheme = settings.createGroup("Style");
    private final SettingGroup sgDebug = settings.createGroup("Advanced");

    private final Setting<Boolean> screenBackgrounds = sgGeneral.add(new BoolSetting.Builder()
        .name("screen-backgrounds")
        .description("Custom screen backgrounds.")
        .defaultValue(true)
        .onChanged(EclipseConfig::screenBackgrounds)
        .build()
    );

    private final Setting<Boolean> performanceMode = sgGeneral.add(new BoolSetting.Builder()
        .name("performance-mode")
        .description("Reduces heavy effects.")
        .defaultValue(false)
        .onChanged(EclipseConfig::performanceMode)
        .build()
    );

    private final Setting<Boolean> adaptivePerformance = sgGeneral.add(new BoolSetting.Builder()
        .name("adaptive-performance")
        .description("Auto-limits heavier screens.")
        .visible(() -> !performanceMode.get())
        .defaultValue(true)
        .onChanged(EclipseConfig::adaptivePerformance)
        .build()
    );

    private final Setting<Integer> uiBlurStrength = sgGeneral.add(new IntSetting.Builder()
        .name("ui-blur")
        .description("Menu background blur.")
        .defaultValue(2)
        .range(0, 10)
        .sliderRange(0, 6)
        .visible(screenBackgrounds::get)
        .onChanged(EclipseConfig::uiBlurStrength)
        .build()
    );

    private final Setting<Integer> backgroundDimStrength = sgGeneral.add(new IntSetting.Builder()
        .name("background-dim")
        .description("Menu background dim.")
        .defaultValue(35)
        .range(0, 180)
        .sliderRange(0, 120)
        .visible(screenBackgrounds::get)
        .onChanged(EclipseConfig::backgroundDimStrength)
        .build()
    );

    private final Setting<Boolean> disableAnimations = sgAnimations.add(new BoolSetting.Builder()
        .name("disable-animations")
        .description("Stops visual motion.")
        .defaultValue(false)
        .onChanged(EclipseConfig::disableAnimations)
        .build()
    );

    private final Setting<Double> globalAnimationSpeed = sgAnimations.add(new DoubleSetting.Builder()
        .name("global-speed")
        .description("Global motion speed.")
        .defaultValue(1.0)
        .range(0.25, 3.0)
        .sliderRange(0.5, 2.0)
        .decimalPlaces(2)
        .visible(this::animationsAvailable)
        .onChanged(EclipseConfig::globalAnimationSpeed)
        .build()
    );

    private final Setting<Double> titleAnimationSpeed = sgAnimations.add(new DoubleSetting.Builder()
        .name("title-speed")
        .description("Title motion speed.")
        .defaultValue(1.0)
        .range(0.25, 3.0)
        .sliderRange(0.5, 2.0)
        .decimalPlaces(2)
        .visible(this::titleAnimationControlsVisible)
        .onChanged(EclipseConfig::titleAnimationSpeed)
        .build()
    );

    private final Setting<Boolean> titleSmoothInterpolation = sgAnimations.add(new BoolSetting.Builder()
        .name("smooth-title")
        .description("Smooth frame blending.")
        .defaultValue(true)
        .visible(this::titleAnimationControlsVisible)
        .onChanged(EclipseConfig::titleSmoothInterpolation)
        .build()
    );

    private final Setting<EclipseConfig.EasingMode> titleEasing = sgAnimations.add(new EnumSetting.Builder<EclipseConfig.EasingMode>()
        .name("title-easing")
        .description("Title transition curve.")
        .defaultValue(EclipseConfig.EasingMode.Smooth)
        .visible(this::titleAnimationControlsVisible)
        .onChanged(EclipseConfig::titleEasingMode)
        .build()
    );

    private final Setting<Boolean> titleParallax = sgAnimations.add(new BoolSetting.Builder()
        .name("title-parallax")
        .description("Mouse parallax.")
        .defaultValue(true)
        .visible(this::titleParallaxVisible)
        .onChanged(EclipseConfig::titleParallax)
        .build()
    );

    private final Setting<Boolean> titleBackground = sgTitle.add(new BoolSetting.Builder()
        .name("title-background")
        .description("Custom title background.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleBackground)
        .build()
    );

    private final Setting<Boolean> titleAnimation = sgTitle.add(new BoolSetting.Builder()
        .name("title-animation")
        .description("Animated title frames.")
        .defaultValue(true)
        .visible(this::titleAnimationToggleVisible)
        .onChanged(EclipseConfig::titleAnimation)
        .build()
    );

    private final Setting<Boolean> titleCrossfade = sgTitle.add(new BoolSetting.Builder()
        .name("title-crossfade")
        .description("Blend title frames.")
        .defaultValue(true)
        .visible(this::titleAnimationControlsVisible)
        .onChanged(EclipseConfig::titleCrossfade)
        .build()
    );

    private final Setting<Boolean> customTitleBackground = sgTitle.add(new BoolSetting.Builder()
        .name("custom-background")
        .description("Use image from disk.")
        .defaultValue(false)
        .visible(titleBackground::get)
        .onChanged(value -> {
            EclipseConfig.customTitleBackground(value);
            if (value) reloadCustomBackground();
        })
        .build()
    );

    private final Setting<String> customTitleBackgroundPath = sgTitle.add(new StringSetting.Builder()
        .name("custom-background-path")
        .description("PNG/JPG path.")
        .defaultValue("")
        .placeholder("C:\\path\\to\\background.png")
        .wide()
        .visible(() -> titleBackground.get() && customTitleBackground.get())
        .onChanged(value -> {
            EclipseConfig.customTitleBackgroundPath(value);
            if (customTitleBackground.get()) reloadCustomBackground();
        })
        .build()
    );

    private final Setting<Boolean> reloadCustomBackground = sgTitle.add(new BoolSetting.Builder()
        .name("reload-custom-background")
        .description("Reload image.")
        .defaultValue(false)
        .visible(() -> titleBackground.get() && customTitleBackground.get())
        .onChanged(value -> reloadCustomBackground())
        .build()
    );

    private final Setting<Integer> frameTime = sgTitle.add(new IntSetting.Builder()
        .name("frame-time")
        .description("Frame time in ms.")
        .defaultValue(1800)
        .range(500, 8000)
        .sliderRange(500, 5000)
        .visible(this::titleAnimationControlsVisible)
        .onChanged(EclipseConfig::frameTimeMs)
        .build()
    );

    private final Setting<Boolean> titleLogo = sgLayout.add(new BoolSetting.Builder()
        .name("title-logo")
        .description("Custom title logo.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleLogo)
        .build()
    );

    private final Setting<Integer> logoWidth = sgLayout.add(new IntSetting.Builder()
        .name("logo-width")
        .description("Logo width.")
        .defaultValue(340)
        .range(180, 420)
        .sliderRange(240, 400)
        .visible(titleLogo::get)
        .onChanged(EclipseConfig::logoWidth)
        .build()
    );

    private final Setting<Integer> logoY = sgLayout.add(new IntSetting.Builder()
        .name("logo-y")
        .description("Custom Y position.")
        .defaultValue(18)
        .range(0, 140)
        .sliderRange(0, 100)
        .visible(this::customLogoYVisible)
        .onChanged(EclipseConfig::logoY)
        .build()
    );

    private final Setting<EclipseConfig.LogoAlignment> logoAlignment = sgLayout.add(new EnumSetting.Builder<EclipseConfig.LogoAlignment>()
        .name("logo-alignment")
        .description("Logo placement.")
        .defaultValue(EclipseConfig.LogoAlignment.Center)
        .visible(titleLogo::get)
        .onChanged(EclipseConfig::logoAlignment)
        .build()
    );

    private final Setting<Double> logoScale = sgLayout.add(new DoubleSetting.Builder()
        .name("logo-scale")
        .description("Logo scale.")
        .defaultValue(1.0)
        .range(0.6, 1.5)
        .sliderRange(0.75, 1.25)
        .decimalPlaces(2)
        .visible(titleLogo::get)
        .onChanged(EclipseConfig::logoScale)
        .build()
    );

    private final Setting<Integer> logoSafeMargin = sgLayout.add(new IntSetting.Builder()
        .name("logo-safe-margin")
        .description("Button spacing.")
        .defaultValue(14)
        .range(4, 48)
        .sliderRange(8, 32)
        .visible(this::logoSafeMarginVisible)
        .onChanged(EclipseConfig::logoSafeMargin)
        .build()
    );

    private final Setting<Boolean> logoAutoScale = sgLayout.add(new BoolSetting.Builder()
        .name("logo-auto-scale")
        .description("Shrink on small windows.")
        .defaultValue(true)
        .visible(titleLogo::get)
        .onChanged(EclipseConfig::logoAutoScale)
        .build()
    );

    private final Setting<Boolean> dynamicLogoSpacing = sgLayout.add(new BoolSetting.Builder()
        .name("dynamic-logo-spacing")
        .description("Auto button spacing.")
        .defaultValue(true)
        .visible(titleLogo::get)
        .onChanged(EclipseConfig::dynamicLogoSpacing)
        .build()
    );

    private final Setting<EclipseConfig.LogoAnimation> logoAnimation = sgAnimations.add(new EnumSetting.Builder<EclipseConfig.LogoAnimation>()
        .name("logo-animation")
        .description("Logo animation.")
        .defaultValue(EclipseConfig.LogoAnimation.None)
        .visible(() -> titleLogo.get() && animationsAvailable())
        .onChanged(EclipseConfig::logoAnimation)
        .build()
    );

    private final Setting<Integer> logoOpacity = sgTheme.add(new IntSetting.Builder()
        .name("logo-opacity")
        .description("Logo alpha.")
        .defaultValue(255)
        .range(40, 255)
        .sliderRange(120, 255)
        .visible(titleLogo::get)
        .onChanged(EclipseConfig::logoOpacity)
        .build()
    );

    private final Setting<Boolean> titleVignette = sgTheme.add(new BoolSetting.Builder()
        .name("title-vignette")
        .description("Dark title edges.")
        .defaultValue(true)
        .visible(titleBackground::get)
        .onChanged(EclipseConfig::titleVignette)
        .build()
    );

    private final Setting<Integer> titleVignetteStrength = sgTheme.add(new IntSetting.Builder()
        .name("vignette-strength")
        .description("Edge darkness.")
        .defaultValue(70)
        .range(0, 180)
        .sliderRange(20, 130)
        .visible(() -> titleBackground.get() && titleVignette.get())
        .onChanged(EclipseConfig::titleVignetteStrength)
        .build()
    );

    private final Setting<Integer> titleBrightness = sgTheme.add(new IntSetting.Builder()
        .name("background-brightness")
        .description("Background brightness.")
        .defaultValue(100)
        .range(50, 150)
        .sliderRange(70, 130)
        .visible(titleBackground::get)
        .onChanged(EclipseConfig::titleBackgroundBrightness)
        .build()
    );

    private final Setting<Integer> titleContrast = sgTheme.add(new IntSetting.Builder()
        .name("background-contrast")
        .description("Background contrast.")
        .defaultValue(100)
        .range(50, 160)
        .sliderRange(80, 135)
        .visible(titleBackground::get)
        .onChanged(EclipseConfig::titleBackgroundContrast)
        .build()
    );

    private final Setting<Boolean> debugVisualState = sgDebug.add(new BoolSetting.Builder()
        .name("debug-visual-state")
        .description("Prints one compact sync message when toggled.")
        .defaultValue(false)
        .onChanged(value -> {
            if (value) info("Eclipse visuals synced: title=" + titleBackground.get() + ", logo=" + titleLogo.get());
        })
        .build()
    );


    public EclipseVisuals() {
        super(Eclipse.VISUALS, "eclipse-visuals", "Controls Eclipse menus, backgrounds, and branding.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        syncConfig();
    }

    @Override
    public void onDeactivate() {
        EclipseConfig.visualsActive(false);
    }

    private boolean animationsAvailable() {
        return !disableAnimations.get() && !performanceMode.get();
    }

    private boolean titleAnimationToggleVisible() {
        return titleBackground.get() && !customTitleBackground.get() && animationsAvailable();
    }

    private boolean titleAnimationControlsVisible() {
        return titleBackground.get()
            && titleAnimation.get()
            && animationsAvailable()
            && !customTitleBackground.get();
    }

    private boolean titleParallaxVisible() {
        return titleBackground.get() && animationsAvailable() && !customTitleBackground.get();
    }

    private boolean customLogoYVisible() {
        return titleLogo.get() && logoAlignment.get() == EclipseConfig.LogoAlignment.Custom;
    }

    private boolean logoSafeMarginVisible() {
        return titleLogo.get() && dynamicLogoSpacing.get();
    }

    private void syncConfig() {
        EclipseConfig.visualsActive(true);
        EclipseConfig.screenBackgrounds(screenBackgrounds.get());
        EclipseConfig.performanceMode(performanceMode.get());
        EclipseConfig.adaptivePerformance(adaptivePerformance.get());
        EclipseConfig.uiBlurStrength(uiBlurStrength.get());
        EclipseConfig.backgroundDimStrength(backgroundDimStrength.get());
        EclipseConfig.disableAnimations(disableAnimations.get());
        EclipseConfig.globalAnimationSpeed(globalAnimationSpeed.get());
        EclipseConfig.titleAnimationSpeed(titleAnimationSpeed.get());
        EclipseConfig.titleSmoothInterpolation(titleSmoothInterpolation.get());
        EclipseConfig.titleEasingMode(titleEasing.get());
        EclipseConfig.titleParallax(titleParallax.get());
        EclipseConfig.titleBackground(titleBackground.get());
        EclipseConfig.titleAnimation(titleAnimation.get());
        EclipseConfig.titleCrossfade(titleCrossfade.get());
        EclipseConfig.customTitleBackground(customTitleBackground.get());
        EclipseConfig.customTitleBackgroundPath(customTitleBackgroundPath.get());
        if (customTitleBackground.get()) reloadCustomBackground();
        EclipseConfig.frameTimeMs(frameTime.get());
        EclipseConfig.titleLogo(titleLogo.get());
        EclipseConfig.logoWidth(logoWidth.get());
        EclipseConfig.logoY(logoY.get());
        EclipseConfig.logoAlignment(logoAlignment.get());
        EclipseConfig.logoScale(logoScale.get());
        EclipseConfig.logoSafeMargin(logoSafeMargin.get());
        EclipseConfig.logoAutoScale(logoAutoScale.get());
        EclipseConfig.dynamicLogoSpacing(dynamicLogoSpacing.get());
        EclipseConfig.logoAnimation(logoAnimation.get());
        EclipseConfig.logoOpacity(logoOpacity.get());
        EclipseConfig.titleVignette(titleVignette.get());
        EclipseConfig.titleVignetteStrength(titleVignetteStrength.get());
        EclipseConfig.titleBackgroundBrightness(titleBrightness.get());
        EclipseConfig.titleBackgroundContrast(titleContrast.get());
    }

    private void reloadCustomBackground() {
        if (!EclipseDynamicTextures.reloadTitleBackground(customTitleBackgroundPath.get()) && customTitleBackground.get()) {
            warning("Custom title background was not loaded. Check the file path.");
        }
    }

}
