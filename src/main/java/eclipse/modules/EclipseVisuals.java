package eclipse.modules;

import eclipse.Eclipse;
import eclipse.EclipseConfig;
import eclipse.gui.EclipseDynamicTextures;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public class EclipseVisuals extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTitle = settings.createGroup("Title Screen");
    private final SettingGroup sgAnimations = settings.createGroup("Animations");
    private final SettingGroup sgLayout = settings.createGroup("Layout");
    private final SettingGroup sgTheme = settings.createGroup("Colors & Theme");
    private final SettingGroup sgCrosshair = settings.createGroup("Crosshair");
    private final SettingGroup sgAdvancedCrosshair = settings.createGroup("Advanced Crosshair");
    private final SettingGroup sgDebug = settings.createGroup("Debug / Developer");

    private final Setting<Boolean> screenBackgrounds = sgGeneral.add(new BoolSetting.Builder()
        .name("screen-backgrounds")
        .description("Applies Eclipse backgrounds to supported Minecraft screens.")
        .defaultValue(true)
        .onChanged(EclipseConfig::screenBackgrounds)
        .build()
    );

    private final Setting<Boolean> performanceMode = sgGeneral.add(new BoolSetting.Builder()
        .name("performance-mode")
        .description("Reduces expensive menu animation and crossfade effects.")
        .defaultValue(false)
        .onChanged(EclipseConfig::performanceMode)
        .build()
    );

    private final Setting<Boolean> adaptivePerformance = sgGeneral.add(new BoolSetting.Builder()
        .name("adaptive-performance")
        .description("Keeps visual effects conservative on heavier screens.")
        .defaultValue(true)
        .onChanged(EclipseConfig::adaptivePerformance)
        .build()
    );

    private final Setting<Integer> uiBlurStrength = sgGeneral.add(new IntSetting.Builder()
        .name("ui-blur")
        .description("Applies vanilla menu blur on Eclipse screen backgrounds.")
        .defaultValue(2)
        .range(0, 10)
        .sliderRange(0, 6)
        .onChanged(EclipseConfig::uiBlurStrength)
        .build()
    );

    private final Setting<Integer> backgroundDimStrength = sgGeneral.add(new IntSetting.Builder()
        .name("background-dim")
        .description("Darkens supported menu backgrounds for readability.")
        .defaultValue(35)
        .range(0, 180)
        .sliderRange(0, 120)
        .onChanged(EclipseConfig::backgroundDimStrength)
        .build()
    );

    private final Setting<Boolean> disableAnimations = sgAnimations.add(new BoolSetting.Builder()
        .name("disable-animations")
        .description("Master switch for Eclipse menu and logo animations.")
        .defaultValue(false)
        .onChanged(EclipseConfig::disableAnimations)
        .build()
    );

    private final Setting<Double> globalAnimationSpeed = sgAnimations.add(new DoubleSetting.Builder()
        .name("global-speed")
        .description("Global multiplier for Eclipse visual animations.")
        .defaultValue(1.0)
        .range(0.25, 3.0)
        .sliderRange(0.5, 2.0)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::globalAnimationSpeed)
        .build()
    );

    private final Setting<Double> titleAnimationSpeed = sgAnimations.add(new DoubleSetting.Builder()
        .name("title-speed")
        .description("Multiplier for title background animation speed.")
        .defaultValue(1.0)
        .range(0.25, 3.0)
        .sliderRange(0.5, 2.0)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::titleAnimationSpeed)
        .build()
    );

    private final Setting<Boolean> titleSmoothInterpolation = sgAnimations.add(new BoolSetting.Builder()
        .name("smooth-title")
        .description("Smooths frame interpolation on animated title backgrounds.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleSmoothInterpolation)
        .build()
    );

    private final Setting<EclipseConfig.EasingMode> titleEasing = sgAnimations.add(new EnumSetting.Builder<EclipseConfig.EasingMode>()
        .name("title-easing")
        .description("Interpolation curve used by title background transitions.")
        .defaultValue(EclipseConfig.EasingMode.Smooth)
        .onChanged(EclipseConfig::titleEasingMode)
        .build()
    );

    private final Setting<Boolean> titleParallax = sgAnimations.add(new BoolSetting.Builder()
        .name("title-parallax")
        .description("Adds subtle mouse parallax to the title background.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleParallax)
        .build()
    );

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

    private final Setting<Boolean> customTitleBackground = sgTitle.add(new BoolSetting.Builder()
        .name("custom-background")
        .description("Uses an image from disk as the title screen background.")
        .defaultValue(false)
        .onChanged(value -> {
            EclipseConfig.customTitleBackground(value);
            if (value) reloadCustomBackground();
        })
        .build()
    );

    private final Setting<String> customTitleBackgroundPath = sgTitle.add(new StringSetting.Builder()
        .name("custom-background-path")
        .description("Absolute path to a png or jpg image used by Custom Background.")
        .defaultValue("")
        .placeholder("C:\\path\\to\\background.png")
        .wide()
        .onChanged(value -> {
            EclipseConfig.customTitleBackgroundPath(value);
            if (customTitleBackground.get()) reloadCustomBackground();
        })
        .build()
    );

    private final Setting<Boolean> reloadCustomBackground = sgTitle.add(new BoolSetting.Builder()
        .name("reload-custom-background")
        .description("Toggle this after replacing the image file.")
        .defaultValue(false)
        .onChanged(value -> reloadCustomBackground())
        .build()
    );

    private final Setting<Integer> frameTime = sgTitle.add(new IntSetting.Builder()
        .name("frame-time")
        .description("Base time between title background frames in milliseconds.")
        .defaultValue(1800)
        .range(500, 8000)
        .sliderRange(500, 5000)
        .onChanged(EclipseConfig::frameTimeMs)
        .build()
    );

    private final Setting<Boolean> titleLogo = sgTitle.add(new BoolSetting.Builder()
        .name("title-logo")
        .description("Replaces the vanilla Minecraft logo with the Eclipse title mark.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleLogo)
        .build()
    );

    private final Setting<Integer> logoWidth = sgTitle.add(new IntSetting.Builder()
        .name("logo-width")
        .description("Base width of the Eclipse title logo.")
        .defaultValue(340)
        .range(180, 420)
        .sliderRange(240, 400)
        .onChanged(EclipseConfig::logoWidth)
        .build()
    );

    private final Setting<Integer> logoY = sgTitle.add(new IntSetting.Builder()
        .name("logo-y")
        .description("Custom vertical logo position when alignment is Custom.")
        .defaultValue(18)
        .range(0, 140)
        .sliderRange(0, 100)
        .onChanged(EclipseConfig::logoY)
        .build()
    );

    private final Setting<EclipseConfig.LogoAlignment> logoAlignment = sgLayout.add(new EnumSetting.Builder<EclipseConfig.LogoAlignment>()
        .name("logo-alignment")
        .description("Logo placement strategy on the title screen.")
        .defaultValue(EclipseConfig.LogoAlignment.Center)
        .onChanged(EclipseConfig::logoAlignment)
        .build()
    );

    private final Setting<Double> logoScale = sgLayout.add(new DoubleSetting.Builder()
        .name("logo-scale")
        .description("Scales the title logo while preserving safe layout.")
        .defaultValue(1.0)
        .range(0.6, 1.5)
        .sliderRange(0.75, 1.25)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::logoScale)
        .build()
    );

    private final Setting<Integer> logoSafeMargin = sgLayout.add(new IntSetting.Builder()
        .name("logo-safe-margin")
        .description("Minimum spacing between logo and title buttons.")
        .defaultValue(14)
        .range(4, 48)
        .sliderRange(8, 32)
        .onChanged(EclipseConfig::logoSafeMargin)
        .build()
    );

    private final Setting<Boolean> logoAutoScale = sgLayout.add(new BoolSetting.Builder()
        .name("logo-auto-scale")
        .description("Shrinks the logo automatically on small windows.")
        .defaultValue(true)
        .onChanged(EclipseConfig::logoAutoScale)
        .build()
    );

    private final Setting<Boolean> dynamicLogoSpacing = sgLayout.add(new BoolSetting.Builder()
        .name("dynamic-logo-spacing")
        .description("Derives title button spacing from the active logo layout.")
        .defaultValue(true)
        .onChanged(EclipseConfig::dynamicLogoSpacing)
        .build()
    );

    private final Setting<EclipseConfig.LogoAnimation> logoAnimation = sgAnimations.add(new EnumSetting.Builder<EclipseConfig.LogoAnimation>()
        .name("logo-animation")
        .description("Subtle animation applied to the title logo.")
        .defaultValue(EclipseConfig.LogoAnimation.None)
        .onChanged(EclipseConfig::logoAnimation)
        .build()
    );

    private final Setting<Integer> logoOpacity = sgTheme.add(new IntSetting.Builder()
        .name("logo-opacity")
        .description("Opacity of the custom title logo.")
        .defaultValue(255)
        .range(40, 255)
        .sliderRange(120, 255)
        .onChanged(EclipseConfig::logoOpacity)
        .build()
    );

    private final Setting<Boolean> titleVignette = sgTheme.add(new BoolSetting.Builder()
        .name("title-vignette")
        .description("Darkens title screen edges for focus.")
        .defaultValue(true)
        .onChanged(EclipseConfig::titleVignette)
        .build()
    );

    private final Setting<Integer> titleVignetteStrength = sgTheme.add(new IntSetting.Builder()
        .name("vignette-strength")
        .description("Strength of title screen edge darkening.")
        .defaultValue(70)
        .range(0, 180)
        .sliderRange(20, 130)
        .onChanged(EclipseConfig::titleVignetteStrength)
        .build()
    );

    private final Setting<Integer> titleBrightness = sgTheme.add(new IntSetting.Builder()
        .name("background-brightness")
        .description("Brightness correction for title backgrounds.")
        .defaultValue(100)
        .range(50, 150)
        .sliderRange(70, 130)
        .onChanged(EclipseConfig::titleBackgroundBrightness)
        .build()
    );

    private final Setting<Integer> titleContrast = sgTheme.add(new IntSetting.Builder()
        .name("background-contrast")
        .description("Contrast correction for title backgrounds.")
        .defaultValue(100)
        .range(50, 160)
        .sliderRange(80, 135)
        .onChanged(EclipseConfig::titleBackgroundContrast)
        .build()
    );

    private final Setting<Boolean> crosshair = sgCrosshair.add(new BoolSetting.Builder()
        .name("crosshair")
        .description("Replaces the vanilla crosshair with a configurable Eclipse crosshair.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshair)
        .build()
    );

    private final Setting<EclipseConfig.CrosshairStyle> crosshairStyle = sgCrosshair.add(new EnumSetting.Builder<EclipseConfig.CrosshairStyle>()
        .name("style")
        .description("Crosshair shape.")
        .defaultValue(EclipseConfig.CrosshairStyle.Classic)
        .onChanged(EclipseConfig::crosshairStyle)
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
        .description("Expands the crosshair while moving.")
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

    private final Setting<Boolean> crosshairOutline = sgAdvancedCrosshair.add(new BoolSetting.Builder()
        .name("outline")
        .description("Adds a dark outline for better visibility.")
        .defaultValue(true)
        .onChanged(EclipseConfig::crosshairOutline)
        .build()
    );

    private final Setting<SettingColor> crosshairOutlineColor = sgAdvancedCrosshair.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(0, 0, 0, 170))
        .onChanged(color -> EclipseConfig.crosshairOutlineColor(color.getPacked()))
        .build()
    );

    private final Setting<SettingColor> crosshairGradientStart = sgAdvancedCrosshair.add(new ColorSetting.Builder()
        .name("gradient-start")
        .description("Gradient start color for crosshair arms.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .onChanged(color -> EclipseConfig.crosshairGradientStart(color.getPacked()))
        .build()
    );

    private final Setting<SettingColor> crosshairGradientEnd = sgAdvancedCrosshair.add(new ColorSetting.Builder()
        .name("gradient-end")
        .description("Gradient end color for crosshair arms.")
        .defaultValue(new SettingColor(79, 216, 255, 255))
        .onChanged(color -> EclipseConfig.crosshairGradientEnd(color.getPacked()))
        .build()
    );

    private final Setting<Boolean> crosshairRainbow = sgAdvancedCrosshair.add(new BoolSetting.Builder()
        .name("rainbow")
        .description("Animates crosshair color with a lightweight hue cycle.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshairRainbow)
        .build()
    );

    private final Setting<Integer> crosshairOpacity = sgAdvancedCrosshair.add(new IntSetting.Builder()
        .name("opacity")
        .description("Overall custom crosshair opacity.")
        .defaultValue(255)
        .range(30, 255)
        .sliderRange(90, 255)
        .onChanged(EclipseConfig::crosshairOpacity)
        .build()
    );

    private final Setting<Boolean> crosshairShadow = sgAdvancedCrosshair.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Draws a soft offset shadow under the crosshair.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshairShadow)
        .build()
    );

    private final Setting<Integer> crosshairShadowStrength = sgAdvancedCrosshair.add(new IntSetting.Builder()
        .name("shadow-strength")
        .description("Opacity of the crosshair shadow.")
        .defaultValue(120)
        .range(20, 220)
        .sliderRange(60, 180)
        .onChanged(EclipseConfig::crosshairShadowStrength)
        .build()
    );

    private final Setting<Double> movementExpansion = sgAdvancedCrosshair.add(new DoubleSetting.Builder()
        .name("movement-expansion")
        .description("Multiplier for movement-based crosshair expansion.")
        .defaultValue(1.0)
        .range(0.0, 3.0)
        .sliderRange(0.0, 2.0)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::crosshairMovementExpansion)
        .build()
    );

    private final Setting<Boolean> recoilSimulation = sgAdvancedCrosshair.add(new BoolSetting.Builder()
        .name("recoil-simulation")
        .description("Briefly expands after attack/use input.")
        .defaultValue(true)
        .onChanged(EclipseConfig::crosshairRecoil)
        .build()
    );

    private final Setting<Boolean> hideWhenHoldingItem = sgAdvancedCrosshair.add(new BoolSetting.Builder()
        .name("hide-when-holding-item")
        .description("Hides the custom crosshair while holding any item.")
        .defaultValue(false)
        .onChanged(EclipseConfig::hideCrosshairWhenHoldingItem)
        .build()
    );

    private final Setting<Boolean> combatOnly = sgAdvancedCrosshair.add(new BoolSetting.Builder()
        .name("combat-only")
        .description("Shows the custom crosshair only while holding a weapon.")
        .defaultValue(false)
        .onChanged(EclipseConfig::crosshairCombatOnly)
        .build()
    );

    private final Setting<Boolean> debugVisualState = sgDebug.add(new BoolSetting.Builder()
        .name("debug-visual-state")
        .description("Prints one compact sync message when toggled.")
        .defaultValue(false)
        .onChanged(value -> {
            if (value) info("Eclipse visuals synced: title=" + titleBackground.get() + ", crosshair=" + crosshair.get());
        })
        .build()
    );

    private long recoilUntil;

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
        EclipseConfig.crosshair(crosshair.get());
        EclipseConfig.crosshairStyle(crosshairStyle.get());
        EclipseConfig.crosshairOutline(crosshairOutline.get());
        EclipseConfig.crosshairDot(crosshairDot.get());
        EclipseConfig.crosshairDynamicGap(crosshairDynamicGap.get());
        EclipseConfig.crosshairGap(crosshairGap.get());
        EclipseConfig.crosshairLength(crosshairLength.get());
        EclipseConfig.crosshairThickness(crosshairThickness.get());
        EclipseConfig.crosshairDotSize(crosshairDotSize.get());
        EclipseConfig.crosshairColor(crosshairColor.get().getPacked());
        EclipseConfig.crosshairOutlineColor(crosshairOutlineColor.get().getPacked());
        EclipseConfig.crosshairGradientStart(crosshairGradientStart.get().getPacked());
        EclipseConfig.crosshairGradientEnd(crosshairGradientEnd.get().getPacked());
        EclipseConfig.crosshairRainbow(crosshairRainbow.get());
        EclipseConfig.crosshairOpacity(crosshairOpacity.get());
        EclipseConfig.crosshairShadow(crosshairShadow.get());
        EclipseConfig.crosshairShadowStrength(crosshairShadowStrength.get());
        EclipseConfig.crosshairMovementExpansion(movementExpansion.get());
        EclipseConfig.crosshairRecoil(recoilSimulation.get());
        EclipseConfig.hideCrosshairWhenHoldingItem(hideWhenHoldingItem.get());
        EclipseConfig.crosshairCombatOnly(combatOnly.get());
    }

    private void reloadCustomBackground() {
        if (!EclipseDynamicTextures.reloadTitleBackground(customTitleBackgroundPath.get()) && customTitleBackground.get()) {
            warning("Custom title background was not loaded. Check the file path.");
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!shouldRenderCrosshair()) return;

        if (EclipseConfig.crosshairRecoil() && (mc.options.attackKey.isPressed() || mc.options.useKey.isPressed())) {
            recoilUntil = System.currentTimeMillis() + 110L;
        }

        int x = event.drawContext.getScaledWindowWidth() / 2;
        int y = event.drawContext.getScaledWindowHeight() / 2;
        int gap = EclipseConfig.crosshairGap() + dynamicExpansion();
        int length = EclipseConfig.crosshairLength();
        int thickness = EclipseConfig.crosshairThickness();
        int primary = primaryColor();
        int secondary = EclipseConfig.crosshairRainbow() ? primary : EclipseConfig.crosshairGradientEnd();

        if (EclipseConfig.crosshairShadow()) {
            drawStyledCrosshair(event, x + 1, y + 1, gap, length, thickness, shadowColor(), shadowColor(), false);
        }

        if (EclipseConfig.crosshairOutline()) {
            drawStyledCrosshair(event, x, y, gap, length, thickness + 2, EclipseConfig.crosshairOutlineColor(), EclipseConfig.crosshairOutlineColor(), true);
        }

        drawStyledCrosshair(event, x, y, gap, length, thickness, primary, secondary, false);

        if (EclipseConfig.crosshairDot()) {
            int dot = EclipseConfig.crosshairDotSize();
            if (EclipseConfig.crosshairOutline()) fillCentered(event, x, y, dot + 2, dot + 2, EclipseConfig.crosshairOutlineColor());
            fillCentered(event, x, y, dot, dot, primary);
        }
    }

    private boolean shouldRenderCrosshair() {
        if (!EclipseConfig.crosshair() || mc.options.hudHidden || mc.player == null) return false;
        if (EclipseConfig.hideCrosshairWhenHoldingItem() && !mc.player.getMainHandStack().isEmpty()) return false;
        return !EclipseConfig.crosshairCombatOnly() || isHoldingCombatItem();
    }

    private boolean isHoldingCombatItem() {
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS)
            || stack.isIn(ItemTags.AXES)
            || stack.isOf(Items.BOW)
            || stack.isOf(Items.CROSSBOW)
            || stack.isOf(Items.TRIDENT);
    }

    private int dynamicExpansion() {
        int expansion = 0;
        if (EclipseConfig.crosshairDynamicGap() && mc.player != null) {
            double speed = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ);
            expansion += Math.min(12, (int) Math.round(speed * 80.0 * EclipseConfig.crosshairMovementExpansion()));
        }

        long now = System.currentTimeMillis();
        if (EclipseConfig.crosshairRecoil() && recoilUntil > now) {
            expansion += (int) Math.ceil((recoilUntil - now) / 28.0);
        }

        return expansion;
    }

    private int primaryColor() {
        if (!EclipseConfig.crosshairRainbow()) return EclipseConfig.crosshairGradientStart();
        float hue = (System.currentTimeMillis() % 3000L) / 3000.0F;
        return EclipseConfig.withAlpha(java.awt.Color.HSBtoRGB(hue, 0.72F, 1.0F), EclipseConfig.crosshairOpacity());
    }

    private int shadowColor() {
        return EclipseConfig.withAlpha(0x000000, EclipseConfig.crosshairShadowStrength());
    }

    private void drawStyledCrosshair(Render2DEvent event, int x, int y, int gap, int length, int thickness, int startColor, int endColor, boolean outline) {
        switch (EclipseConfig.crosshairStyle()) {
            case Dot -> {
            }
            case Cross -> drawCrosshair(event, x, y, Math.max(0, gap / 2), length, thickness, startColor, endColor, outline, false);
            case TShape -> drawCrosshair(event, x, y, gap, length, thickness, startColor, endColor, outline, true);
            case Circle -> drawCircle(event, x, y, Math.max(3, gap + length / 2), thickness, startColor);
            case Classic -> drawCrosshair(event, x, y, gap, length, thickness, startColor, endColor, outline, false);
        }
    }

    private void drawCrosshair(Render2DEvent event, int x, int y, int gap, int length, int thickness, int startColor, int endColor, boolean outline, boolean tShape) {
        int extra = outline ? 1 : 0;
        int half = thickness / 2;

        fillGradient(event, x - gap - length - extra, y - half, x - gap + extra, y - half + thickness, endColor, startColor);
        fillGradient(event, x + gap + 1 - extra, y - half, x + gap + 1 + length + extra, y - half + thickness, startColor, endColor);
        fillGradient(event, x - half, y - gap - length - extra, x - half + thickness, y - gap + extra, endColor, startColor);
        if (!tShape) {
            fillGradient(event, x - half, y + gap + 1 - extra, x - half + thickness, y + gap + 1 + length + extra, startColor, endColor);
        }
    }

    private void drawCircle(Render2DEvent event, int x, int y, int radius, int thickness, int color) {
        for (int i = 0; i < thickness; i++) {
            int r = radius + i;
            fill(event, x - r, y - 1, x - r + 1, y + 1, color);
            fill(event, x + r, y - 1, x + r + 1, y + 1, color);
            fill(event, x - 1, y - r, x + 1, y - r + 1, color);
            fill(event, x - 1, y + r, x + 1, y + r + 1, color);
            fill(event, x - r * 7 / 10, y - r * 7 / 10, x - r * 7 / 10 + 1, y - r * 7 / 10 + 1, color);
            fill(event, x + r * 7 / 10, y - r * 7 / 10, x + r * 7 / 10 + 1, y - r * 7 / 10 + 1, color);
            fill(event, x - r * 7 / 10, y + r * 7 / 10, x - r * 7 / 10 + 1, y + r * 7 / 10 + 1, color);
            fill(event, x + r * 7 / 10, y + r * 7 / 10, x + r * 7 / 10 + 1, y + r * 7 / 10 + 1, color);
        }
    }

    private void fillCentered(Render2DEvent event, int centerX, int centerY, int width, int height, int color) {
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        fill(event, x1, y1, x1 + width, y1 + height, color);
    }

    private void fillGradient(Render2DEvent event, int x1, int y1, int x2, int y2, int colorA, int colorB) {
        event.drawContext.fillGradient(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), colorA, colorB);
    }

    private void fill(Render2DEvent event, int x1, int y1, int x2, int y2, int color) {
        event.drawContext.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color);
    }
}
