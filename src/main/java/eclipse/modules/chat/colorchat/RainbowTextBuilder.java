package eclipse.modules.chat.colorchat;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class RainbowTextBuilder {
    public Text build(String input, double saturation, double brightness, double speed, RainbowMode mode, boolean preserveSpaces, boolean animated) {
        if (input.isEmpty()) return Text.empty();

        double phase = animated ? (System.currentTimeMillis() % 100000L) / 1000.0 * speed : 0.0;
        return mode == RainbowMode.PER_WORD
            ? perWord(input, saturation, brightness, phase)
            : perChar(input, saturation, brightness, preserveSpaces, phase);
    }

    private Text perChar(String input, double saturation, double brightness, boolean preserveSpaces, double phase) {
        MutableText out = Text.empty();
        int length = Math.max(1, preserveSpaces ? input.replaceAll("\\s+", "").length() : input.length());
        int index = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c) && preserveSpaces) {
                out.append(Text.literal(String.valueOf(c)));
                continue;
            }

            out.append(Text.literal(String.valueOf(c)).setStyle(ColorUtil.style(hsv((index / (double) length + phase) % 1.0, saturation, brightness))));
            index++;
        }

        return out;
    }

    private Text perWord(String input, double saturation, double brightness, double phase) {
        MutableText out = Text.empty();
        String[] parts = input.split("((?<=\\s)|(?=\\s))");
        int words = 0;
        for (String part : parts) if (!part.isBlank()) words++;

        int index = 0;
        for (String part : parts) {
            if (part.isBlank()) {
                out.append(Text.literal(part));
                continue;
            }

            out.append(Text.literal(part).setStyle(ColorUtil.style(hsv((index / (double) Math.max(1, words) + phase) % 1.0, saturation, brightness))));
            index++;
        }

        return out;
    }

    private int hsv(double hue, double saturation, double brightness) {
        Color color = Color.fromHsv(hue, saturation, brightness);
        return (color.r << 16) | (color.g << 8) | color.b;
    }
}

