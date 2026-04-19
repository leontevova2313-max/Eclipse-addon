package eclipse;

public final class EclipseConfig {
    private static boolean titleBackground = true;
    private static boolean titleAnimation = true;
    private static boolean titleCrossfade = true;
    private static boolean titleLogo = true;
    private static boolean screenBackgrounds = true;
    private static boolean allScreenBackgrounds = true;
    private static boolean performanceMode = false;
    private static int frameTimeMs = 1800;
    private static int logoWidth = 430;
    private static int logoY = 16;
    private static int logoStarSize = 3;
    private static int logoLineAlpha = 210;
    private static int logoColor = 0xFFFFFFFF;
    private static int logoGlowColor = 0x8829D6FF;
    private static boolean logoTwinkle = true;
    private static int backgroundDim = 36;
    private static boolean crosshair = false;
    private static int crosshairGap = 4;
    private static int crosshairLength = 6;
    private static int crosshairThickness = 1;
    private static int crosshairDotSize = 1;
    private static int crosshairColor = 0xFFFFFFFF;
    private static int crosshairOutlineColor = 0xAA000000;
    private static boolean crosshairDot = true;
    private static boolean crosshairOutline = true;
    private static boolean crosshairDynamicGap = true;
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
        titleBackground = value;
    }

    public static boolean titleAnimation() {
        return titleAnimation;
    }

    public static void titleAnimation(boolean value) {
        titleAnimation = value;
    }

    public static boolean titleCrossfade() {
        return titleCrossfade && !performanceMode;
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

    public static boolean screenBackgrounds() {
        return screenBackgrounds;
    }

    public static void screenBackgrounds(boolean value) {
        screenBackgrounds = value;
    }

    public static boolean allScreenBackgrounds() {
        return allScreenBackgrounds;
    }

    public static void allScreenBackgrounds(boolean value) {
        allScreenBackgrounds = value;
    }

    public static int backgroundDim() {
        return backgroundDim;
    }

    public static void backgroundDim(int value) {
        backgroundDim = Math.max(0, Math.min(220, value));
    }

    public static boolean performanceMode() {
        return performanceMode;
    }

    public static void performanceMode(boolean value) {
        performanceMode = value;
    }

    public static int frameTimeMs() {
        return performanceMode ? Math.max(frameTimeMs, 3000) : frameTimeMs;
    }

    public static void frameTimeMs(int value) {
        frameTimeMs = value;
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

    public static int logoStarSize() {
        return logoStarSize;
    }

    public static void logoStarSize(int value) {
        logoStarSize = value;
    }

    public static int logoLineAlpha() {
        return logoLineAlpha;
    }

    public static void logoLineAlpha(int value) {
        logoLineAlpha = value;
    }

    public static int logoColor() {
        return logoColor;
    }

    public static void logoColor(int value) {
        logoColor = value;
    }

    public static int logoGlowColor() {
        return logoGlowColor;
    }

    public static void logoGlowColor(int value) {
        logoGlowColor = value;
    }

    public static boolean logoTwinkle() {
        return logoTwinkle;
    }

    public static void logoTwinkle(boolean value) {
        logoTwinkle = value;
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
        return crosshairColor;
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

    public static boolean crosshairDot() {
        return crosshairDot;
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
}
