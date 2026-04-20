package eclipse.modules.chat.colorchat;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Random;

public final class RandomPaletteTextBuilder {
    public Text build(String input, boolean lettersOnly, RandomSeedMode seedMode, RandomPaletteMode paletteMode, List<SettingColor> customPalette) {
        if (input.isEmpty()) return Text.empty();

        Random random = new Random(seedMode == RandomSeedMode.STABLE ? input.hashCode() : System.nanoTime());
        List<Integer> palette = customPalette(customPalette);
        MutableText out = Text.empty();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (lettersOnly && !Character.isLetter(c)) {
                out.append(Text.literal(String.valueOf(c)));
                continue;
            }

            int color = paletteMode == RandomPaletteMode.CUSTOM_PALETTE && !palette.isEmpty()
                ? palette.get(random.nextInt(palette.size()))
                : random.nextInt(0x1000000);
            out.append(Text.literal(String.valueOf(c)).setStyle(ColorUtil.style(color)));
        }

        return out;
    }

    private List<Integer> customPalette(List<SettingColor> colors) {
        if (colors == null || colors.isEmpty()) return List.of();
        return colors.stream().map(ColorUtil::rgb).toList();
    }
}

