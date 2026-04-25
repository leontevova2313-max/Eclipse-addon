package eclipse.client.persist;

import eclipse.client.spatial.RouteRecord;
import eclipse.client.spatial.WaypointRecord;
import eclipse.client.theme.ClientThemeId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientState {
    public ClientThemeId theme = ClientThemeId.DARK_MONO;
    public Map<String, Boolean> moduleStates = new LinkedHashMap<>();
    public Map<String, HudWidgetState> hudLayout = new LinkedHashMap<>();
    public List<WaypointRecord> waypoints = new ArrayList<>();
    public List<RouteRecord> routes = new ArrayList<>();

    public static final class HudWidgetState {
        public int x;
        public int y;
        public int width = 120;
        public int height = 24;
        public boolean visible = true;
    }
}
