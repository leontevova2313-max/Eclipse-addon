package eclipse.modules.visuals;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CrosshairInfo extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showBlockName = sgGeneral.add(new BoolSetting.Builder()
        .name("show-block-name")
        .description("Show targeted block name.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showPosition = sgGeneral.add(new BoolSetting.Builder()
        .name("show-position")
        .description("Show targeted block position.")
        .defaultValue(false)
        .build()
    );

    public CrosshairInfo() {
        super(Eclipse.VISUALS, "crosshair-info", "Shows targeted block information near the crosshair.");
    }

    public boolean showBlockName() { return showBlockName.get(); }
    public boolean showPosition() { return showPosition.get(); }
}
