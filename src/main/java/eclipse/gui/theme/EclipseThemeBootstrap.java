package eclipse.gui.theme;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.utils.PostInit;

public final class EclipseThemeBootstrap {
    private static boolean registered;

    private EclipseThemeBootstrap() {
    }

    @PostInit(dependencies = GuiThemes.class)
    public static void registerAndSelect() {
        ensureRegisteredAndSelected();
    }

    public static void ensureRegisteredAndSelected() {
        if (!registered) {
            GuiThemes.add(new EclipseModernTheme());
            registered = true;
        }

        GuiThemes.select(EclipseModernTheme.NAME);
    }
}
