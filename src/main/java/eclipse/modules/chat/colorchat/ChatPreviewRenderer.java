package eclipse.modules.chat.colorchat;

import eclipse.modules.chat.ColorChat;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public final class ChatPreviewRenderer {
    private static final ChatColorFormatter FORMATTER = new ChatColorFormatter();

    private ChatPreviewRenderer() {
    }

    public static void render(DrawContext context, TextRenderer textRenderer, String input, int screenWidth, int screenHeight) {
        ColorChat module = Modules.get().get(ColorChat.class);
        if (module == null || !module.previewEnabled()) return;
        if (input == null || input.isBlank()) return;

        Text preview = FORMATTER.preview(input, module, true);
        int maxWidth = Math.max(80, screenWidth - 8);
        List<OrderedText> lines = textRenderer.wrapLines(preview, maxWidth);
        if (lines.isEmpty()) return;

        int lineCount = Math.min(2, lines.size());
        int lineHeight = textRenderer.fontHeight + 2;
        int boxHeight = lineCount * lineHeight + 5;
        int y = Math.max(4, screenHeight - 34 - boxHeight);

        context.fill(2, y - 2, screenWidth - 2, y + boxHeight, 0x99000000);
        context.drawTextWithShadow(textRenderer, Text.literal("Preview:"), 5, y, 0xFFAAAAAA);

        int textY = y + lineHeight;
        for (int i = 0; i < lineCount; i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), 5, textY + i * lineHeight, 0xFFFFFFFF);
        }
    }
}

