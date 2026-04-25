package eclipse.gui.client;

import eclipse.client.runtime.ClientRuntime;
import eclipse.client.theme.ThemeTokens;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class EclipseClientTheme {
    public static final int BG = 0x00000000;
    public static final int SURFACE = 0xFF111418;
    public static final int SURFACE_SOFT = 0xFF151A20;
    public static final int SURFACE_INSET = 0xFF0E1116;
    public static final int SURFACE_DARK = 0xFF1C222A;
    public static final int LINE = 0x33343D4A;
    public static final int HIGHLIGHT = 0x33FF6A3D;
    public static final int HIGHLIGHT_SOFT = 0x22FF6A3D;
    public static final int SHADOW = 0x88000000;
    public static final int SHADOW_SOFT = 0x44000000;
    public static final int TEXT = 0xFFE9EEF7;
    public static final int TEXT_MUTED = 0xFFC7CFDA;
    public static final int TEXT_FAINT = 0xFF7C889A;
    public static final int TEXT_INVERT = 0xFFF6F8FC;
    public static final int ACCENT = 0xFFFF6A3D;
    public static final int ACCENT_SOFT = 0xFF6A2A1E;
    public static final int CHIP_ACTIVE = 0x66FF6A3D;
    public static final int CHIP_INACTIVE = 0x44131A20;
    public static final int TOGGLE_TRACK_ON = 0xFFFF6A3D;
    public static final int TOGGLE_TRACK_OFF = 0xFF394252;
    public static final int GOOD = 0xFF63D39A;
    public static final int WARN = 0xFFFFB066;
    public static final int ALERT = 0xFFFF5A4F;

    private static final float MAIN_PANEL_ALPHA = 0.12f;
    private static final float DETAIL_PANEL_ALPHA = 0.18f;
    private static final float HUD_PANEL_ALPHA = 0.20f;
    private static float renderAlpha = 1.0f;

    private EclipseClientTheme() {
    }

    private static ThemeTokens theme() {
        return ClientRuntime.theme().active();
    }

    public static int text() {
        return fade(TEXT);
    }

    public static int textMuted() {
        return fade(TEXT_MUTED);
    }

    public static int textFaint() {
        return fade(TEXT_FAINT);
    }

    public static int textInvert() {
        return fade(TEXT_INVERT);
    }

    public static int accent() {
        return fade(ACCENT);
    }

    public static int good() {
        return fade(GOOD);
    }

    public static int warn() {
        return fade(WARN);
    }

    public static void renderAlpha(float alpha) {
        renderAlpha = MathHelper.clamp(alpha, 0.0f, 1.0f);
    }

    public static void drawWorkspaceBackground(DrawContext context, int width, int height) {
        // Full transparent backdrop on purpose. Keep only ultra-subtle scan hints.
        int line = alpha(ACCENT, 0.04f);
        context.fill(18, 14, width - 18, 15, line);
        context.fill(18, height - 15, width - 18, height - 14, line);
    }

    public static void drawDockTray(DrawContext context, int x, int y, int w, int h) {
        int fill = alpha(SURFACE_SOFT, 0.16f);
        int edge = alpha(0xFFFFFFFF, 0.08f);
        int accent = alpha(ACCENT, 0.16f);
        context.fill(x, y + 2, x + w, y + h - 2, fill);
        context.fill(x + 2, y, x + w - 2, y + h, fill);
        context.fill(x + 2, y + 1, x + w - 2, y + 2, edge);
        context.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, alpha(0xFF000000, 0.20f));
        context.fill(x + 6, y + h - 3, x + w - 6, y + h - 2, accent);
        context.fill(x + 8, y + 4, x + w - 8, y + 5, alpha(0xFFFFFFFF, 0.03f));
    }


    public static void drawRaisedPanel(DrawContext context, int x, int y, int w, int h) {
        drawFramedPanel(context, x, y, w, h, alpha(SURFACE, MAIN_PANEL_ALPHA), alpha(0xFFFFFFFF, 0.06f), alpha(ACCENT, 0.18f));
    }

    public static void drawSoftPanel(DrawContext context, int x, int y, int w, int h) {
        drawFramedPanel(context, x, y, w, h, alpha(SURFACE_SOFT, MAIN_PANEL_ALPHA), alpha(0xFFFFFFFF, 0.04f), alpha(ACCENT, 0.12f));
    }

    public static void drawInsetPanel(DrawContext context, int x, int y, int w, int h) {
        drawFramedPanel(context, x, y, w, h, alpha(SURFACE_INSET, DETAIL_PANEL_ALPHA), alpha(0xFFFFFFFF, 0.05f), alpha(0xFF000000, 0.16f));
    }

    public static void drawGlassPanel(DrawContext context, int x, int y, int w, int h, boolean selected) {
        int fill = alpha(selected ? SURFACE_DARK : SURFACE_SOFT, selected ? 0.18f : 0.10f);
        drawFramedPanel(context, x, y, w, h, fill, alpha(0xFFFFFFFF, 0.04f), selected ? alpha(ACCENT, 0.55f) : alpha(ACCENT, 0.16f));
        if (selected) {
            int pulse = alpha(ALERT, 0.26f);
            context.fill(x + 1, y + 1, x + 3, y + h - 1, pulse);
            context.fill(x + 4, y + 1, x + w - 4, y + 2, alpha(ACCENT, 0.14f));
            context.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, alpha(ALERT, 0.12f));
        }
    }

    public static void drawHudPanel(DrawContext context, int x, int y, int w, int h) {
        drawFramedPanel(context, x, y, w, h, alpha(SURFACE_INSET, HUD_PANEL_ALPHA), alpha(0xFFFFFFFF, 0.05f), alpha(ACCENT, 0.20f));
    }

    public static void drawCard(DrawContext context, int x, int y, int w, int h, boolean selected) {
        drawGlassPanel(context, x, y, w, h, selected);
    }

    public static void drawHeaderChip(DrawContext context, int x, int y, int w, int h, boolean active) {
        int fill = active ? alpha(ACCENT_SOFT, 0.34f) : alpha(SURFACE_SOFT, 0.12f);
        int border = active ? alpha(ACCENT, 0.85f) : alpha(0xFFFFFFFF, 0.08f);
        drawFramedPanel(context, x, y, w, h, fill, alpha(0xFFFFFFFF, 0.04f), border);
        if (active) {
            context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, alpha(ALERT, 0.60f));
        }
    }

    public static void drawMiniChip(DrawContext context, int x, int y, int w, int h, boolean active) {
        if (active) drawDarkButton(context, x, y, w, h);
        else drawInsetPanel(context, x, y, w, h);
    }

    public static void drawSearchCapsule(DrawContext context, int x, int y, int w, int h) {
        drawInsetPanel(context, x, y, w, h);
    }

    public static void drawDarkButton(DrawContext context, int x, int y, int w, int h) {
        drawFramedPanel(context, x, y, w, h, alpha(ACCENT_SOFT, 0.42f), alpha(0xFFFFFFFF, 0.05f), alpha(ACCENT, 0.90f));
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, alpha(ALERT, 0.50f));
    }

    public static void drawDivider(DrawContext context, int x, int y, int w) {
        context.fill(x, y, x + w, y + 1, alpha(ACCENT, 0.14f));
    }

    public static void drawToggle(DrawContext context, int x, int y, int w, int h, boolean on) {
        drawInsetPanel(context, x, y, w, h);
        int innerX = x + 3;
        int innerY = y + 3;
        int innerW = w - 6;
        int innerH = h - 6;
        context.fill(innerX, innerY, innerX + innerW, innerY + innerH, on ? alpha(ACCENT, 0.92f) : alpha(0xFF2B333E, 0.85f));
        int knobSize = innerH;
        int knobX = on ? innerX + innerW - knobSize : innerX;
        drawFramedPanel(context, knobX, innerY, knobSize, knobSize, alpha(0xFFF4F5F7, 0.82f), alpha(0xFFFFFFFF, 0.10f), alpha(0xFF000000, 0.24f));
    }

    public static void drawStatusDot(DrawContext context, int x, int y, boolean active) {
        int color = active ? good() : alpha(0xFF596271, 0.90f);
        context.fill(x, y, x + 6, y + 6, color);
        if (active) context.fill(x + 1, y + 1, x + 5, y + 5, alpha(0xFFFFFFFF, 0.12f));
    }

    public static void drawCanvasGrid(DrawContext context, int x, int y, int w, int h, int spacing) {
        int line = alpha(ACCENT, 0.04f);
        for (int gx = x; gx <= x + w; gx += spacing) context.fill(gx, y, gx + 1, y + h, line);
        for (int gy = y; gy <= y + h; gy += spacing) context.fill(x, gy, x + w, gy + 1, line);
    }

    public static void drawGraph(DrawContext context, int[] samples, int count, int x, int y, int w, int h, int color) {
        if (count <= 1) return;
        int max = 1;
        for (int sample : samples) if (sample > max) max = sample;
        int start = Math.max(0, samples.length - count);
        int prevX = x;
        int prevY = y + h - scale(samples[start], max, h);
        for (int i = start + 1; i < samples.length; i++) {
            int index = i - start;
            int px = x + (index * (w - 1)) / Math.max(1, count - 1);
            int py = y + h - scale(samples[i], max, h);
            drawLine(context, prevX, prevY, px, py, color);
            prevX = px;
            prevY = py;
        }
    }

    public static void drawIconButton(DrawContext context, TextRenderer text, int x, int y, int size, boolean active, String iconId) {
        drawHeaderChip(context, x, y, size, size, active);
        drawIconGlyph(context, text, x, y, size, active, iconId);
    }

    private static void drawIconGlyph(DrawContext context, TextRenderer text, int x, int y, int size, boolean active, String iconId) {
        int color = active ? textInvert() : textMuted();
        int accent = active ? alpha(ALERT, 0.78f) : alpha(ACCENT, 0.65f);
        int left = x + 5;
        int top = y + 5;
        int right = x + size - 5;
        int bottom = y + size - 5;
        int midX = (left + right) / 2;
        int midY = (top + bottom) / 2;

        switch (iconId) {
            case "workspace" -> {
                // four panel squares
                int s = 3;
                context.fill(left, top, left + s, top + s, color);
                context.fill(midX + 1, top, midX + 1 + s, top + s, color);
                context.fill(left, midY + 1, left + s, midY + 1 + s, color);
                context.fill(midX + 1, midY + 1, midX + 1 + s, midY + 1 + s, accent);
            }
            case "spatial" -> {
                // locator / route icon
                context.fill(midX, top, midX + 1, bottom - 2, color);
                context.fill(left + 1, midY, right - 1, midY + 1, color);
                context.fill(midX - 1, bottom - 3, midX + 2, bottom, accent);
                context.fill(right - 3, top + 1, right, top + 4, accent);
            }
            case "hud" -> {
                // monitor frame
                context.fill(left, top + 1, right, top + 2, color);
                context.fill(left, top + 1, left + 1, bottom - 2, color);
                context.fill(right - 1, top + 1, right, bottom - 2, color);
                context.fill(left, bottom - 2, right, bottom - 1, color);
                context.fill(midX - 2, bottom - 1, midX + 3, bottom, accent);
            }
            case "inspector" -> {
                // magnifier
                context.fill(left + 1, top + 1, right - 3, top + 2, color);
                context.fill(left + 1, bottom - 4, right - 3, bottom - 3, color);
                context.fill(left + 1, top + 2, left + 2, bottom - 3, color);
                context.fill(right - 4, top + 2, right - 3, bottom - 3, color);
                context.fill(right - 3, bottom - 3, right, bottom, accent);
            }
            case "theme-dark" -> {
                // crescent-ish split disk
                context.fill(left + 1, top + 1, right - 1, bottom - 1, color);
                context.fill(midX, top + 1, right - 2, bottom - 1, alpha(SURFACE_INSET, 0.88f));
                context.fill(left + 1, top + 1, left + 2, bottom - 1, accent);
            }
            case "theme-light" -> {
                // sun + rays
                context.fill(midX - 1, midY - 1, midX + 2, midY + 2, color);
                context.fill(midX, top, midX + 1, top + 2, accent);
                context.fill(midX, bottom - 2, midX + 1, bottom, accent);
                context.fill(left, midY, left + 2, midY + 1, accent);
                context.fill(right - 2, midY, right, midY + 1, accent);
            }
            case "close" -> {
                context.fill(left, top, left + 1, top + 1, accent);
                context.fill(right - 1, top, right, top + 1, accent);
                context.fill(left + 2, top + 2, left + 3, top + 3, color);
                context.fill(right - 3, top + 2, right - 2, top + 3, color);
                context.fill(midX, midY, midX + 1, midY + 1, color);
                context.fill(left + 2, bottom - 3, left + 3, bottom - 2, color);
                context.fill(right - 3, bottom - 3, right - 2, bottom - 2, color);
                context.fill(left, bottom - 1, left + 1, bottom, accent);
                context.fill(right - 1, bottom - 1, right, bottom, accent);
            }
            default -> {
                drawCentered(text, context, "?", x, y + ((size - 8) / 2), size, color);
            }
        }
    }

    private static void drawFramedPanel(DrawContext context, int x, int y, int w, int h, int fill, int topGlow, int accentLine) {
        context.fill(x, y, x + w, y + h, fill);
        context.fill(x + 1, y + 1, x + w - 1, y + 2, topGlow);
        int shadow = alpha(0xFF000000, 0.18f);
        context.fill(x, y + h - 1, x + w, y + h, shadow);
        context.fill(x + w - 1, y, x + w, y + h, shadow);
        drawCornerBrackets(context, x, y, w, h, accentLine);
        int scan = alpha(0xFFFFFFFF, 0.015f);
        for (int lineY = y + 4; lineY < y + h - 3; lineY += 5) {
            context.fill(x + 2, lineY, x + w - 2, lineY + 1, scan);
        }
    }

    private static void drawCornerBrackets(DrawContext context, int x, int y, int w, int h, int color) {
        int arm = Math.min(7, Math.max(4, Math.min(w, h) / 4));
        // top-left
        context.fill(x, y, x + arm, y + 1, color);
        context.fill(x, y, x + 1, y + arm, color);
        // top-right
        context.fill(x + w - arm, y, x + w, y + 1, color);
        context.fill(x + w - 1, y, x + w, y + arm, color);
        // bottom-left
        context.fill(x, y + h - 1, x + arm, y + h, color);
        context.fill(x, y + h - arm, x + 1, y + h, color);
        // bottom-right
        context.fill(x + w - arm, y + h - 1, x + w, y + h, color);
        context.fill(x + w - 1, y + h - arm, x + w, y + h, color);
    }

    private static void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            context.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static int scale(int value, int max, int height) {
        return MathHelper.clamp((int) ((value / (double) max) * height), 0, height);
    }

    public static void drawText(TextRenderer text, DrawContext context, String value, int x, int y) {
        context.drawText(text, value, x, y, text(), false);
    }

    public static void drawMutedText(TextRenderer text, DrawContext context, String value, int x, int y) {
        context.drawText(text, value, x, y, textMuted(), false);
    }

    public static void drawFaintText(TextRenderer text, DrawContext context, String value, int x, int y) {
        context.drawText(text, value, x, y, textFaint(), false);
    }

    public static void drawInvertText(TextRenderer text, DrawContext context, String value, int x, int y) {
        context.drawText(text, value, x, y, textInvert(), false);
    }

    public static void drawCentered(TextRenderer text, DrawContext context, String value, int x, int y, int w, int color) {
        int tx = x + (w - text.getWidth(value)) / 2;
        context.drawText(text, value, tx, y, color, false);
    }

    public static void drawScriptText(TextRenderer text, DrawContext context, String value, int x, int y, int color) {
        if (value == null || value.isEmpty()) return;
        int cursor = x;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            int yo = switch (i % 4) {
                case 0 -> 0;
                case 1 -> -1;
                case 2 -> 0;
                default -> 1;
            };
            context.drawText(text, String.valueOf(c), cursor, y + yo, color, false);
            cursor += text.getWidth(String.valueOf(c));
        }
        if (value.length() > 2) {
            context.fill(x, y + 9, cursor, y + 10, alpha(ACCENT, 0.08f));
        }
    }

    public static void drawScriptCentered(TextRenderer text, DrawContext context, String value, int x, int y, int w, int color) {
        int tx = x + (w - text.getWidth(value)) / 2;
        drawScriptText(text, context, value, tx, y, color);
    }

    public static int alpha(int color, float alpha) {
        int a = MathHelper.clamp((int) (alpha * renderAlpha * 255.0f), 0, 255);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int fade(int color) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int a = MathHelper.clamp((int) (baseAlpha * renderAlpha), 0, 255);
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
