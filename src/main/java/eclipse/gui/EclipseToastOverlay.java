package eclipse.gui;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Iterator;

public final class EclipseToastOverlay {
    private static final int DEFAULT_ACCENT = 0xFF47F2A3;
    private static final int DEFAULT_BACKGROUND = 0xE6101216;
    private static final int MAX_WIDTH = 340;
    private static final int MIN_WIDTH = 190;
    private static final int HEIGHT = 36;
    private static final int GAP = 7;
    private static final int MARGIN = 12;
    private static final Object EVENT_LISTENER = new EventListener();
    private static final ArrayDeque<Notice> NOTICES = new ArrayDeque<>();

    private static boolean registered;
    private static boolean useCustomNotifier = true;
    private static NotificationPosition position = NotificationPosition.TopRight;
    private static NotificationStyle style = NotificationStyle.Eclipse;
    private static double animationSpeed = 1.0;
    private static int maxNotifications = 4;
    private static int durationMs = 3000;
    private static int accentColor = DEFAULT_ACCENT;
    private static int backgroundColor = DEFAULT_BACKGROUND;

    private EclipseToastOverlay() {
    }

    public static void init() {
        if (registered) return;
        MeteorClient.EVENT_BUS.subscribe(EVENT_LISTENER);
        registered = true;
    }

    public static void configure(boolean useCustomNotifier, NotificationPosition position, double animationSpeed, int maxNotifications, int durationMs, int accentColor, int backgroundColor, NotificationStyle style) {
        EclipseToastOverlay.useCustomNotifier = useCustomNotifier;
        EclipseToastOverlay.position = position == null ? NotificationPosition.TopRight : position;
        EclipseToastOverlay.animationSpeed = Math.max(0.15, animationSpeed);
        EclipseToastOverlay.maxNotifications = Math.max(1, Math.min(8, maxNotifications));
        EclipseToastOverlay.durationMs = Math.max(700, durationMs);
        EclipseToastOverlay.accentColor = accentColor;
        EclipseToastOverlay.backgroundColor = backgroundColor;
        EclipseToastOverlay.style = style == null ? NotificationStyle.Eclipse : style;
        trimToLimit();
    }

    public static boolean showModuleToggle(Module module) {
        if (!useCustomNotifier || module == null) return false;

        boolean enabled = module.isActive();
        int accent = enabled ? 0xFF47F2A3 : 0xFFFF6B6B;
        String state = enabled ? "Enabled" : "Disabled";
        String body = module.category == null ? state : state + " in " + module.category.name;
        show(module.title, body, accent);
        return true;
    }

    public static void show(String title, String body, int accent) {
        show(title, body, ItemStack.EMPTY, accent);
    }

    public static void show(String title, String body, ItemStack icon, int accent) {
        if (title == null || body == null) return;

        NOTICES.addLast(new Notice(
            title,
            body,
            icon == null ? ItemStack.EMPTY : icon.copy(),
            forceAlpha(accent),
            System.currentTimeMillis()
        ));

        trimToLimit();
    }

    public static void render(Render2DEvent event) {
        if (event == null || event.drawContext == null) return;
        render(event.drawContext);
    }

    public static void render(DrawContext context) {
        if (context == null || NOTICES.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        long now = System.currentTimeMillis();
        removeExpired(now);
        if (NOTICES.isEmpty()) return;

        TextRenderer text = mc.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        boolean bottom = position == NotificationPosition.BottomLeft || position == NotificationPosition.BottomRight;

        int index = 0;
        Iterator<Notice> iterator = bottom ? NOTICES.descendingIterator() : NOTICES.iterator();
        while (iterator.hasNext()) {
            Notice notice = iterator.next();
            renderNotice(context, text, notice, now, screenWidth, screenHeight, index, bottom);
            index++;
        }
    }

    private static void renderNotice(DrawContext context, TextRenderer text, Notice notice, long now, int screenWidth, int screenHeight, int index, boolean bottom) {
        long age = now - notice.startedAt;
        double progress = animationProgress(age);
        if (progress <= 0.0) return;

        boolean compact = style == NotificationStyle.Compact;
        int height = compact ? 30 : HEIGHT;
        int iconSpace = !notice.icon.isEmpty() ? 24 : 0;
        int textLimit = MAX_WIDTH - 30 - iconSpace;
        String titleText = fit(text, notice.title, textLimit);
        String bodyText = fit(text, notice.body, textLimit);
        int contentWidth = Math.max(text.getWidth(titleText), text.getWidth(bodyText));
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, contentWidth + 28 + iconSpace));
        int x = baseX(screenWidth, width);
        int y = baseY(screenHeight, height, index, bottom);
        int slide = (int) Math.round((1.0 - progress) * 24.0);

        if (position == NotificationPosition.TopRight || position == NotificationPosition.BottomRight) x += slide;
        else if (position == NotificationPosition.TopLeft || position == NotificationPosition.BottomLeft) x -= slide;
        else y -= slide;

        int shadowAlpha = (int) Math.round(90.0 * progress);
        int textAlpha = (int) Math.round(255.0 * progress);
        int mutedAlpha = (int) Math.round(205.0 * progress);
        int bg = scaleAlpha(backgroundColor, progress);
        int edge = scaleAlpha(notice.accent, progress);
        int soft = scaleAlpha(notice.accent, progress * 0.35);
        int textColor = (textAlpha << 24) | 0xEAF7F2;
        int mutedColor = (mutedAlpha << 24) | 0xB8C4C0;

        context.fill(x + 2, y + 2, x + width + 2, y + height + 2, shadowAlpha << 24);
        context.fill(x, y, x + width, y + height, bg);
        context.fill(x, y, x + 3, y + height, edge);
        if (style == NotificationStyle.Eclipse) {
            context.fillGradient(x + 3, y, x + width, y + 1, soft, 0x00000000);
            context.fillGradient(x + 3, y + height - 1, x + width, y + height, 0x00000000, soft);
        }

        int textX = x + 12;
        if (!notice.icon.isEmpty()) {
            context.drawItem(notice.icon, x + 10, y + (height - 16) / 2);
            textX += 24;
        }

        context.drawTextWithShadow(text, titleText, textX, y + (compact ? 4 : 6), textColor);
        context.drawTextWithShadow(text, bodyText, textX, y + (compact ? 17 : 20), mutedColor);
    }

    private static double animationProgress(long age) {
        double enterMs = 160.0 / animationSpeed;
        double exitMs = 220.0 / animationSpeed;
        double appear = Math.min(1.0, age / enterMs);
        double disappear = Math.min(1.0, (durationMs - age) / exitMs);
        double progress = Math.max(0.0, Math.min(1.0, Math.min(appear, disappear)));
        return progress * progress * (3.0 - 2.0 * progress);
    }

    private static int baseX(int screenWidth, int width) {
        return switch (position) {
            case TopLeft, BottomLeft -> MARGIN;
            case TopRight, BottomRight -> screenWidth - width - MARGIN;
            case TopCenter -> (screenWidth - width) / 2;
        };
    }

    private static int baseY(int screenHeight, int height, int index, boolean bottom) {
        int offset = index * (height + GAP);
        return bottom ? screenHeight - MARGIN - height - offset : MARGIN + offset;
    }

    private static void removeExpired(long now) {
        while (!NOTICES.isEmpty()) {
            Notice notice = NOTICES.peekFirst();
            if (notice == null || now - notice.startedAt < durationMs) return;
            NOTICES.removeFirst();
        }
    }

    private static void trimToLimit() {
        while (NOTICES.size() > maxNotifications) {
            NOTICES.removeFirst();
        }
    }

    private static int forceAlpha(int color) {
        return ((color >>> 24) == 0 ? 0xFF000000 : 0) | color;
    }

    private static int scaleAlpha(int color, double progress) {
        int alpha = (int) Math.round(((color >>> 24) & 0xFF) * progress);
        return (alpha << 24) | (color & 0x00FFFFFF);
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

    public enum NotificationPosition {
        TopLeft,
        TopRight,
        TopCenter,
        BottomLeft,
        BottomRight
    }

    public enum NotificationStyle {
        Eclipse,
        Compact
    }

    private record Notice(String title, String body, ItemStack icon, int accent, long startedAt) {
    }

    private static final class EventListener {
        @EventHandler
        private void onRender2D(Render2DEvent event) {
            EclipseToastOverlay.render(event);
        }
    }
}
