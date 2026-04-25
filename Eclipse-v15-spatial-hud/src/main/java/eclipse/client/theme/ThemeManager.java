package eclipse.client.theme;

public final class ThemeManager {
    private ClientThemeId current = ClientThemeId.DARK_MONO;
    private ThemeTokens active = new DarkMonoTheme();

    public ThemeTokens active() { return active; }
    public ClientThemeId current() { return current; }

    public void set(ClientThemeId id) {
        current = id;
        active = switch (id) {
            case DARK_MONO -> new DarkMonoTheme();
            case LIGHT_MONO -> new LightMonoTheme();
        };
    }

    public void cycle() {
        set(current == ClientThemeId.DARK_MONO ? ClientThemeId.LIGHT_MONO : ClientThemeId.DARK_MONO);
    }
}
