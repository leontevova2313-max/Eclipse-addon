package eclipse.client.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryPresetStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, InventoryPreset>>() {}.getType();

    private InventoryPresetStore() {
    }

    public static Map<String, InventoryPreset> load() {
        Path file = path();
        try {
            if (Files.exists(file)) {
                String raw = Files.readString(file, StandardCharsets.UTF_8);
                Map<String, InventoryPreset> presets = GSON.fromJson(raw, TYPE);
                if (presets != null) return new LinkedHashMap<>(presets);
            }
        } catch (Exception ignored) {
            try {
                Path backup = backupPath(file);
                if (Files.exists(backup)) {
                    String raw = Files.readString(backup, StandardCharsets.UTF_8);
                    Map<String, InventoryPreset> presets = GSON.fromJson(raw, TYPE);
                    if (presets != null) return new LinkedHashMap<>(presets);
                }
            } catch (Exception ignoredAgain) {
            }
        }
        return new LinkedHashMap<>();
    }

    public static void save(Map<String, InventoryPreset> presets) throws IOException {
        writeAtomic(path(), presets, TYPE);
    }

    public static Path path() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/eclipse-inventory-presets.json");
    }

    private static void writeAtomic(Path file, Object value, Type type) throws IOException {
        Path dir = file.getParent();
        if (dir != null) Files.createDirectories(dir);

        if (Files.exists(file)) Files.copy(file, backupPath(file), StandardCopyOption.REPLACE_EXISTING);

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(value, type, writer);
        }

        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupPath(Path file) {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    public static final class InventoryPreset {
        public String name = "default";
        public Map<Integer, PresetSlot> slots = new LinkedHashMap<>();
    }

    public static final class PresetSlot {
        public String itemId = "minecraft:air";
        public int count;
    }
}
