package eclipse.modules.visuals;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ScreenshotGrid extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> columns = sgGeneral.add(new IntSetting.Builder()
        .name("columns")
        .description("Vertical divisions.")
        .defaultValue(3)
        .range(2, 6)
        .sliderRange(2, 6)
        .build()
    );

    private final Setting<Integer> rows = sgGeneral.add(new IntSetting.Builder()
        .name("rows")
        .description("Horizontal divisions.")
        .defaultValue(3)
        .range(2, 6)
        .sliderRange(2, 6)
        .build()
    );

    private final Setting<Boolean> centerCross = sgGeneral.add(new BoolSetting.Builder()
        .name("center-cross")
        .description("Draw a subtle center cross.")
        .defaultValue(true)
        .build()
    );

    public ScreenshotGrid() {
        super(Eclipse.VISUALS, "screenshot-grid", "Composition guides for screenshots and video.");
    }

    public int columns() { return columns.get(); }
    public int rows() { return rows.get(); }
    public boolean centerCross() { return centerCross.get(); }
}
