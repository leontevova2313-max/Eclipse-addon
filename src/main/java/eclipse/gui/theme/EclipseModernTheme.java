package eclipse.gui.theme;

import eclipse.EclipseConfig;
import eclipse.gui.theme.widgets.EclipseButton;
import eclipse.gui.theme.widgets.EclipseCheckbox;
import eclipse.gui.theme.widgets.EclipseModule;
import eclipse.gui.theme.widgets.EclipseSlider;
import eclipse.gui.theme.widgets.EclipseTopBar;
import eclipse.gui.theme.widgets.EclipseWindow;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class EclipseModernTheme extends MeteorGuiTheme {
    public static final String NAME = "Eclipse Modern";

    private final SettingGroup sgEclipse = settings.createGroup("Eclipse Modern");

    public final Setting<Double> density = sgEclipse.add(new DoubleSetting.Builder()
        .name("density")
        .description("Reduces padding and keeps the layout compact.")
        .defaultValue(0.9)
        .min(0.82)
        .sliderRange(0.82, 1.05)
        .build()
    );

    public final Setting<Boolean> flatMode = sgEclipse.add(new BoolSetting.Builder()
        .name("flat-mode")
        .description("Uses flatter cards and lighter accents.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> shadows = sgEclipse.add(new BoolSetting.Builder()
        .name("shadows")
        .description("Draws soft card shadows behind windows and controls.")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> surface = sgEclipse.add(new ColorSetting.Builder()
        .name("surface-color")
        .description("Base color used for panels.")
        .defaultValue(new SettingColor(16, 18, 24, 184))
        .build()
    );

    public final Setting<SettingColor> surfaceHover = sgEclipse.add(new ColorSetting.Builder()
        .name("surface-hover-color")
        .description("Panel color when hovered.")
        .defaultValue(new SettingColor(22, 25, 32, 204))
        .build()
    );

    public final Setting<SettingColor> header = sgEclipse.add(new ColorSetting.Builder()
        .name("header-color")
        .description("Header color for windows and large sections.")
        .defaultValue(new SettingColor(18, 22, 29, 214))
        .build()
    );

    public final Setting<SettingColor> headerHover = sgEclipse.add(new ColorSetting.Builder()
        .name("header-hover-color")
        .description("Hovered header color.")
        .defaultValue(new SettingColor(24, 29, 38, 224))
        .build()
    );

    public final Setting<SettingColor> border = sgEclipse.add(new ColorSetting.Builder()
        .name("border-color")
        .description("Thin border color used around panels.")
        .defaultValue(new SettingColor(255, 255, 255, 24))
        .build()
    );

    public final Setting<SettingColor> row = sgEclipse.add(new ColorSetting.Builder()
        .name("row-color")
        .description("Base row color used for module tiles.")
        .defaultValue(new SettingColor(255, 255, 255, 9))
        .build()
    );

    public final Setting<SettingColor> rowHover = sgEclipse.add(new ColorSetting.Builder()
        .name("row-hover-color")
        .description("Hovered row color used for module tiles.")
        .defaultValue(new SettingColor(255, 255, 255, 16))
        .build()
    );

    public EclipseModernTheme() {
        super();
        accentColor.set(new SettingColor(108, 132, 255, 255));
        checkboxColor.set(new SettingColor(108, 132, 255, 255));
        backgroundColor.get().set(new SettingColor(14, 16, 20, 190));
        moduleBackground.set(new SettingColor(28, 32, 40, 156));
        sliderLeft.set(new SettingColor(108, 132, 255, 255));
        sliderRight.set(new SettingColor(255, 255, 255, 16));
        separatorCenter.set(new SettingColor(255, 255, 255, 42));
        separatorEdges.set(new SettingColor(255, 255, 255, 9));
        scrollbarColor.get().set(new SettingColor(255, 255, 255, 16));
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new EclipseWindow(icon, title));
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new EclipseButton(text, texture));
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new EclipseCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new EclipseSlider(value, min, max));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new EclipseModule(module, title));
    }

    @Override
    public WTopBar topBar() {
        return w(new EclipseTopBar());
    }

    @Override
    public WidgetScreen moduleScreen(Module module) {
        return super.moduleScreen(module);
    }

    @Override
    public double scale(double value) {
        return super.scale(value * density.get());
    }

    public Color surfaceColor() {
        return surface.get();
    }

    public Color surfaceHoverColor() {
        return surfaceHover.get();
    }

    public Color headerColor() {
        return header.get();
    }

    public Color headerHoverColor() {
        return headerHover.get();
    }

    public Color borderColor() {
        return border.get();
    }

    public Color bottomBorderColor() {
        return EclipseThemeRenderer.withAlpha(borderColor(), Math.max(12, borderColor().a / 2));
    }

    public Color topHighlightColor() {
        return new Color(255, 255, 255, flatMode.get() ? 22 : 34);
    }

    public Color rowColor() {
        return row.get();
    }

    public Color rowHoverColor() {
        return rowHover.get();
    }

    public Color rowSeparatorColor() {
        return new Color(255, 255, 255, 8);
    }

    public Color accentSoftColor() {
        Color accent = accentColor.get();
        return new Color(accent.r, accent.g, accent.b, 64);
    }

    public Color accentLineColor() {
        Color accent = accentColor.get();
        return new Color(accent.r, accent.g, accent.b, 255);
    }

    public Color trackColor() {
        return new Color(255, 255, 255, 18);
    }

    public Color headerShadowColor() {
        return new Color(0, 0, 0, shadows.get() && !EclipseConfig.performanceMode() ? 34 : 0);
    }

    public Color shadowColor(double strength) {
        int alpha = shadows.get() && !EclipseConfig.performanceMode() ? (int) Math.round(30 * strength) : 0;
        return new Color(0, 0, 0, alpha);
    }
}
