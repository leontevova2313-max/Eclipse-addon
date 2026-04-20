package eclipse.modules.chat.colorchat;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.Style;

final class ColorUtil {
    private ColorUtil() {
    }

    static int rgb(SettingColor color) {
        return ((clamp(color.r) & 0xFF) << 16) | ((clamp(color.g) & 0xFF) << 8) | (clamp(color.b) & 0xFF);
    }

    static int argb(SettingColor color) {
        return ((clamp(color.a) & 0xFF) << 24) | rgb(color);
    }

    static int lerp(int from, int to, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    static Style style(int rgb) {
        return Style.EMPTY.withColor(rgb);
    }

    static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static int lerpChannel(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

