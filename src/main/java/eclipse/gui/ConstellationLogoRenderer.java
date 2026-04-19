package eclipse.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

public final class ConstellationLogoRenderer {
    private static final Map<Character, Glyph> GLYPHS = new HashMap<>();
    private static final int DESIGN_HEIGHT = 72;
    private static final int LETTER_GAP = 9;

    static {
        glyph('E',
            points(0, 0, 42, 0, 0, 34, 34, 34, 0, 68, 44, 68),
            lines(0, 1, 0, 2, 2, 3, 2, 4, 4, 5),
            48
        );
        glyph('C',
            points(45, 6, 16, 0, 0, 19, 0, 50, 16, 68, 45, 62),
            lines(0, 1, 1, 2, 2, 3, 3, 4, 4, 5),
            50
        );
        glyph('L',
            points(0, 0, 0, 68, 43, 68),
            lines(0, 1, 1, 2),
            46
        );
        glyph('I',
            points(0, 0, 36, 0, 18, 0, 18, 68, 0, 68, 36, 68),
            lines(0, 1, 2, 3, 4, 5),
            40
        );
        glyph('P',
            points(0, 68, 0, 0, 35, 0, 48, 15, 36, 33, 0, 33),
            lines(0, 1, 1, 2, 2, 3, 3, 4, 4, 5),
            53
        );
        glyph('S',
            points(45, 5, 14, 0, 0, 18, 16, 34, 43, 34, 51, 52, 34, 68, 0, 62),
            lines(0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7),
            55
        );
    }

    private ConstellationLogoRenderer() {
    }

    public static int height(int width) {
        return Math.max(40, Math.round(width * DESIGN_HEIGHT / (float) designWidth("ECLIPSE")));
    }

    public static void render(DrawContext context, String text, int x, int y, int width, int color, int glowColor, int starSize, int lineAlpha, boolean twinkle) {
        int designWidth = designWidth(text);
        float scale = width / (float) designWidth;
        long now = System.currentTimeMillis();
        int cursor = x;

        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = GLYPHS.get(Character.toUpperCase(text.charAt(i)));
            if (glyph == null) {
                cursor += Math.round((24 + LETTER_GAP) * scale);
                continue;
            }

            drawGlyph(context, glyph, cursor, y, scale, color, glowColor, starSize, lineAlpha, twinkle, now, i);
            cursor += Math.round((glyph.width + LETTER_GAP) * scale);
        }
    }

    private static void drawGlyph(DrawContext context, Glyph glyph, int x, int y, float scale, int color, int glowColor, int starSize, int lineAlpha, boolean twinkle, long now, int glyphIndex) {
        int lineColor = withAlpha(color, lineAlpha);
        int glow = withAlpha(glowColor, Math.min(150, Math.max(20, alpha(glowColor))));

        for (int i = 0; i < glyph.lines.length; i += 2) {
            Point a = glyph.points[glyph.lines[i]];
            Point b = glyph.points[glyph.lines[i + 1]];
            int x1 = x + Math.round(a.x * scale);
            int y1 = y + Math.round(a.y * scale);
            int x2 = x + Math.round(b.x * scale);
            int y2 = y + Math.round(b.y * scale);
            drawLine(context, x1, y1, x2, y2, Math.max(1, Math.round(scale * 1.4F)), glow);
            drawLine(context, x1, y1, x2, y2, Math.max(1, Math.round(scale)), lineColor);
        }

        for (int i = 0; i < glyph.points.length; i++) {
            Point point = glyph.points[i];
            int px = x + Math.round(point.x * scale);
            int py = y + Math.round(point.y * scale);
            int pulse = 0;
            if (twinkle) {
                double phase = (now / 260.0) + glyphIndex * 0.9 + i * 1.7;
                pulse = (int) Math.round((Math.sin(phase) + 1.0) * 1.2);
            }

            int size = Math.max(2, Math.round(starSize * scale)) + pulse;
            fillCentered(context, px, py, size + 4, size + 4, withAlpha(glowColor, 70));
            fillCentered(context, px, py, size + 1, size + 1, withAlpha(color, 235));
            fillCentered(context, px, py, Math.max(1, size - 2), Math.max(1, size - 2), 0xFFFFFFFF);
        }
    }

    private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int thickness, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            fillCentered(context, x1, y1, thickness, thickness, color);
            return;
        }

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(x1 + (x2 - x1) * t);
            int y = Math.round(y1 + (y2 - y1) * t);
            fillCentered(context, x, y, thickness, thickness, color);
        }
    }

    private static void fillCentered(DrawContext context, int centerX, int centerY, int width, int height, int color) {
        int x1 = centerX - width / 2;
        int y1 = centerY - height / 2;
        context.fill(x1, y1, x1 + width, y1 + height, color);
    }

    private static int designWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = GLYPHS.get(Character.toUpperCase(text.charAt(i)));
            width += glyph != null ? glyph.width : 24;
            if (i + 1 < text.length()) width += LETTER_GAP;
        }

        return width;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    private static void glyph(char c, Point[] points, int[] lines, int width) {
        GLYPHS.put(c, new Glyph(points, lines, width));
    }

    private static Point[] points(int... values) {
        Point[] points = new Point[values.length / 2];
        for (int i = 0; i < values.length; i += 2) {
            points[i / 2] = new Point(values[i], values[i + 1]);
        }
        return points;
    }

    private static int[] lines(int... values) {
        return values;
    }

    private record Glyph(Point[] points, int[] lines, int width) {
    }

    private record Point(int x, int y) {
    }
}
