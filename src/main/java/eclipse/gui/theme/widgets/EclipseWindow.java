package eclipse.gui.theme.widgets;

import eclipse.gui.theme.EclipseModernTheme;
import eclipse.gui.theme.EclipseThemeRenderer;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

public class EclipseWindow extends WWindow implements MeteorWidget {
    public EclipseWindow(WWidget icon, String title) {
        super(icon, title);
        padding = 0;
        spacing = 0;
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new EclipseHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            EclipseModernTheme theme = (EclipseModernTheme) theme();
            double bodyY = y + header.height;
            double bodyHeight = Math.max(0, height - header.height);
            EclipseThemeRenderer.panel(renderer, theme, x, bodyY, width, bodyHeight, mouseOver, false, theme.shadows.get());
        }
    }

    private class EclipseHeader extends WHeader {
        private EclipseHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            EclipseModernTheme theme = (EclipseModernTheme) theme();
            EclipseThemeRenderer.sectionHeader(renderer, theme, x, y, width, height, mouseOver, expanded || animProgress > 0);
        }
    }
}
