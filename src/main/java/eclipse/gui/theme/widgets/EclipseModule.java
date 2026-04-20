package eclipse.gui.theme.widgets;

import eclipse.gui.theme.EclipseModernTheme;
import eclipse.gui.theme.EclipseThemeRenderer;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class EclipseModule extends WPressable {
    private final Module module;
    private final String title;
    private double titleWidth;
    private double hoverProgress;
    private double activeProgress;

    public EclipseModule(Module module, String title) {
        this.module = module;
        this.title = title;
        this.tooltip = module.description;
        this.hoverProgress = module.isActive() ? 1 : 0;
        this.activeProgress = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
        return theme.scale(5);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        if (titleWidth == 0) titleWidth = theme.textWidth(title);
        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        EclipseModernTheme theme = (EclipseModernTheme) theme();
        double pad = pad();

        hoverProgress += delta * 7 * ((module.isActive() || mouseOver) ? 1 : -1);
        hoverProgress = MathHelper.clamp(hoverProgress, 0, 1);
        activeProgress += delta * 8 * (module.isActive() ? 1 : -1);
        activeProgress = MathHelper.clamp(activeProgress, 0, 1);

        EclipseThemeRenderer.moduleRow(renderer, theme, x, y, width, height, hoverProgress, activeProgress);

        double textX = x + pad + theme.scale(3);
        renderer.text(title, textX, y + pad, theme.textColor.get(), false);
    }
}
