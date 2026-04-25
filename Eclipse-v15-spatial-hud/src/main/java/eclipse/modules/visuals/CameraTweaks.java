package eclipse.modules.visuals;

import eclipse.Eclipse;
import eclipse.EclipseConfig;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CameraTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> customFov = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-fov")
        .description("Overrides Minecraft's dynamic world FOV with a fixed value.")
        .defaultValue(false)
        .onChanged(EclipseConfig::customFov)
        .build()
    );

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Fixed world camera FOV.")
        .defaultValue(110.0)
        .range(30.0, 170.0)
        .sliderRange(60.0, 140.0)
        .decimalPlaces(1)
        .onChanged(EclipseConfig::fov)
        .build()
    );

    private final Setting<Boolean> lowCamera = sgGeneral.add(new BoolSetting.Builder()
        .name("low-camera")
        .description("Lowers the first-person viewpoint for a shorter character feel.")
        .defaultValue(false)
        .onChanged(EclipseConfig::lowCamera)
        .build()
    );

    private final Setting<Double> cameraHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("camera-height")
        .description("Standing eye height used by Low Camera.")
        .defaultValue(0.62)
        .range(0.15, 1.62)
        .sliderRange(0.35, 1.62)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::cameraHeight)
        .build()
    );

    private final Setting<Double> sneakCameraHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("sneak-camera-height")
        .description("Eye height used while sneaking with Low Camera.")
        .defaultValue(0.42)
        .range(0.1, 1.62)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::sneakCameraHeight)
        .build()
    );

    private final Setting<Double> sneakFovScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("sneak-fov-scale")
        .description("FOV multiplier applied while sneaking with Low Camera.")
        .defaultValue(0.85)
        .range(0.5, 1.2)
        .sliderRange(0.65, 1.0)
        .decimalPlaces(2)
        .onChanged(EclipseConfig::sneakFovScale)
        .build()
    );

    private final Setting<Boolean> noViewBob = sgGeneral.add(new BoolSetting.Builder()
        .name("no-view-bob")
        .description("Disables first-person view bobbing.")
        .defaultValue(true)
        .onChanged(EclipseConfig::noViewBob)
        .build()
    );

    private final Setting<Boolean> noHurtTilt = sgGeneral.add(new BoolSetting.Builder()
        .name("no-hurt-tilt")
        .description("Disables the hurt camera tilt and screen jerk when taking damage.")
        .defaultValue(true)
        .onChanged(EclipseConfig::noHurtTilt)
        .build()
    );

    public CameraTweaks() {
        super(Eclipse.VISUALS, "eclipse-camera", "Fine-tunes first-person FOV, viewpoint height, and camera-based block targeting.");
        runInMainMenu = true;
        EclipseConfig.cameraTweaksActive(false);
    }

    @Override
    public void onActivate() {
        EclipseConfig.cameraTweaksActive(true);
        syncConfig();
    }

    @Override
    public void onDeactivate() {
        EclipseConfig.cameraTweaksActive(false);
        EclipseConfig.customFov(false);
        EclipseConfig.lowCamera(false);
        EclipseConfig.noViewBob(false);
        EclipseConfig.noHurtTilt(false);
    }

    private void syncConfig() {
        EclipseConfig.customFov(customFov.get());
        EclipseConfig.fov(fov.get());
        EclipseConfig.lowCamera(lowCamera.get());
        EclipseConfig.cameraHeight(cameraHeight.get());
        EclipseConfig.sneakCameraHeight(sneakCameraHeight.get());
        EclipseConfig.sneakFovScale(sneakFovScale.get());
        EclipseConfig.noViewBob(noViewBob.get());
        EclipseConfig.noHurtTilt(noHurtTilt.get());
    }
}

