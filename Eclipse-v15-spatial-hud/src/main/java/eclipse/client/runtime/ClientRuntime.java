package eclipse.client.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eclipse.client.hud.HudAnchor;
import eclipse.client.hud.HudLayoutStore;
import eclipse.client.hud.HudRuntime;
import eclipse.client.hud.HudWidgetBinding;
import eclipse.client.perf.PerfInspectorService;
import eclipse.client.persist.ClientState;
import eclipse.client.spatial.RouteRecord;
import eclipse.client.spatial.SpatialRuntime;
import eclipse.client.spatial.WaypointRecord;
import eclipse.client.theme.ThemeManager;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public final class ClientRuntime {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ThemeManager THEME = new ThemeManager();
    private static final PerfInspectorService PERF = new PerfInspectorService();
    private static final HudRuntime HUD = new HudRuntime();
    private static final SpatialRuntime SPATIAL = new SpatialRuntime();
    private static final ClientModuleCatalog MODULES = new ClientModuleCatalog();
    private static Supplier<List<Module>> moduleSupplier = List::of;
    private static ClientState state = new ClientState();
    private static boolean bootstrapped;

    private ClientRuntime() {
    }

    public static void bootstrap() {
        if (bootstrapped) return;

        HUD.register(new HudWidgetBinding("coordinates-hud", null, 20, 14, 160, 18, true, HudAnchor.TOP_LEFT));
        HUD.register(new HudWidgetBinding("clock-hud", null, 20, 38, 92, 18, true, HudAnchor.TOP_LEFT));
        HUD.register(new HudWidgetBinding("nearest-waypoint", null, 20, 62, 156, 18, true, HudAnchor.TOP_LEFT));
        HUD.register(new HudWidgetBinding("active-modules", null, 20, 86, 138, 80, true, HudAnchor.TOP_LEFT));
        HUD.register(new HudWidgetBinding("debug-overlay", null, 20, 172, 154, 48, false, HudAnchor.TOP_LEFT));

        load();
        bootstrapped = true;
    }

    public static void setModuleSupplier(Supplier<List<Module>> supplier) {
        moduleSupplier = supplier == null ? List::of : supplier;
        MODULES.setSource(moduleSupplier);
    }

    public static ThemeManager theme() {
        return THEME;
    }

    public static PerfInspectorService perf() {
        return PERF;
    }

    public static HudRuntime hud() {
        return HUD;
    }

    public static SpatialRuntime spatial() {
        return SPATIAL;
    }

    public static ClientModuleCatalog modules() {
        return MODULES;
    }

    public static void sampleFrame(long ns) {
        PERF.sampleFrame(ns);
    }

    public static void sampleTick(long ns, int enabledModules) {
        PERF.sampleTick(ns, enabledModules, HUD.visibleCount(), SPATIAL.visibleWaypointCount(), SPATIAL.routes().size());
    }

    public static void sampleHud(long ns) {
        PERF.sampleHud(ns);
    }

    public static void sampleUi(long ns) {
        PERF.sampleUi(ns);
    }

    public static void save(List<Module> modules) {
        state.theme = THEME.current();
        state.moduleStates.clear();
        for (Module module : modules) state.moduleStates.put(module.name, module.isActive());

        state.hudLayout.clear();
        for (HudWidgetBinding binding : HUD.widgets()) {
            ClientState.HudWidgetState widgetState = new ClientState.HudWidgetState();
            widgetState.x = binding.x();
            widgetState.y = binding.y();
            widgetState.width = binding.width();
            widgetState.height = binding.height();
            widgetState.visible = binding.visible();
            state.hudLayout.put(binding.widgetId(), widgetState);
        }

        state.waypoints.clear();
        for (WaypointRecord waypoint : SPATIAL.waypoints()) {
            WaypointRecord copy = new WaypointRecord(waypoint.id, waypoint.name, waypoint.dimension, waypoint.x, waypoint.y, waypoint.z, waypoint.color);
            copy.visible = waypoint.visible;
            state.waypoints.add(copy);
        }
        state.routes.clear();
        for (RouteRecord route : SPATIAL.routes()) {
            RouteRecord copy = new RouteRecord(route.id, route.name);
            copy.visible = route.visible;
            copy.waypointIds.addAll(route.waypointIds);
            state.routes.add(copy);
        }

        try {
            store().save(state);
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        save(moduleSupplier.get());
    }

    public static void applyModuleStates(Supplier<List<Module>> supplier) {
        setModuleSupplier(supplier);
        for (Module module : supplier.get()) {
            Boolean enabled = state.moduleStates.get(module.name);
            if (enabled == null) continue;
            if (enabled && !module.isActive()) module.toggle();
            if (!enabled && module.isActive()) module.toggle();
        }
    }

    private static void load() {
        try {
            state = store().loadOrDefault(new ClientState());
            if (state.theme != null) THEME.set(state.theme);
            state.hudLayout.forEach((id, widgetState) -> {
                HudWidgetBinding binding = HUD.find(id);
                if (binding != null) {
                    binding.setPosition(widgetState.x, widgetState.y);
                    binding.setVisible(widgetState.visible);
                }
            });
            SPATIAL.replace(state.waypoints, state.routes);
        } catch (IOException ignored) {
            state = new ClientState();
        }
    }

    private static HudLayoutStore store() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/eclipse-client.json");
        return new HudLayoutStore(GSON, path, ClientState.class);
    }
}
