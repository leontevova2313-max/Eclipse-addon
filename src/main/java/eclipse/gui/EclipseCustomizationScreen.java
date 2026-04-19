package eclipse.gui;

import com.eclipse.mixin.PlayerSkinWidgetAccessor;
import eclipse.skins.SkinCustomizationManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

public class EclipseCustomizationScreen extends Screen {
    private static final int BUTTON_WIDTH = 190;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 8;
    private static final int PREVIEW_MIN_WIDTH = 138;
    private static final int PREVIEW_MAX_WIDTH = 190;
    private static final int PREVIEW_MIN_HEIGHT = 168;
    private static final int PREVIEW_MAX_HEIGHT = 230;

    private final Screen parent;
    private PlayerSkinWidget skinWidget;
    private int modelX;
    private int modelY;
    private int modelW;
    private int modelH;
    private ButtonWidget skinModelButton;
    private ButtonWidget applySkinButton;
    private float trackedXRotation = -5.0F;
    private float trackedYRotation = 30.0F;

    public EclipseCustomizationScreen(Screen parent) {
        super(Text.translatable("eclipse.customization.title"));
        this.parent = parent;
        SkinCustomizationManager.load();
    }

    @Override
    protected void init() {
        int contentTop = 34;
        int contentBottom = Math.max(contentTop + PREVIEW_MIN_HEIGHT, height - 46);
        int contentHeight = contentBottom - contentTop;
        int center = width / 2;

        modelH = MathHelper.clamp(contentHeight - 24, PREVIEW_MIN_HEIGHT, PREVIEW_MAX_HEIGHT);
        modelW = MathHelper.clamp(modelH * 3 / 4, PREVIEW_MIN_WIDTH, PREVIEW_MAX_WIDTH);

        boolean twoColumns = width >= 520;
        int controlsX = twoColumns ? center + 28 : center - BUTTON_WIDTH / 2;
        int controlsY = twoColumns ? contentTop + 30 : contentTop + modelH + 28;

        modelX = twoColumns ? center - 176 : center - modelW / 2;
        modelY = contentTop + Math.max(0, (twoColumns ? contentHeight - modelH : 0) / 2);

        if (client.player == null) {
            skinWidget = new PlayerSkinWidget(modelW, modelH, client.getLoadedEntityModels(), SkinCustomizationManager::currentSkinTextures);
            skinWidget.setX(modelX);
            skinWidget.setY(modelY);
            addDrawableChild(skinWidget);
        }

        skinModelButton = addDrawableChild(ButtonWidget.builder(skinModelText(), button -> {
            SkinCustomizationManager.skinModel(SkinCustomizationManager.skinModel().next());
            button.setMessage(skinModelText());
        }).dimensions(controlsX, controlsY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.customization.load_skin_file"), button ->
            chooseFile(file -> SkinCustomizationManager.loadSkinFile(file.toPath()))
        ).dimensions(controlsX, controlsY + row(1), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        applySkinButton = addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.customization.apply_skin"), button ->
            SkinCustomizationManager.applyLoadedOfficialSkin()
        ).dimensions(controlsX, controlsY + row(2), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.customization.refresh_skin"), button ->
            SkinCustomizationManager.refreshOfficialPreview()
        ).dimensions(controlsX, controlsY + row(3), BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("eclipse.customization.done"), button -> close())
            .dimensions(center - 100, height - 32, 200, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xEE05070D);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);

        int previewCenterX = modelX + modelW / 2;
        int previewCenterY = modelY + modelH / 2;
        if (client != null && client.player != null) {
            float targetMouseX = previewCenterX + MathHelper.clamp(mouseX - previewCenterX, -modelW * 0.55F, modelW * 0.55F);
            float targetMouseY = previewCenterY + MathHelper.clamp(mouseY - previewCenterY, -modelH * 0.42F, modelH * 0.42F);
            InventoryScreen.drawEntity(context, modelX, modelY, modelX + modelW, modelY + modelH, modelH / 2, 0.0625F, targetMouseX, targetMouseY, client.player);
        } else if (skinWidget != null) {
            updateSkinWidgetTracking(mouseX, mouseY, delta);
        }

        if (skinModelButton != null) skinModelButton.setMessage(skinModelText());
        if (applySkinButton != null) applySkinButton.active = SkinCustomizationManager.hasLoadedSkinFile();

        int infoX = width >= 520 ? width / 2 + 28 : width / 2;
        int infoY = width >= 520 ? 178 : Math.min(height - 70, modelY + modelH + 150);
        drawInfo(context, infoX, infoY, width >= 520);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private void chooseFile(FileConsumer consumer) {
        new Thread(() -> {
            FileDialog dialog = new FileDialog((Frame) null, "Choose PNG", FileDialog.LOAD);
            dialog.setFile("*.png");
            dialog.setVisible(true);
            if (dialog.getFile() == null) return;
            consumer.accept(new File(dialog.getDirectory(), dialog.getFile()));
        }, "Eclipse File Picker").start();
    }

    private Text skinModelText() {
        return Text.translatable("eclipse.customization.skin_model", Text.translatable(switch (SkinCustomizationManager.skinModel()) {
            case Auto -> "eclipse.skin_model.auto";
            case Classic -> "eclipse.skin_model.classic";
            case Slim -> "eclipse.skin_model.slim";
        }));
    }

    private void updateSkinWidgetTracking(int mouseX, int mouseY, float delta) {
        float centerX = modelX + modelW / 2.0F;
        float centerY = modelY + modelH / 2.0F;
        float dx = MathHelper.clamp((mouseX - centerX) / Math.max(1.0F, modelW / 2.0F), -1.0F, 1.0F);
        float dy = MathHelper.clamp((mouseY - centerY) / Math.max(1.0F, modelH / 2.0F), -1.0F, 1.0F);
        float targetY = 30.0F + dx * 30.0F;
        float targetX = -5.0F - dy * 22.0F;
        float smoothing = MathHelper.clamp(delta * 0.25F, 0.08F, 0.32F);
        trackedYRotation += (targetY - trackedYRotation) * smoothing;
        trackedXRotation += (targetX - trackedXRotation) * smoothing;

        PlayerSkinWidgetAccessor accessor = (PlayerSkinWidgetAccessor) skinWidget;
        accessor.eclipse$setYRotation(trackedYRotation);
        accessor.eclipse$setXRotation(trackedXRotation);
    }

    private void drawInfo(DrawContext context, int x, int y, boolean leftAligned) {
        Text[] lines = new Text[] {
            Text.translatable("eclipse.customization.preview_hint"),
            Text.translatable("eclipse.customization.loaded_file", SkinCustomizationManager.loadedSkinFileName()),
            Text.literal(SkinCustomizationManager.status())
        };

        for (int i = 0; i < lines.length; i++) {
            int lineY = y + i * 12;
            if (leftAligned) {
                context.drawTextWithShadow(textRenderer, lines[i], x, lineY, 0xFFB8C0CC);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, lines[i], x, lineY, 0xFFB8C0CC);
            }
        }
    }

    private int row(int index) {
        return index * (BUTTON_HEIGHT + ROW_GAP);
    }

    @FunctionalInterface
    private interface FileConsumer {
        void accept(File file);
    }
}
