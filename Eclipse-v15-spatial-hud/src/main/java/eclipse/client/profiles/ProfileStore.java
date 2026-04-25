package eclipse.client.profiles;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

public final class ProfileStore {
    private static final List<String> CONFIG_FILES = List.of(
        "meteor-client/modules.nbt",
        "meteor-client/config.nbt",
        "meteor-client/hud.nbt",
        "meteor-client/macros.nbt",
        "config/eclipse-client.json",
        "config/eclipse-inventory-presets.json"
    );

    private static boolean bootstrapped;

    private ProfileStore() {
    }

    public static void bootstrapAutosafe() {
        if (bootstrapped) return;
        bootstrapped = true;

        try {
            save("_autosafe");
        } catch (IOException ignored) {
        }
    }

    public static void save(String profileName) throws IOException {
        String safeName = safeProfileName(profileName);
        Path profileDir = profilesDir().resolve(safeName);
        Files.createDirectories(profileDir);

        for (String relative : CONFIG_FILES) {
            Path source = runDir().resolve(relative);
            if (!Files.exists(source) || Files.isDirectory(source)) continue;

            Path target = profileDir.resolve(relative);
            copyAtomic(source, target);
        }

        Files.writeString(profileDir.resolve("profile.meta"), "saved=" + Instant.now() + System.lineSeparator());
    }

    public static void load(String profileName) throws IOException {
        String safeName = safeProfileName(profileName);
        Path profileDir = profilesDir().resolve(safeName);
        if (!Files.isDirectory(profileDir)) throw new IOException("Profile does not exist: " + safeName);

        for (String relative : CONFIG_FILES) {
            Path source = profileDir.resolve(relative);
            if (!Files.exists(source) || Files.isDirectory(source)) continue;

            Path target = runDir().resolve(relative);
            copyAtomic(source, target);
        }
    }

    public static Path profilesDir() {
        return runDir().resolve("config/eclipse-profiles");
    }

    public static String safeProfileName(String name) {
        if (name == null) return "default";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "default";
        return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void copyAtomic(Path source, Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);

        if (Files.exists(target)) {
            Files.copy(target, target.resolveSibling(target.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);

        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path runDir() {
        return MinecraftClient.getInstance().runDirectory.toPath();
    }
}
