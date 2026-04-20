package eclipse.gui.theme.widgets;

import eclipse.gui.theme.EclipseModernTheme;
import eclipse.gui.theme.EclipseThemeRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorTopBar;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class EclipseTopBar extends WMeteorTopBar {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        EclipseModernTheme theme = (EclipseModernTheme) theme();
        Color color = theme.surfaceColor();
        if (hovered) color = EclipseThemeRenderer.mix(color, theme.surfaceHoverColor(), 0.75);
        if (pressed) color = EclipseThemeRenderer.mix(color, theme.accentSoftColor(), 0.55);
        return color;
    }

    @Override
    protected Color getNameColor() {
        return ((EclipseModernTheme) theme()).textColor.get();
    }
}
