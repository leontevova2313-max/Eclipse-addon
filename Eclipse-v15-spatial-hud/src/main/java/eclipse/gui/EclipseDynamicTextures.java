package eclipse.gui;

import eclipse.EclipseConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EclipseDynamicTextures {
    private static final Identifier TITLE_BACKGROUND = Identifier.of("eclipse", "dynamic/title_background");

    private EclipseDynamicTextures() {
    }

    public static boolean reloadTitleBackground(String pathValue) {
        EclipseConfig.clearCustomTitleBackgroundTexture();
        if (pathValue == null || pathValue.isBlank()) return false;

        Path path = Path.of(pathValue.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) return false;

        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            int width = image.getWidth();
            int height = image.getHeight();

            MinecraftClient client = MinecraftClient.getInstance();
            client.getTextureManager().destroyTexture(TITLE_BACKGROUND);
            client.getTextureManager().registerTexture(
                TITLE_BACKGROUND,
                new NativeImageBackedTexture(() -> "Eclipse custom title background", image)
            );
            EclipseConfig.customTitleBackgroundTexture(TITLE_BACKGROUND, width, height);
            return true;
        } catch (IOException | RuntimeException ignored) {
            EclipseConfig.clearCustomTitleBackgroundTexture();
            return false;
        }
    }
}
