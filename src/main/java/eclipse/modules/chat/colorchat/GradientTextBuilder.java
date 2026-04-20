package eclipse.modules.chat.colorchat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class GradientTextBuilder {
    public Text build(String input, int startColor, int endColor, GradientMode mode, boolean loop, boolean preserveSpaces, double phase) {
        if (input.isEmpty()) return Text.empty();

        return switch (mode) {
            case LINEAR, PER_CHAR -> perChar(input, startColor, endColor, loop, preserveSpaces, phase);
            case PER_WORD -> perWord(input, startColor, endColor, loop, phase);
        };
    }

    private Text perChar(String input, int startColor, int endColor, boolean loop, boolean preserveSpaces, double phase) {
        MutableText out = Text.empty();
        int steps = Math.max(1, visibleLength(input, preserveSpaces) - 1);
        int index = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c) && preserveSpaces) {
                out.append(Text.literal(String.valueOf(c)));
                continue;
            }

            double t = steps == 0 ? 0.0 : index / (double) steps;
            if (loop) t = triangular((t + phase) % 1.0);
            out.append(Text.literal(String.valueOf(c)).setStyle(ColorUtil.style(ColorUtil.lerp(startColor, endColor, t))));
            index++;
        }

        return out;
    }

    private Text perWord(String input, int startColor, int endColor, boolean loop, double phase) {
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

            double t = words <= 1 ? 0.0 : index / (double) (words - 1);
            if (loop) t = triangular((t + phase) % 1.0);
            out.append(Text.literal(part).setStyle(ColorUtil.style(ColorUtil.lerp(startColor, endColor, t))));
            index++;
        }

        return out;
    }

    private int visibleLength(String input, boolean preserveSpaces) {
        if (!preserveSpaces) return input.length();

        int length = 0;
        for (int i = 0; i < input.length(); i++) {
            if (!Character.isWhitespace(input.charAt(i))) length++;
        }
        return length;
    }

    private double triangular(double t) {
        return t <= 0.5 ? t * 2.0 : (1.0 - t) * 2.0;
    }
}

