package eclipse.skins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LocalSkinManager {
    private static final Identifier DEFAULT_SKIN = Identifier.of("eclipse", "textures/gui/skin_preview.png");

    private static String skinSlot = "Default";
    private static String capeSlot = "None";
    private static boolean slimModel;

    private LocalSkinManager() {
    }

    public static Identifier previewSkin() {
        return DEFAULT_SKIN;
    }

    public static String skinSlot() {
        return skinSlot;
    }

    public static void skinSlot(String value) {
        skinSlot = value;
        save();
    }

    public static String capeSlot() {
        return capeSlot;
    }

    public static void capeSlot(String value) {
        capeSlot = value;
        save();
    }

    public static boolean slimModel() {
        return slimModel;
    }

    public static void slimModel(boolean value) {
        slimModel = value;
        save();
    }

    public static Path directory() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.runDirectory.toPath().resolve("eclipse-skins");
    }

    public static void load() {
        Path config = directory().resolve("local.txt");
        if (!Files.exists(config)) return;

        try {
            for (String line : Files.readAllLines(config)) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                switch (parts[0]) {
                    case "skin" -> skinSlot = parts[1];
                    case "cape" -> capeSlot = parts[1];
                    case "slim" -> slimModel = Boolean.parseBoolean(parts[1]);
                    default -> {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        Path dir = directory();
        Path config = dir.resolve("local.txt");

        try {
            Files.createDirectories(dir);
            Files.writeString(config, "skin=" + skinSlot + "\ncape=" + capeSlot + "\nslim=" + slimModel + "\n");
        } catch (IOException ignored) {
        }
    }
}
