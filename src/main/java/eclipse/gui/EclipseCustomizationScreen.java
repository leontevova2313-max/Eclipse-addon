package eclipse.gui;

import eclipse.skins.LocalSkinManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class EclipseCustomizationScreen extends Screen {
    private final Screen parent;

    public EclipseCustomizationScreen(Screen parent) {
        super(Text.translatable("eclipse.customization.title"));
        this.parent = parent;
        LocalSkinManager.load();
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = height / 2 - 42;

        addDrawableChild(ButtonWidget.builder(skinText(), button -> {
            LocalSkinManager.skinSlot(next(LocalSkinManager.skinSlot(), "Default", "Local 1", "Local 2", "Local 3"));
            button.setMessage(skinText());
        }).dimensions(center - 100, y, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(capeText(), button -> {
            LocalSkinManager.capeSlot(next(LocalSkinManager.capeSlot(), "None", "Local Cape 1", "Local Cape 2"));
            button.setMessage(capeText());
        }).dimensions(center - 100, y + 24, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(modelText(), button -> {
            LocalSkinManager.slimModel(!LocalSkinManager.slimModel());
            button.setMessage(modelText());
        }).dimensions(center - 100, y + 48, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.customization.open_folder"), button -> {
            if (client != null) client.keyboard.setClipboard(LocalSkinManager.directory().toString());
        }).dimensions(center - 100, y + 72, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.customization.done"), button -> close())
            .dimensions(center - 100, height - 36, 200, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xEE05070D);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 24, 0xFFFFFFFF);

        int previewX = width / 2 - 32;
        int previewY = 58;
        context.drawTexture(
            net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
            LocalSkinManager.previewSkin(),
            previewX, previewY,
            0.0F, 0.0F,
            64, 64,
            64, 64,
            64, 64
        );

        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("eclipse.customization.preview_hint"), width / 2, previewY + 72, 0xFFB8C0CC);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private String next(String current, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) return values[(i + 1) % values.length];
        }

        return values[0];
    }

    private Text skinText() {
        return Text.translatable("eclipse.customization.skin", slotText(LocalSkinManager.skinSlot()));
    }

    private Text capeText() {
        return Text.translatable("eclipse.customization.cape", capeSlotText(LocalSkinManager.capeSlot()));
    }

    private Text modelText() {
        return Text.translatable(
            "eclipse.customization.model",
            Text.translatable(LocalSkinManager.slimModel()
                ? "eclipse.customization.model.slim"
                : "eclipse.customization.model.classic")
        );
    }

    private Text slotText(String slot) {
        return switch (slot) {
            case "Local 1" -> Text.translatable("eclipse.skin.local_1");
            case "Local 2" -> Text.translatable("eclipse.skin.local_2");
            case "Local 3" -> Text.translatable("eclipse.skin.local_3");
            default -> Text.translatable("eclipse.skin.default");
        };
    }

    private Text capeSlotText(String slot) {
        return switch (slot) {
            case "Local Cape 1" -> Text.translatable("eclipse.cape.local_1");
            case "Local Cape 2" -> Text.translatable("eclipse.cape.local_2");
            default -> Text.translatable("eclipse.cape.none");
        };
    }
}
