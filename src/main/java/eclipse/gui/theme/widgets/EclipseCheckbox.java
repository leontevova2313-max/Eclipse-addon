package eclipse.gui.theme.widgets;

import eclipse.gui.theme.EclipseModernTheme;
import eclipse.gui.theme.EclipseThemeRenderer;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorCheckbox;
import net.minecraft.util.math.MathHelper;

public class EclipseCheckbox extends WMeteorCheckbox {
    private double fillProgress;

    public EclipseCheckbox(boolean checked) {
        super(checked);
        fillProgress = checked ? 1 : 0;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EclipseModernTheme theme = (EclipseModernTheme) theme();
        fillProgress += (checked ? 1 : -1) * delta * 12;
        fillProgress = MathHelper.clamp(fillProgress, 0, 1);

        EclipseThemeRenderer.panel(renderer, theme, x, y, width, height, mouseOver, checked, false);

        if (fillProgress > 0) {
            double size = (width - theme.scale(6)) * fillProgress;
            double px = x + (width - size) / 2;
            double py = y + (height - size) / 2;
            renderer.quad(px, py, size, size, EclipseThemeRenderer.withAlpha(theme.accentLineColor(), (int) Math.round(255 * fillProgress)));
        }
    }
}
