package eclipse.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public final class EclipseToastOverlay {
    private static final long DURATION_MS = 3000L;
    private static final int MAX_WIDTH = 340;
    private static final int MIN_WIDTH = 190;

    private static String title;
    private static String body;
    private static ItemStack icon = ItemStack.EMPTY;
    private static int accent;
    private static long startedAt;

    private EclipseToastOverlay() {
    }

    public static void show(String title, String body, int accent) {
        show(title, body, ItemStack.EMPTY, accent);
    }

    public static void show(String title, String body, ItemStack icon, int accent) {
        EclipseToastOverlay.title = title;
        EclipseToastOverlay.body = body;
        EclipseToastOverlay.icon = icon == null ? ItemStack.EMPTY : icon.copy();
        EclipseToastOverlay.accent = accent;
        EclipseToastOverlay.startedAt = System.currentTimeMillis();
    }

    public static void render(DrawContext context) {
        if (title == null || body == null) return;

        long age = System.currentTimeMillis() - startedAt;
        if (age >= DURATION_MS) {
            clear();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        TextRenderer text = mc.textRenderer;
        boolean hasIcon = !icon.isEmpty();
        int iconSpace = hasIcon ? 24 : 0;
        String titleText = fit(text, title, MAX_WIDTH - 28 - iconSpace);
        String bodyText = fit(text, body, MAX_WIDTH - 28 - iconSpace);

        int contentWidth = Math.max(text.getWidth(titleText), text.getWidth(bodyText));
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, contentWidth + 28 + iconSpace));
        int height = 34;
        int x = (context.getScaledWindowWidth() - width) / 2;

        double appear = Math.min(1.0, age / 180.0);
        double disappear = Math.min(1.0, (DURATION_MS - age) / 220.0);
        double progress = Math.max(0.0, Math.min(1.0, Math.min(appear, disappear)));
        int y = 10 + (int) Math.round((1.0 - progress) * -18.0);
        int alpha = (int) Math.round(225.0 * progress);

        int bg = (alpha << 24) | 0x101216;
        int edge = ((int) Math.round(255.0 * progress) << 24) | (accent & 0x00FFFFFF);
        int soft = ((int) Math.round(70.0 * progress) << 24) | (accent & 0x00FFFFFF);
        int textColor = ((int) Math.round(255.0 * progress) << 24) | 0xEAF7F2;
        int mutedColor = ((int) Math.round(210.0 * progress) << 24) | 0xB8C4C0;

        context.fill(x + 2, y + 2, x + width + 2, y + height + 2, ((int) Math.round(95.0 * progress) << 24));
        context.fill(x, y, x + width, y + height, bg);
        context.fill(x, y, x + 3, y + height, edge);
        context.fillGradient(x + 3, y, x + width, y + 1, soft, 0x00000000);
        context.fillGradient(x + 3, y + height - 1, x + width, y + height, 0x00000000, soft);

        int textX = x + 12;
        if (hasIcon) {
            context.drawItem(icon, x + 10, y + 9);
            textX += 24;
        }

        context.drawTextWithShadow(text, titleText, textX, y + 6, textColor);
        context.drawTextWithShadow(text, bodyText, textX, y + 19, mutedColor);
    }

    private static void clear() {
        title = null;
        body = null;
        icon = ItemStack.EMPTY;
        startedAt = 0L;
    }

    private static String fit(TextRenderer text, String value, int maxWidth) {
        if (text.getWidth(value) <= maxWidth) return value;

        String ellipsis = "...";
        int max = Math.max(0, maxWidth - text.getWidth(ellipsis));
        String result = value;
        while (!result.isEmpty() && text.getWidth(result) > max) {
            result = result.substring(0, result.length() - 1);
        }

        return result + ellipsis;
    }
}
