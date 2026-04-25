package eclipse.gui;

import eclipse.EclipseConfig;

public final class TitleLogoLayout {
    public static final int LOGO_TEXTURE_WIDTH = 1213;
    public static final int LOGO_TEXTURE_HEIGHT = 587;

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int LOGO_TOP_PADDING = 24;
    private static final int LOGO_BUTTON_SPACING = 14;
    private static final int FOOTER_SAFE_HEIGHT = 24;
    private static final int SIDE_RESERVED_WIDTH = 260;
    private static final int MIN_LOGO_WIDTH = 96;
    private static final int MAX_LOGO_WIDTH = 360;

    private TitleLogoLayout() {
    }

    public static Bounds calculate(int screenWidth, int screenHeight, int configuredLogoWidth, int buttonRows) {
        int rows = Math.max(1, buttonRows);
        int spacing = EclipseConfig.dynamicLogoSpacing()
            ? Math.max(8, EclipseConfig.logoSafeMargin())
            : LOGO_BUTTON_SPACING;
        int buttonGroupHeight = rows * BUTTON_HEIGHT + (rows - 1) * BUTTON_GAP;
        int maxLogoHeight = Math.max(34, screenHeight - LOGO_TOP_PADDING - spacing - buttonGroupHeight - FOOTER_SAFE_HEIGHT);
        int widthByHeight = maxLogoHeight * LOGO_TEXTURE_WIDTH / LOGO_TEXTURE_HEIGHT;
        int widthByScreen = Math.max(MIN_LOGO_WIDTH, screenWidth - SIDE_RESERVED_WIDTH);
        int configuredWidth = (int) Math.round(configuredLogoWidth * EclipseConfig.logoScale());
        int autoMax = EclipseConfig.logoAutoScale() ? Math.min(widthByScreen, widthByHeight) : MAX_LOGO_WIDTH;
        int logoWidth = clamp(Math.min(Math.min(configuredWidth, MAX_LOGO_WIDTH), autoMax), MIN_LOGO_WIDTH, MAX_LOGO_WIDTH);
        int logoHeight = logoWidth * LOGO_TEXTURE_HEIGHT / LOGO_TEXTURE_WIDTH;

        int totalHeight = logoHeight + spacing + buttonGroupHeight;
        int top = switch (EclipseConfig.logoAlignment()) {
            case Top -> Math.max(12, EclipseConfig.logoSafeMargin());
            case Custom -> Math.max(0, EclipseConfig.logoY());
            case Center -> Math.max(12, Math.min(LOGO_TOP_PADDING, (screenHeight - FOOTER_SAFE_HEIGHT - totalHeight) / 2));
        };
        int logoX = (screenWidth - logoWidth) / 2;
        int buttonStartY = top + logoHeight + spacing;

        return new Bounds(logoX, top, logoWidth, logoHeight, buttonStartY, BUTTON_GAP, BUTTON_HEIGHT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Bounds(int logoX, int logoY, int logoWidth, int logoHeight, int buttonStartY, int buttonGap, int buttonHeight) {
        public int rowY(int row) {
            return buttonStartY + row * (buttonHeight + buttonGap);
        }
    }
}
