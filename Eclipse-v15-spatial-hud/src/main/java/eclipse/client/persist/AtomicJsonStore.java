package eclipse.client.persist;

import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicJsonStore<T> {
    private final Gson gson;
    private final Path file;
    private final Type type;

    public AtomicJsonStore(Gson gson, Path file, Type type) {
        this.gson = gson;
        this.file = file;
        this.type = type;
    }

    public void save(T value) throws IOException {
        Path dir = file.getParent();
        if (dir != null) Files.createDirectories(dir);

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            gson.toJson(value, type, writer);
        }

        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public T loadOrDefault(T defaultValue) throws IOException {
        if (!Files.exists(file)) return defaultValue;
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        T parsed = gson.fromJson(raw, type);
        return parsed == null ? defaultValue : parsed;
    }
}
