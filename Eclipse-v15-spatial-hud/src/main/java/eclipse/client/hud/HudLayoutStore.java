package eclipse.client.hud;

import com.google.gson.Gson;
import eclipse.client.persist.AtomicJsonStore;
import eclipse.client.persist.ClientState;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

public final class HudLayoutStore {
    private final AtomicJsonStore<ClientState> backing;

    public HudLayoutStore(Gson gson, Path path, Type type) {
        this.backing = new AtomicJsonStore<>(gson, path, type);
    }

    public void save(ClientState state) throws IOException { backing.save(state); }
    public ClientState loadOrDefault(ClientState state) throws IOException { return backing.loadOrDefault(state); }
}
