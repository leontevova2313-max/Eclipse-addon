package eclipse.modules.utility;

import eclipse.Eclipse;
import eclipse.client.profiles.ProfileStore;
import eclipse.client.runtime.ClientRuntime;
import eclipse.gui.EclipseToastOverlay;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.io.IOException;

public class Profiles extends Module {
    public enum Action {
        Save,
        Load,
        Autosafe
    }

    private static final int OK = 0x47F2A3;
    private static final int WARN = 0xFFCF5A;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Action> action = sgGeneral.add(new EnumSetting.Builder<Action>()
        .name("action")
        .description("Save stores the current setup. Load restores a stored setup. Autosafe refreshes the crash-safe backup.")
        .defaultValue(Action.Save)
        .build()
    );

    private final Setting<String> profileName = sgGeneral.add(new StringSetting.Builder()
        .name("profile-name")
        .description("Profile folder name in config/eclipse-profiles.")
        .defaultValue("default")
        .build()
    );

    private final Setting<Boolean> saveRuntimeStateFirst = sgGeneral.add(new BoolSetting.Builder()
        .name("save-runtime-state-first")
        .description("Saves Eclipse runtime state before copying profile files.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Shows Eclipse toasts for profile actions.")
        .defaultValue(true)
        .build()
    );

    public Profiles() {
        super(Eclipse.UTILITY, "profiles", "Saves and restores Eclipse/Meteor settings with .bak protection against crashes.");
    }

    @Override
    public void onActivate() {
        try {
            if (saveRuntimeStateFirst.get()) ClientRuntime.save();

            switch (action.get()) {
                case Save -> {
                    ProfileStore.save(name());
                    toast("Profile saved", name(), OK);
                }
                case Load -> {
                    ProfileStore.load(name());
                    toast("Profile loaded", name() + " — restart/rejoin recommended", OK);
                }
                case Autosafe -> {
                    ProfileStore.save("_autosafe");
                    toast("Autosafe refreshed", "_autosafe", OK);
                }
            }
        } catch (IOException exception) {
            toast("Profile action failed", exception.getMessage(), WARN);
        } finally {
            if (isActive()) toggle();
        }
    }

    private String name() {
        return ProfileStore.safeProfileName(profileName.get());
    }

    private void toast(String title, String message, int color) {
        if (notify.get()) EclipseToastOverlay.show(title, message == null ? "" : message, color);
    }
}
