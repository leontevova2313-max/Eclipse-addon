package eclipse.modules.chat.colorchat;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;

public final class PresetManager {
    private PresetManager() {
    }

    public static List<Integer> colors(PresetName preset, List<SettingColor> custom1, List<SettingColor> custom2) {
        return switch (preset) {
            case RED_WHITE -> List.of(0xFF3B3B, 0xFFFFFF);
            case GOLD -> List.of(0xFFF08A, 0xFFB000, 0xFFF7C2);
            case OCEAN -> List.of(0x00B7FF, 0x006DFF, 0x00FFD1);
            case LIME_PURPLE -> List.of(0xA7FF2E, 0xB65CFF);
            case RAINBOW_SOFT -> List.of(0xFF7A7A, 0xFFD36E, 0xFFFF8A, 0x8AFFA2, 0x7AD7FF, 0xC58AFF);
            case CUSTOM_1 -> customColors(custom1, 0xFF3B3B, 0xFFFFFF);
            case CUSTOM_2 -> customColors(custom2, 0x00B7FF, 0xB65CFF);
        };
    }

    private static List<Integer> customColors(List<SettingColor> colors, int fallbackA, int fallbackB) {
        if (colors == null || colors.isEmpty()) return List.of(fallbackA, fallbackB);

        List<Integer> out = new ArrayList<>(colors.size());
        for (SettingColor color : colors) out.add(ColorUtil.rgb(color));
        return out;
    }
}

