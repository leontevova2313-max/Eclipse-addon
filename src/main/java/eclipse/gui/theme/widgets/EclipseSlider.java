package eclipse.gui.theme.widgets;

import eclipse.gui.theme.EclipseModernTheme;
import eclipse.gui.theme.EclipseThemeRenderer;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.input.WMeteorSlider;

public class EclipseSlider extends WMeteorSlider {
    public EclipseSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EclipseModernTheme theme = (EclipseModernTheme) theme();
        double handleWidth = handleSize();
        double trackX = x + handleWidth / 2;
        double trackWidth = Math.max(0, width - handleWidth);
        double progress = Math.max(0, Math.min(1, (value - min) / (max - min)));

        EclipseThemeRenderer.sliderTrack(renderer, theme, trackX, y, trackWidth, height, progress, dragging || handleMouseOver);
    }
}
