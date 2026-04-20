package eclipse.modules.chat.colorchat;

import eclipse.modules.chat.ColorChat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Random;

public final class ChatColorFormatter {
    private final GradientTextBuilder gradientBuilder = new GradientTextBuilder();
    private final RainbowTextBuilder rainbowBuilder = new RainbowTextBuilder();
    private final RandomPaletteTextBuilder randomBuilder = new RandomPaletteTextBuilder();

    public Text preview(String input, ColorChat module, boolean animated) {
        if (input == null || input.isEmpty()) return Text.empty();

        return switch (module.mode()) {
            case OFF, PLAIN -> Text.literal(input);
            case SOLID -> Text.literal(input).setStyle(ColorUtil.style(ColorUtil.rgb(module.solidColor())));
            case DUAL -> dualPreview(input, module);
            case GRADIENT -> gradientBuilder.build(
                input,
                ColorUtil.rgb(module.gradientStartColor()),
                ColorUtil.rgb(module.gradientEndColor()),
                module.gradientMode(),
                module.gradientLoop(),
                module.spacePreservation(),
                animated ? ((System.currentTimeMillis() % 100000L) / 1000.0 * module.gradientSpeed()) : 0.0
            );
            case RAINBOW -> rainbowBuilder.build(
                input,
                module.rainbowSaturation(),
                module.rainbowBrightness(),
                module.rainbowSpeed(),
                module.rainbowMode(),
                module.spacePreservation(),
                animated
            );
            case RANDOM_PER_CHAR -> randomBuilder.build(
                input,
                module.randomizeLettersOnly(),
                module.randomSeedMode(),
                module.randomPaletteMode(),
                module.randomCustomPalette()
            );
            case PRESET -> presetPreview(input, module);
        };
    }

    public String raw(String input, ColorChat module, CompatibilityStrategy strategy) {
        if (input == null || input.isEmpty()) return input;
        if (module.mode() == ColorMode.OFF || module.mode() == ColorMode.PLAIN) return input;

        String formatted = switch (module.mode()) {
            case SOLID -> rawToken(input, ColorUtil.rgb(module.solidColor()), strategy);
            case DUAL -> dualRaw(input, module, strategy);
            case GRADIENT -> gradientRaw(input, module, strategy);
            case RAINBOW -> rainbowRaw(input, module, strategy);
            case RANDOM_PER_CHAR -> randomRaw(input, module, strategy);
            case PRESET -> presetRaw(input, module, strategy);
            case OFF, PLAIN -> input;
        };

        return module.resetFormattingAtEnd() ? formatted + reset(strategy) : formatted;
    }

    private Text dualPreview(String input, ColorChat module) {
        int primary = ColorUtil.rgb(module.primaryColor());
        int secondary = ColorUtil.rgb(module.secondaryColor());
        return switch (module.dualModeType()) {
            case HALF_SPLIT -> splitPreview(input, primary, secondary);
            case WORD_SPLIT -> wordAlternatingPreview(input, primary, secondary);
            case ALTERNATE_CHAR -> alternatingCharPreview(input, primary, secondary, module.spacePreservation());
        };
    }

    private Text splitPreview(String input, int primary, int secondary) {
        int split = input.length() / 2;
        return Text.empty()
            .append(Text.literal(input.substring(0, split)).setStyle(ColorUtil.style(primary)))
            .append(Text.literal(input.substring(split)).setStyle(ColorUtil.style(secondary)));
    }

    private Text wordAlternatingPreview(String input, int primary, int secondary) {
        MutableText out = Text.empty();
        String[] parts = input.split("((?<=\\s)|(?=\\s))");
        int word = 0;
        for (String part : parts) {
            if (part.isBlank()) {
                out.append(Text.literal(part));
            } else {
                out.append(Text.literal(part).setStyle(ColorUtil.style((word++ & 1) == 0 ? primary : secondary)));
            }
        }
        return out;
    }

    private Text alternatingCharPreview(String input, int primary, int secondary, boolean preserveSpaces) {
        MutableText out = Text.empty();
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c) && preserveSpaces) {
                out.append(Text.literal(String.valueOf(c)));
            } else {
                out.append(Text.literal(String.valueOf(c)).setStyle(ColorUtil.style((index++ & 1) == 0 ? primary : secondary)));
            }
        }
        return out;
    }

    private Text presetPreview(String input, ColorChat module) {
        List<Integer> colors = PresetManager.colors(module.presetName(), module.customPreset1(), module.customPreset2());
        if (colors.size() == 1) return Text.literal(input).setStyle(ColorUtil.style(colors.get(0)));

        MutableText out = Text.empty();
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c) && module.spacePreservation()) {
                out.append(Text.literal(String.valueOf(c)));
            } else {
                out.append(Text.literal(String.valueOf(c)).setStyle(ColorUtil.style(colors.get(index++ % colors.size()))));
            }
        }
        return out;
    }

    private String dualRaw(String input, ColorChat module, CompatibilityStrategy strategy) {
        int primary = ColorUtil.rgb(module.primaryColor());
        int secondary = ColorUtil.rgb(module.secondaryColor());
        return switch (module.dualModeType()) {
            case HALF_SPLIT -> {
                int split = input.length() / 2;
                yield rawToken(input.substring(0, split), primary, strategy) + rawToken(input.substring(split), secondary, strategy);
            }
            case WORD_SPLIT -> rawWords(input, strategy, i -> (i & 1) == 0 ? primary : secondary);
            case ALTERNATE_CHAR -> rawChars(input, strategy, module.spacePreservation(), i -> (i & 1) == 0 ? primary : secondary);
        };
    }

    private String gradientRaw(String input, ColorChat module, CompatibilityStrategy strategy) {
        int start = ColorUtil.rgb(module.gradientStartColor());
        int end = ColorUtil.rgb(module.gradientEndColor());
        if (module.gradientMode() == GradientMode.PER_WORD) {
            int words = Math.max(1, countWords(input));
            return rawWords(input, strategy, i -> ColorUtil.lerp(start, end, words <= 1 ? 0.0 : i / (double) (words - 1)));
        }

        int length = Math.max(1, visibleLength(input, module.spacePreservation()) - 1);
        return rawChars(input, strategy, module.spacePreservation(), i -> ColorUtil.lerp(start, end, length <= 0 ? 0.0 : i / (double) length));
    }

    private String rainbowRaw(String input, ColorChat module, CompatibilityStrategy strategy) {
        if (module.rainbowMode() == RainbowMode.PER_WORD) {
            int words = Math.max(1, countWords(input));
            return rawWords(input, strategy, i -> hsv(i / (double) words, module.rainbowSaturation(), module.rainbowBrightness()));
        }

        int length = Math.max(1, visibleLength(input, module.spacePreservation()));
        return rawChars(input, strategy, module.spacePreservation(), i -> hsv(i / (double) length, module.rainbowSaturation(), module.rainbowBrightness()));
    }

    private String randomRaw(String input, ColorChat module, CompatibilityStrategy strategy) {
        Random random = new Random(module.randomSeedMode() == RandomSeedMode.STABLE ? input.hashCode() : System.nanoTime());
        List<Integer> palette = module.randomCustomPalette().stream().map(ColorUtil::rgb).toList();
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (module.randomizeLettersOnly() && !Character.isLetter(c)) {
                out.append(c);
                continue;
            }

            int color = module.randomPaletteMode() == RandomPaletteMode.CUSTOM_PALETTE && !palette.isEmpty()
                ? palette.get(random.nextInt(palette.size()))
                : random.nextInt(0x1000000);
            out.append(rawToken(String.valueOf(c), color, strategy));
        }

        return out.toString();
    }

    private String presetRaw(String input, ColorChat module, CompatibilityStrategy strategy) {
        List<Integer> colors = PresetManager.colors(module.presetName(), module.customPreset1(), module.customPreset2());
        return rawChars(input, strategy, module.spacePreservation(), i -> colors.get(i % colors.size()));
    }

    private String rawWords(String input, CompatibilityStrategy strategy, ColorProvider colorProvider) {
        StringBuilder out = new StringBuilder();
        String[] parts = input.split("((?<=\\s)|(?=\\s))");
        int word = 0;
        for (String part : parts) {
            if (part.isBlank()) out.append(part);
            else out.append(rawToken(part, colorProvider.color(word++), strategy));
        }
        return out.toString();
    }

    private String rawChars(String input, CompatibilityStrategy strategy, boolean preserveSpaces, ColorProvider colorProvider) {
        StringBuilder out = new StringBuilder();
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c) && preserveSpaces) out.append(c);
            else out.append(rawToken(String.valueOf(c), colorProvider.color(index++), strategy));
        }
        return out.toString();
    }

    private String rawToken(String token, int color, CompatibilityStrategy strategy) {
        return switch (strategy) {
            case LEGACY_AMPERSAND -> "&" + ColorUtil.hex(color) + token;
            case MINIMESSAGE_TEXT -> "<" + ColorUtil.hex(color) + ">" + escapeMiniMessage(token);
            case AUTO_PLAIN_FALLBACK -> token;
        };
    }

    private String reset(CompatibilityStrategy strategy) {
        return switch (strategy) {
            case LEGACY_AMPERSAND -> "&r";
            case MINIMESSAGE_TEXT -> "<reset>";
            case AUTO_PLAIN_FALLBACK -> "";
        };
    }

    private String escapeMiniMessage(String token) {
        return token.replace("\\", "\\\\").replace("<", "\\<");
    }

    private int visibleLength(String input, boolean preserveSpaces) {
        if (!preserveSpaces) return input.length();

        int length = 0;
        for (int i = 0; i < input.length(); i++) {
            if (!Character.isWhitespace(input.charAt(i))) length++;
        }
        return length;
    }

    private int countWords(String input) {
        int count = 0;
        for (String part : input.split("\\s+")) {
            if (!part.isBlank()) count++;
        }
        return count;
    }

    private int hsv(double hue, double saturation, double brightness) {
        java.awt.Color color = java.awt.Color.getHSBColor((float) hue, (float) saturation, (float) brightness);
        return color.getRGB() & 0xFFFFFF;
    }

    @FunctionalInterface
    private interface ColorProvider {
        int color(int index);
    }
}

