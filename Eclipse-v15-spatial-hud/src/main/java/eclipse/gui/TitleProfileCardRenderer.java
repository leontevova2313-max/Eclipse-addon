package eclipse.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;

public final class TitleProfileCardRenderer {
    private TitleProfileCardRenderer() {}

    public static void render(DrawContext context, SkinTextures textures, String username, int x, int y, int w, int h) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int shadow = 0x22000000;
        int panel = 0xC815151A;
        int border = 0x55FFFFFF;
        int topLine = 0x33FFFFFF;
        int accent = 0xAAE8E8E8;
        int muted = 0xFFB8BBC4;
        int text = 0xFFF2F3F7;

        // Outer shadow and panel
        context.fill(x + 3, y + 4, x + w + 3, y + h + 4, shadow);
        context.fill(x, y, x + w, y + h, panel);
        context.fill(x, y, x + w, y + 1, topLine);
        context.fill(x, y, x + 1, y + h, topLine);
        context.fill(x + w - 1, y, x + w, y + h, border);
        context.fill(x, y + h - 1, x + w, y + h, border);

        int headerH = 18;
        context.fill(x + 1, y + 1, x + w - 1, y + headerH, 0x1AFFFFFF);
        context.fill(x + 12, y + headerH + 6, x + w - 12, y + headerH + 7, 0x22FFFFFF);

        int headSize = 32;
        int headX = x + 12;
        int headY = y + headerH + 12;
        context.fill(headX - 3, headY - 3, headX + headSize + 3, headY + headSize + 3, 0x22000000);
        context.fill(headX - 1, headY - 1, headX + headSize + 1, headY + headSize + 1, 0xFF101217);
        PlayerSkinDrawer.draw(context, textures, headX, headY, headSize);

        int textX = headX + headSize + 12;
        int userY = headY + 2;
        context.drawTextWithShadow(tr, Text.literal(username), textX, userY, text);
        context.drawText(tr, Text.literal("Eclipse session"), textX, userY + 13, muted, false);

        int chipY = y + h - 24;
        int chipX = x + 12;
        int chipW = Math.min(w - 24, Math.max(72, tr.getWidth("CLIENT READY") + 20));
        context.fill(chipX + 2, chipY + 2, chipX + chipW + 2, chipY + 18 + 2, 0x16000000);
        context.fill(chipX, chipY, chipX + chipW, chipY + 18, 0xFF0F1116);
        context.fill(chipX, chipY, chipX + chipW, chipY + 1, 0x2CFFFFFF);
        context.drawCenteredTextWithShadow(tr, "CLIENT READY", chipX + chipW / 2, chipY + 5, accent);
    }
}
