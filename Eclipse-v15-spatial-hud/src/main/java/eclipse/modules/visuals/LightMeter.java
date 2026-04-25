package eclipse.modules.visuals;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LightMeter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showBlock = sgGeneral.add(new BoolSetting.Builder()
        .name("show-block-light")
        .description("Shows block light level.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showSky = sgGeneral.add(new BoolSetting.Builder()
        .name("show-sky-light")
        .description("Shows sky light level.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> safeThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("safe-threshold")
        .description("Safe light threshold hint.")
        .defaultValue(8)
        .range(0, 15)
        .sliderRange(0, 15)
        .build()
    );

    public LightMeter() {
        super(Eclipse.VISUALS, "light-meter", "Displays light values for the targeted block.");
    }

    public boolean showBlock() { return showBlock.get(); }
    public boolean showSky() { return showSky.get(); }
    public int safeThreshold() { return safeThreshold.get(); }
}
