package eclipse.gui.theme;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public final class EclipseThemeRenderer {
    private EclipseThemeRenderer() {
    }

    public static void panel(GuiRenderer renderer, EclipseModernTheme theme, double x, double y, double width, double height, boolean hovered, boolean active) {
        panel(renderer, theme, x, y, width, height, hovered, active, true);
    }

    public static void panel(GuiRenderer renderer, EclipseModernTheme theme, double x, double y, double width, double height, boolean hovered, boolean active, boolean shadow) {
        if (shadow && theme.shadows.get()) drawShadow(renderer, theme, x, y, width, height);

        Color fill = mix(theme.surfaceColor(), theme.surfaceHoverColor(), hovered ? 0.45 : 0.0);
        if (active) fill = mix(fill, theme.accentSoftColor(), 0.38);

        renderer.quad(x, y, width, height, fill);
        renderer.quad(x, y, width, theme.scale(1), theme.topHighlightColor());
        renderer.quad(x, y + height - theme.scale(1), width, theme.scale(1), theme.bottomBorderColor());
        renderer.quad(x, y, theme.scale(1), height, theme.borderColor());
        renderer.quad(x + width - theme.scale(1), y, theme.scale(1), height, theme.borderColor());
    }

    public static void sectionHeader(GuiRenderer renderer, EclipseModernTheme theme, double x, double y, double width, double height, boolean hovered, boolean active) {
        if (theme.shadows.get()) renderer.quad(x + theme.scale(2), y + theme.scale(2), width, height, theme.headerShadowColor());

        Color fill = mix(theme.headerColor(), theme.headerHoverColor(), hovered ? 0.5 : 0.0);
        if (active) fill = mix(fill, theme.accentSoftColor(), 0.24);

        renderer.quad(x, y, width, height, fill);
        renderer.quad(x, y, width, theme.scale(1), theme.topHighlightColor());
        renderer.quad(x, y + height - theme.scale(1), width, theme.scale(1), active ? theme.accentLineColor() : theme.bottomBorderColor());
        renderer.quad(x, y, theme.scale(1), height, theme.borderColor());
        renderer.quad(x + width - theme.scale(1), y, theme.scale(1), height, theme.borderColor());
    }

    public static void moduleRow(GuiRenderer renderer, EclipseModernTheme theme, double x, double y, double width, double height, double hoverProgress, double activeProgress) {
        double emphasis = Math.max(hoverProgress, activeProgress);
        Color row = mix(theme.rowColor(), theme.rowHoverColor(), hoverProgress);
        row = mix(row, theme.accentSoftColor(), activeProgress * 0.42);

        renderer.quad(x, y, width, height, row);
        renderer.quad(x, y + height - theme.scale(1), width, theme.scale(1), theme.rowSeparatorColor());

        if (emphasis > 0.0) {
            renderer.quad(x, y, theme.scale(2), height, withAlpha(theme.accentLineColor(), (int) Math.round(theme.accentLineColor().a * emphasis)));
        }
    }

    public static void sliderTrack(GuiRenderer renderer, EclipseModernTheme theme, double x, double y, double width, double height, double progress, boolean hovered) {
        double trackHeight = Math.max(2, theme.scale(3));
        double trackY = y + height / 2 - trackHeight / 2;
        renderer.quad(x, trackY, width, trackHeight, theme.trackColor());
        renderer.quad(x, trackY, width * progress, trackHeight, theme.accentLineColor());

        double handleWidth = Math.max(theme.scale(8), height - theme.scale(6));
        double handleHeight = Math.max(theme.scale(10), height - theme.scale(4));
        double handleX = x + (width - handleWidth) * progress;
        double handleY = y + height / 2 - handleHeight / 2;
        panel(renderer, theme, handleX, handleY, handleWidth, handleHeight, hovered, true, theme.shadows.get());
    }

    private static void drawShadow(GuiRenderer renderer, EclipseModernTheme theme, double x, double y, double width, double height) {
        renderer.quad(x + theme.scale(1), y + theme.scale(2), width, height, theme.shadowColor(0.55));
        renderer.quad(x + theme.scale(2), y + theme.scale(4), width, height, theme.shadowColor(0.35));
        renderer.quad(x + theme.scale(4), y + theme.scale(7), width, height, theme.shadowColor(0.18));
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.r, color.g, color.b, Math.max(0, Math.min(255, alpha)));
    }

    public static Color mix(Color first, Color second, double amount) {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        int r = (int) Math.round(first.r + (second.r - first.r) * clamped);
        int g = (int) Math.round(first.g + (second.g - first.g) * clamped);
        int b = (int) Math.round(first.b + (second.b - first.b) * clamped);
        int a = (int) Math.round(first.a + (second.a - first.a) * clamped);
        return new Color(r, g, b, a);
    }
}
