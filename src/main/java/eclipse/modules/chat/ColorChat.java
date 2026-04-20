package eclipse.modules.chat;

import eclipse.Eclipse;
import eclipse.modules.chat.colorchat.ColorMode;
import eclipse.modules.chat.colorchat.CompatibilityStrategy;
import eclipse.modules.chat.colorchat.DualModeType;
import eclipse.modules.chat.colorchat.GradientMode;
import eclipse.modules.chat.colorchat.PresetName;
import eclipse.modules.chat.colorchat.RainbowMode;
import eclipse.modules.chat.colorchat.RandomPaletteMode;
import eclipse.modules.chat.colorchat.RandomSeedMode;
import eclipse.modules.chat.colorchat.SendMode;
import eclipse.modules.chat.colorchat.ChatSendTransformer;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorListSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

import java.util.List;

public class ColorChat extends Module {
    private final ChatSendTransformer sendTransformer = new ChatSendTransformer();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSolid = settings.createGroup("Solid");
    private final SettingGroup sgDual = settings.createGroup("Dual");
    private final SettingGroup sgGradient = settings.createGroup("Gradient");
    private final SettingGroup sgRainbow = settings.createGroup("Rainbow");
    private final SettingGroup sgRandom = settings.createGroup("Random");
    private final SettingGroup sgPreset = settings.createGroup("Presets");
    private final SettingGroup sgApply = settings.createGroup("Apply To");

    private final Setting<ColorMode> mode = sgGeneral.add(new EnumSetting.Builder<ColorMode>()
        .name("mode")
        .description("Controls how outgoing chat text is formatted locally and, if enabled, before send.")
        .defaultValue(ColorMode.OFF)
        .build()
    );

    private final Setting<Boolean> previewEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("preview-enabled")
        .description("Renders a local formatted preview above the chat input.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> previewInChatScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("preview-in-chat-screen")
        .description("Shows the preview only in the normal chat screen.")
        .defaultValue(true)
        .visible(previewEnabled::get)
        .build()
    );

    private final Setting<Boolean> stripFormattingBeforeSend = sgGeneral.add(new BoolSetting.Builder()
        .name("strip-formatting-before-send")
        .description("Removes common formatting syntaxes before applying the selected send mode.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SendMode> sendMode = sgGeneral.add(new EnumSetting.Builder<SendMode>()
        .name("send-mode")
        .description("PREVIEW_ONLY is safest. Raw modes depend on server/plugin support.")
        .defaultValue(SendMode.PREVIEW_ONLY)
        .build()
    );

    private final Setting<CompatibilityStrategy> compatibilityStrategy = sgGeneral.add(new EnumSetting.Builder<CompatibilityStrategy>()
        .name("compatibility-strategy")
        .description("Selects the plain-text syntax used when raw sending is enabled.")
        .defaultValue(CompatibilityStrategy.AUTO_PLAIN_FALLBACK)
        .visible(() -> sendMode.get() == SendMode.SEND_RAW || sendMode.get() == SendMode.SERVER_COMPAT)
        .build()
    );

    private final Setting<Boolean> ignoreCommands = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-commands")
        .description("Skips normal commands unless private messages or command argument text are enabled below.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> spacePreservation = sgGeneral.add(new BoolSetting.Builder()
        .name("space-preservation")
        .description("Keeps spaces from advancing gradient or rainbow indexes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> resetFormattingAtEnd = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-formatting-at-end")
        .description("Adds a reset marker after raw formatted text when the selected strategy supports it.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> solidColor = sgSolid.add(new ColorSetting.Builder()
        .name("solid-color")
        .description("Color used by SOLID mode.")
        .defaultValue(new SettingColor(255, 70, 70, 255))
        .visible(() -> mode.get() == ColorMode.SOLID)
        .build()
    );

    private final Setting<SettingColor> primaryColor = sgDual.add(new ColorSetting.Builder()
        .name("primary-color")
        .description("First color used by DUAL mode.")
        .defaultValue(new SettingColor(255, 70, 70, 255))
        .visible(() -> mode.get() == ColorMode.DUAL)
        .build()
    );

    private final Setting<SettingColor> secondaryColor = sgDual.add(new ColorSetting.Builder()
        .name("secondary-color")
        .description("Second color used by DUAL mode.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .visible(() -> mode.get() == ColorMode.DUAL)
        .build()
    );

    private final Setting<DualModeType> dualModeType = sgDual.add(new EnumSetting.Builder<DualModeType>()
        .name("dual-mode-type")
        .description("Controls how DUAL mode splits text.")
        .defaultValue(DualModeType.HALF_SPLIT)
        .visible(() -> mode.get() == ColorMode.DUAL)
        .build()
    );

    private final Setting<SettingColor> gradientStartColor = sgGradient.add(new ColorSetting.Builder()
        .name("gradient-start-color")
        .description("Start color used by GRADIENT mode.")
        .defaultValue(new SettingColor(255, 70, 70, 255))
        .visible(() -> mode.get() == ColorMode.GRADIENT)
        .build()
    );

    private final Setting<SettingColor> gradientEndColor = sgGradient.add(new ColorSetting.Builder()
        .name("gradient-end-color")
        .description("End color used by GRADIENT mode.")
        .defaultValue(new SettingColor(70, 120, 255, 255))
        .visible(() -> mode.get() == ColorMode.GRADIENT)
        .build()
    );

    private final Setting<GradientMode> gradientMode = sgGradient.add(new EnumSetting.Builder<GradientMode>()
        .name("gradient-mode")
        .description("Controls whether gradient color changes by line, word, or character.")
        .defaultValue(GradientMode.PER_CHAR)
        .visible(() -> mode.get() == ColorMode.GRADIENT)
        .build()
    );

    private final Setting<Boolean> gradientLoop = sgGradient.add(new BoolSetting.Builder()
        .name("gradient-loop")
        .description("Loops the gradient for animated local preview.")
        .defaultValue(false)
        .visible(() -> mode.get() == ColorMode.GRADIENT)
        .build()
    );

    private final Setting<Double> gradientSpeed = sgGradient.add(new DoubleSetting.Builder()
        .name("gradient-speed")
        .description("Local preview animation speed. Does not affect packets.")
        .defaultValue(0.2)
        .range(0.0, 5.0)
        .sliderRange(0.0, 2.0)
        .visible(() -> mode.get() == ColorMode.GRADIENT && gradientLoop.get())
        .build()
    );

    private final Setting<Double> rainbowSaturation = sgRainbow.add(new DoubleSetting.Builder()
        .name("rainbow-saturation")
        .description("Rainbow saturation.")
        .defaultValue(0.75)
        .range(0.0, 1.0)
        .sliderRange(0.0, 1.0)
        .visible(() -> mode.get() == ColorMode.RAINBOW)
        .build()
    );

    private final Setting<Double> rainbowBrightness = sgRainbow.add(new DoubleSetting.Builder()
        .name("rainbow-brightness")
        .description("Rainbow brightness.")
        .defaultValue(1.0)
        .range(0.0, 1.0)
        .sliderRange(0.0, 1.0)
        .visible(() -> mode.get() == ColorMode.RAINBOW)
        .build()
    );

    private final Setting<Double> rainbowSpeed = sgRainbow.add(new DoubleSetting.Builder()
        .name("rainbow-speed")
        .description("Local preview rainbow speed. Does not affect packets.")
        .defaultValue(0.35)
        .range(0.0, 5.0)
        .sliderRange(0.0, 2.0)
        .visible(() -> mode.get() == ColorMode.RAINBOW)
        .build()
    );

    private final Setting<RainbowMode> rainbowMode = sgRainbow.add(new EnumSetting.Builder<RainbowMode>()
        .name("rainbow-mode")
        .description("Controls whether rainbow colors change by character or word.")
        .defaultValue(RainbowMode.PER_CHAR)
        .visible(() -> mode.get() == ColorMode.RAINBOW)
        .build()
    );

    private final Setting<Boolean> randomizeLettersOnly = sgRandom.add(new BoolSetting.Builder()
        .name("randomize-letters-only")
        .description("Only applies random colors to letters.")
        .defaultValue(false)
        .visible(() -> mode.get() == ColorMode.RANDOM_PER_CHAR)
        .build()
    );

    private final Setting<RandomSeedMode> randomSeedMode = sgRandom.add(new EnumSetting.Builder<RandomSeedMode>()
        .name("random-seed-mode")
        .description("STABLE repeats colors for the same string. PER_SEND changes each send/preview refresh.")
        .defaultValue(RandomSeedMode.STABLE)
        .visible(() -> mode.get() == ColorMode.RANDOM_PER_CHAR)
        .build()
    );

    private final Setting<RandomPaletteMode> randomPaletteMode = sgRandom.add(new EnumSetting.Builder<RandomPaletteMode>()
        .name("random-palette-mode")
        .description("Uses full RGB or a custom palette.")
        .defaultValue(RandomPaletteMode.FULL_RGB)
        .visible(() -> mode.get() == ColorMode.RANDOM_PER_CHAR)
        .build()
    );

    private final Setting<List<SettingColor>> randomCustomPalette = sgRandom.add(new ColorListSetting.Builder()
        .name("random-custom-palette")
        .description("Palette used by RANDOM_PER_CHAR when CUSTOM_PALETTE is selected.")
        .defaultValue(List.of(new SettingColor(255, 70, 70), new SettingColor(70, 120, 255), new SettingColor(255, 255, 255)))
        .visible(() -> mode.get() == ColorMode.RANDOM_PER_CHAR && randomPaletteMode.get() == RandomPaletteMode.CUSTOM_PALETTE)
        .build()
    );

    private final Setting<PresetName> presetName = sgPreset.add(new EnumSetting.Builder<PresetName>()
        .name("preset-name")
        .description("Built-in or custom preset used by PRESET mode.")
        .defaultValue(PresetName.OCEAN)
        .visible(() -> mode.get() == ColorMode.PRESET)
        .build()
    );

    private final Setting<List<SettingColor>> customPreset1 = sgPreset.add(new ColorListSetting.Builder()
        .name("custom-1")
        .description("Colors used by preset CUSTOM_1.")
        .defaultValue(List.of(new SettingColor(255, 70, 70), new SettingColor(255, 255, 255)))
        .visible(() -> mode.get() == ColorMode.PRESET && presetName.get() == PresetName.CUSTOM_1)
        .build()
    );

    private final Setting<List<SettingColor>> customPreset2 = sgPreset.add(new ColorListSetting.Builder()
        .name("custom-2")
        .description("Colors used by preset CUSTOM_2.")
        .defaultValue(List.of(new SettingColor(0, 183, 255), new SettingColor(182, 92, 255)))
        .visible(() -> mode.get() == ColorMode.PRESET && presetName.get() == PresetName.CUSTOM_2)
        .build()
    );

    private final Setting<String> customPresetNotes = sgPreset.add(new StringSetting.Builder()
        .name("preset-notes")
        .description("Optional user label for custom preset experiments.")
        .defaultValue("")
        .visible(() -> mode.get() == ColorMode.PRESET)
        .build()
    );

    private final Setting<Boolean> applyNormalChat = sgApply.add(new BoolSetting.Builder()
        .name("normal-chat")
        .description("Applies formatting to normal chat messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> applyPrivateMessages = sgApply.add(new BoolSetting.Builder()
        .name("private-messages")
        .description("Applies formatting to /msg, /tell, /w, and /r message text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> applyCommandArgumentText = sgApply.add(new BoolSetting.Builder()
        .name("command-argument-text")
        .description("Applies formatting to text after the first argument of other commands.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> ignoredCommandPrefixes = sgApply.add(new StringListSetting.Builder()
        .name("ignored-command-prefixes")
        .description("Command prefixes that ColorChat never modifies.")
        .defaultValue("/login", "/register", "/l", "/reg")
        .build()
    );

    public ColorChat() {
        super(Eclipse.CHAT, "color-chat", "Local chat color preview and safe outgoing text formatting helper.");
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onMessageSend(SendMessageEvent event) {
        event.message = sendTransformer.transform(event.message, this);
    }

    public boolean formattingEnabled() {
        return isActive() && mode.get() != ColorMode.OFF && mode.get() != ColorMode.PLAIN;
    }

    public ColorMode mode() {
        return mode.get();
    }

    public boolean previewEnabled() {
        return formattingEnabled() && previewEnabled.get() && previewInChatScreen.get();
    }

    public boolean stripFormattingBeforeSend() {
        return stripFormattingBeforeSend.get();
    }

    public SendMode sendMode() {
        return sendMode.get();
    }

    public CompatibilityStrategy compatibilityStrategy() {
        return compatibilityStrategy.get();
    }

    public boolean ignoreCommands() {
        return ignoreCommands.get();
    }

    public boolean spacePreservation() {
        return spacePreservation.get();
    }

    public boolean resetFormattingAtEnd() {
        return resetFormattingAtEnd.get();
    }

    public boolean applyNormalChat() {
        return applyNormalChat.get();
    }

    public boolean applyPrivateMessages() {
        return applyPrivateMessages.get();
    }

    public boolean applyCommandArgumentText() {
        return applyCommandArgumentText.get();
    }

    public List<String> ignoredCommandPrefixes() {
        return ignoredCommandPrefixes.get();
    }

    public SettingColor solidColor() {
        return solidColor.get();
    }

    public SettingColor primaryColor() {
        return primaryColor.get();
    }

    public SettingColor secondaryColor() {
        return secondaryColor.get();
    }

    public DualModeType dualModeType() {
        return dualModeType.get();
    }

    public SettingColor gradientStartColor() {
        return gradientStartColor.get();
    }

    public SettingColor gradientEndColor() {
        return gradientEndColor.get();
    }

    public GradientMode gradientMode() {
        return gradientMode.get();
    }

    public boolean gradientLoop() {
        return gradientLoop.get();
    }

    public double gradientSpeed() {
        return gradientSpeed.get();
    }

    public double rainbowSaturation() {
        return rainbowSaturation.get();
    }

    public double rainbowBrightness() {
        return rainbowBrightness.get();
    }

    public double rainbowSpeed() {
        return rainbowSpeed.get();
    }

    public RainbowMode rainbowMode() {
        return rainbowMode.get();
    }

    public boolean randomizeLettersOnly() {
        return randomizeLettersOnly.get();
    }

    public RandomSeedMode randomSeedMode() {
        return randomSeedMode.get();
    }

    public RandomPaletteMode randomPaletteMode() {
        return randomPaletteMode.get();
    }

    public List<SettingColor> randomCustomPalette() {
        return randomCustomPalette.get();
    }

    public PresetName presetName() {
        return presetName.get();
    }

    public List<SettingColor> customPreset1() {
        return customPreset1.get();
    }

    public List<SettingColor> customPreset2() {
        return customPreset2.get();
    }

    public String customPresetNotes() {
        return customPresetNotes.get();
    }
}

