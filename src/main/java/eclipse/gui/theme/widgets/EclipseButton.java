package eclipse.gui.theme.widgets;

import eclipse.gui.theme.EclipseModernTheme;
import eclipse.gui.theme.EclipseThemeRenderer;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable.WMeteorButton;

public class EclipseButton extends WMeteorButton {
    public EclipseButton(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EclipseModernTheme theme = (EclipseModernTheme) theme();
        double pad = pad();

        EclipseThemeRenderer.panel(renderer, theme, x, y, width, height, mouseOver, pressed, theme.shadows.get());

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, theme.textColor.get(), false);
        } else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, theme.textColor.get());
        }
    }
}
