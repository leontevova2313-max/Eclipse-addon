package eclipse;

import net.minecraft.util.Identifier;

public final class EclipseConfig {
    public enum LogoAlignment {
        Center,
        Top,
        Custom
    }

    public enum LogoAnimation {
        None,
        Pulse,
        Fade
    }

    public enum EasingMode {
        Linear,
        Smooth,
        EaseOut
    }

    public enum CrosshairStyle {
        Classic,
        Dot,
        Cross,
        TShape,
        Circle
    }

    private static boolean titleBackground = true;
    private static final long TITLE_BACKGROUND_TRANSITION_MS = 900L;
    private static long titleBackgroundChangedAt = System.currentTimeMillis();
    private static boolean titleAnimation = true;
    private static boolean titleCrossfade = true;
    private static boolean titleLogo = true;
    private static boolean customTitleBackground = false;
    private static String customTitleBackgroundPath = "";
    private static Identifier customTitleBackgroundTexture;
    private static int customTitleBackgroundWidth = 1920;
    private static int customTitleBackgroundHeight = 1080;
    private static boolean screenBackgrounds = true;
    private static boolean performanceMode = false;
    private static boolean adaptivePerformance = true;
    private static boolean disableAnimations = false;
    private static double globalAnimationSpeed = 1.0;
    private static int uiBlurStrength = 0;
    private static int backgroundDimStrength = 35;
    private static int frameTimeMs = 1800;
    private static double titleAnimationSpeed = 1.0;
    private static boolean titleSmoothInterpolation = true;
    private static EasingMode titleEasingMode = EasingMode.Smooth;
    private static boolean titleParallax = true;
    private static boolean titleVignette = true;
    private static int titleVignetteStrength = 70;
    private static int titleBackgroundBrightness = 100;
    private static int titleBackgroundContrast = 100;
    private static int logoWidth = 340;
    private static int logoY = 18;
    private static LogoAlignment logoAlignment = LogoAlignment.Center;
    private static double logoScale = 1.0;
    private static LogoAnimation logoAnimation = LogoAnimation.None;
    private static int logoOpacity = 255;
    private static int logoSafeMargin = 14;
    private static boolean logoAutoScale = true;
    private static boolean dynamicLogoSpacing = true;
    private static boolean crosshair = false;
    private static int crosshairGap = 4;
    private static int crosshairLength = 6;
    private static int crosshairThickness = 1;
    private static int crosshairDotSize = 1;
    private static int crosshairColor = 0xFFFFFFFF;
    private static int crosshairOutlineColor = 0xAA000000;
    private static int crosshairGradientStart = 0xFFFFFFFF;
    private static int crosshairGradientEnd = 0xFF4FD8FF;
    private static boolean crosshairDot = true;
    private static boolean crosshairOutline = true;
    private static boolean crosshairDynamicGap = true;
    private static CrosshairStyle crosshairStyle = CrosshairStyle.Classic;
    private static boolean crosshairRainbow = false;
    private static int crosshairOpacity = 255;
    private static boolean crosshairShadow = false;
    private static int crosshairShadowStrength = 120;
    private static double crosshairMovementExpansion = 1.0;
    private static boolean crosshairRecoil = true;
    private static boolean hideCrosshairWhenHoldingItem = false;
    private static boolean crosshairCombatOnly = false;
    private static boolean cameraTweaksActive = false;
    private static boolean customFov = false;
    private static double fov = 110.0;
    private static boolean lowCamera = false;
    private static double cameraHeight = 0.62;
    private static double sneakCameraHeight = 0.42;
    private static double sneakFovScale = 0.85;

    private EclipseConfig() {
    }

    public static boolean titleBackground() {
        return titleBackground;
    }

    public static void titleBackground(boolean value) {
        if (titleBackground != value) titleBackgroundChangedAt = System.currentTimeMillis();
        titleBackground = value;
    }

    public static float titleBackgroundTransition() {
        if (disableAnimations()) return 0.0F;
        long elapsed = System.currentTimeMillis() - titleBackgroundChangedAt;
        if (elapsed >= TITLE_BACKGROUND_TRANSITION_MS) return 0.0F;
        float progress = 1.0F - (elapsed / (float) TITLE_BACKGROUND_TRANSITION_MS);
        return ease(progress);
    }

    public static boolean titleAnimation() {
        return titleAnimation && !disableAnimations();
    }

    public static void titleAnimation(boolean value) {
        titleAnimation = value;
    }

    public static boolean titleCrossfade() {
        return titleCrossfade && !performanceMode && !disableAnimations();
    }

    public static void titleCrossfade(boolean value) {
        titleCrossfade = value;
    }

    public static boolean titleLogo() {
        return titleLogo;
    }

    public static void titleLogo(boolean value) {
        titleLogo = value;
    }

    public static boolean customTitleBackground() {
        return customTitleBackground && customTitleBackgroundTexture != null;
    }

    public static void customTitleBackground(boolean value) {
        customTitleBackground = value;
    }

    public static String customTitleBackgroundPath() {
        return customTitleBackgroundPath;
    }

    public static void customTitleBackgroundPath(String value) {
        customTitleBackgroundPath = value == null ? "" : value.trim();
    }

    public static Identifier customTitleBackgroundTexture() {
        return customTitleBackgroundTexture;
    }

    public static void customTitleBackgroundTexture(Identifier texture, int width, int height) {
        customTitleBackgroundTexture = texture;
        customTitleBackgroundWidth = width;
        customTitleBackgroundHeight = height;
    }

    public static void clearCustomTitleBackgroundTexture() {
        customTitleBackgroundTexture = null;
        customTitleBackgroundWidth = 1920;
        customTitleBackgroundHeight = 1080;
    }

    public static int customTitleBackgroundWidth() {
        return customTitleBackgroundWidth;
    }

    public static int customTitleBackgroundHeight() {
        return customTitleBackgroundHeight;
    }

    public static boolean screenBackgrounds() {
        return screenBackgrounds;
    }

    public static void screenBackgrounds(boolean value) {
        screenBackgrounds = value;
    }

    public static boolean performanceMode() {
        return performanceMode || adaptivePerformance;
    }

    public static void performanceMode(boolean value) {
        performanceMode = value;
    }

    public static boolean adaptivePerformance() {
        return adaptivePerformance;
    }

    public static void adaptivePerformance(boolean value) {
        adaptivePerformance = value;
    }

    public static boolean disableAnimations() {
        return disableAnimations || performanceMode;
    }

    public static void disableAnimations(boolean value) {
        disableAnimations = value;
    }

    public static double globalAnimationSpeed() {
        return disableAnimations() ? 0.0 : globalAnimationSpeed;
    }

    public static void globalAnimationSpeed(double value) {
        globalAnimationSpeed = value;
    }

    public static int uiBlurStrength() {
        return uiBlurStrength;
    }

    public static void uiBlurStrength(int value) {
        uiBlurStrength = value;
    }

    public static int backgroundDimStrength() {
        return backgroundDimStrength;
    }

    public static void backgroundDimStrength(int value) {
        backgroundDimStrength = value;
    }

    public static int frameTimeMs() {
        double speed = Math.max(0.1, titleAnimationSpeed * Math.max(0.1, globalAnimationSpeed()));
        int adjusted = (int) Math.round(frameTimeMs / speed);
        return performanceMode ? Math.max(adjusted, 3000) : Math.max(120, adjusted);
    }

    public static void frameTimeMs(int value) {
        frameTimeMs = value;
    }

    public static double titleAnimationSpeed() {
        return titleAnimationSpeed;
    }

    public static void titleAnimationSpeed(double value) {
        titleAnimationSpeed = value;
    }

    public static boolean titleSmoothInterpolation() {
        return titleSmoothInterpolation && !disableAnimations();
    }

    public static void titleSmoothInterpolation(boolean value) {
        titleSmoothInterpolation = value;
    }

    public static EasingMode titleEasingMode() {
        return titleEasingMode;
    }

    public static void titleEasingMode(EasingMode value) {
        titleEasingMode = value == null ? EasingMode.Smooth : value;
    }

    public static boolean titleParallax() {
        return titleParallax && !performanceMode();
    }

    public static void titleParallax(boolean value) {
        titleParallax = value;
    }

    public static boolean titleVignette() {
        return titleVignette;
    }

    public static void titleVignette(boolean value) {
        titleVignette = value;
    }

    public static int titleVignetteStrength() {
        return titleVignetteStrength;
    }

    public static void titleVignetteStrength(int value) {
        titleVignetteStrength = value;
    }

    public static int titleBackgroundBrightness() {
        return titleBackgroundBrightness;
    }

    public static void titleBackgroundBrightness(int value) {
        titleBackgroundBrightness = value;
    }

    public static int titleBackgroundContrast() {
        return titleBackgroundContrast;
    }

    public static void titleBackgroundContrast(int value) {
        titleBackgroundContrast = value;
    }

    public static int logoWidth() {
        return logoWidth;
    }

    public static void logoWidth(int value) {
        logoWidth = value;
    }

    public static int logoY() {
        return logoY;
    }

    public static void logoY(int value) {
        logoY = value;
    }

    public static LogoAlignment logoAlignment() {
        return logoAlignment;
    }

    public static void logoAlignment(LogoAlignment value) {
        logoAlignment = value == null ? LogoAlignment.Center : value;
    }

    public static double logoScale() {
        return logoScale;
    }

    public static void logoScale(double value) {
        logoScale = value;
    }

    public static LogoAnimation logoAnimation() {
        return disableAnimations() ? LogoAnimation.None : logoAnimation;
    }

    public static void logoAnimation(LogoAnimation value) {
        logoAnimation = value == null ? LogoAnimation.None : value;
    }

    public static int logoOpacity() {
        return logoOpacity;
    }

    public static void logoOpacity(int value) {
        logoOpacity = value;
    }

    public static int logoSafeMargin() {
        return logoSafeMargin;
    }

    public static void logoSafeMargin(int value) {
        logoSafeMargin = value;
    }

    public static boolean logoAutoScale() {
        return logoAutoScale;
    }

    public static void logoAutoScale(boolean value) {
        logoAutoScale = value;
    }

    public static boolean dynamicLogoSpacing() {
        return dynamicLogoSpacing;
    }

    public static void dynamicLogoSpacing(boolean value) {
        dynamicLogoSpacing = value;
    }

    public static boolean crosshair() {
        return crosshair;
    }

    public static void crosshair(boolean value) {
        crosshair = value;
    }

    public static int crosshairGap() {
        return crosshairGap;
    }

    public static void crosshairGap(int value) {
        crosshairGap = value;
    }

    public static int crosshairLength() {
        return crosshairLength;
    }

    public static void crosshairLength(int value) {
        crosshairLength = value;
    }

    public static int crosshairThickness() {
        return crosshairThickness;
    }

    public static void crosshairThickness(int value) {
        crosshairThickness = value;
    }

    public static int crosshairDotSize() {
        return crosshairDotSize;
    }

    public static void crosshairDotSize(int value) {
        crosshairDotSize = value;
    }

    public static int crosshairColor() {
        return withAlpha(crosshairColor, crosshairOpacity);
    }

    public static void crosshairColor(int value) {
        crosshairColor = value;
    }

    public static int crosshairOutlineColor() {
        return crosshairOutlineColor;
    }

    public static void crosshairOutlineColor(int value) {
        crosshairOutlineColor = value;
    }

    public static int crosshairGradientStart() {
        return withAlpha(crosshairGradientStart, crosshairOpacity);
    }

    public static void crosshairGradientStart(int value) {
        crosshairGradientStart = value;
    }

    public static int crosshairGradientEnd() {
        return withAlpha(crosshairGradientEnd, crosshairOpacity);
    }

    public static void crosshairGradientEnd(int value) {
        crosshairGradientEnd = value;
    }

    public static boolean crosshairDot() {
        return crosshairDot || crosshairStyle == CrosshairStyle.Dot;
    }

    public static void crosshairDot(boolean value) {
        crosshairDot = value;
    }

    public static boolean crosshairOutline() {
        return crosshairOutline;
    }

    public static void crosshairOutline(boolean value) {
        crosshairOutline = value;
    }

    public static boolean crosshairDynamicGap() {
        return crosshairDynamicGap;
    }

    public static void crosshairDynamicGap(boolean value) {
        crosshairDynamicGap = value;
    }

    public static CrosshairStyle crosshairStyle() {
        return crosshairStyle;
    }

    public static void crosshairStyle(CrosshairStyle value) {
        crosshairStyle = value == null ? CrosshairStyle.Classic : value;
    }

    public static boolean crosshairRainbow() {
        return crosshairRainbow && !performanceMode();
    }

    public static void crosshairRainbow(boolean value) {
        crosshairRainbow = value;
    }

    public static int crosshairOpacity() {
        return crosshairOpacity;
    }

    public static void crosshairOpacity(int value) {
        crosshairOpacity = value;
    }

    public static boolean crosshairShadow() {
        return crosshairShadow;
    }

    public static void crosshairShadow(boolean value) {
        crosshairShadow = value;
    }

    public static int crosshairShadowStrength() {
        return crosshairShadowStrength;
    }

    public static void crosshairShadowStrength(int value) {
        crosshairShadowStrength = value;
    }

    public static double crosshairMovementExpansion() {
        return crosshairMovementExpansion;
    }

    public static void crosshairMovementExpansion(double value) {
        crosshairMovementExpansion = value;
    }

    public static boolean crosshairRecoil() {
        return crosshairRecoil;
    }

    public static void crosshairRecoil(boolean value) {
        crosshairRecoil = value;
    }

    public static boolean hideCrosshairWhenHoldingItem() {
        return hideCrosshairWhenHoldingItem;
    }

    public static void hideCrosshairWhenHoldingItem(boolean value) {
        hideCrosshairWhenHoldingItem = value;
    }

    public static boolean crosshairCombatOnly() {
        return crosshairCombatOnly;
    }

    public static void crosshairCombatOnly(boolean value) {
        crosshairCombatOnly = value;
    }

    public static boolean customFov() {
        return cameraTweaksActive && customFov;
    }

    public static void customFov(boolean value) {
        customFov = value;
    }

    public static double fov() {
        return fov;
    }

    public static void fov(double value) {
        fov = value;
    }

    public static boolean lowCamera() {
        return cameraTweaksActive && lowCamera;
    }

    public static void lowCamera(boolean value) {
        lowCamera = value;
    }

    public static double cameraHeight() {
        return cameraHeight;
    }

    public static void cameraHeight(double value) {
        cameraHeight = value;
    }

    public static double sneakCameraHeight() {
        return sneakCameraHeight;
    }

    public static void sneakCameraHeight(double value) {
        sneakCameraHeight = value;
    }

    public static double sneakFovScale() {
        return sneakFovScale;
    }

    public static void sneakFovScale(double value) {
        sneakFovScale = value;
    }

    public static void cameraTweaksActive(boolean value) {
        cameraTweaksActive = value;
    }

    public static float ease(float progress) {
        return switch (titleEasingMode) {
            case Linear -> progress;
            case Smooth -> progress * progress * (3.0F - 2.0F * progress);
            case EaseOut -> 1.0F - (1.0F - progress) * (1.0F - progress);
        };
    }

    public static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
