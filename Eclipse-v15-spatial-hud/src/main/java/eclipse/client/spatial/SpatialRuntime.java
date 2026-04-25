package eclipse.client.spatial;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Standalone spatial workspace storing waypoints and lightweight routes. This is intentionally
 * independent from Meteor's waypoint systems so the Eclipse client can evolve its own UX.
 */
public final class SpatialRuntime {
    private final List<WaypointRecord> waypoints = new ArrayList<>();
    private final List<RouteRecord> routes = new ArrayList<>();

    public List<WaypointRecord> waypoints() {
        return waypoints;
    }

    public List<RouteRecord> routes() {
        return routes;
    }

    public void replace(List<WaypointRecord> newWaypoints, List<RouteRecord> newRoutes) {
        waypoints.clear();
        routes.clear();
        if (newWaypoints != null) waypoints.addAll(newWaypoints);
        if (newRoutes != null) routes.addAll(newRoutes);
    }

    public WaypointRecord addCurrentPlayerWaypoint(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        if (player == null || client.world == null) return null;
        String waypointName = (name == null || name.isBlank()) ? defaultWaypointName() : name;
        RegistryKey<World> key = client.world.getRegistryKey();
        WaypointRecord waypoint = new WaypointRecord(
            UUID.randomUUID().toString(),
            waypointName,
            key.getValue().toString(),
            (int) Math.floor(player.getX()),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ()),
            0xFF79A8FF
        );
        waypoints.add(waypoint);
        return waypoint;
    }

    public void removeWaypoint(String id) {
        if (id == null) return;
        waypoints.removeIf(waypoint -> id.equals(waypoint.id));
        for (RouteRecord route : routes) {
            route.waypointIds.removeIf(id::equals);
        }
    }

    public RouteRecord createRoute(String name) {
        RouteRecord route = new RouteRecord(UUID.randomUUID().toString(), (name == null || name.isBlank()) ? defaultRouteName() : name);
        routes.add(route);
        return route;
    }

    public void removeRoute(String id) {
        if (id == null) return;
        routes.removeIf(route -> id.equals(route.id));
    }

    public void appendWaypointToRoute(String routeId, String waypointId) {
        if (routeId == null || waypointId == null) return;
        RouteRecord route = findRoute(routeId);
        if (route == null) return;
        if (!route.waypointIds.contains(waypointId)) route.waypointIds.add(waypointId);
    }

    public void removeWaypointFromRoute(String routeId, String waypointId) {
        RouteRecord route = findRoute(routeId);
        if (route == null || waypointId == null) return;
        route.waypointIds.removeIf(waypointId::equals);
    }

    public WaypointRecord findWaypoint(String id) {
        for (WaypointRecord waypoint : waypoints) if (id != null && id.equals(waypoint.id)) return waypoint;
        return null;
    }

    public RouteRecord findRoute(String id) {
        for (RouteRecord route : routes) if (id != null && id.equals(route.id)) return route;
        return null;
    }

    public WaypointRecord nearestVisibleWaypoint() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        if (client == null || client.world == null || player == null) return null;
        String dimension = client.world.getRegistryKey().getValue().toString();
        return waypoints.stream()
            .filter(waypoint -> waypoint.visible)
            .filter(waypoint -> dimension.equals(waypoint.dimension))
            .min(Comparator.comparingDouble(waypoint -> squaredDistance(player, waypoint)))
            .orElse(null);
    }

    public int visibleWaypointCount() {
        int count = 0;
        for (WaypointRecord waypoint : waypoints) if (waypoint.visible) count++;
        return count;
    }

    private double squaredDistance(ClientPlayerEntity player, WaypointRecord waypoint) {
        double dx = player.getX() - waypoint.x;
        double dy = player.getY() - waypoint.y;
        double dz = player.getZ() - waypoint.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private String defaultWaypointName() {
        return "Waypoint " + (waypoints.size() + 1);
    }

    private String defaultRouteName() {
        return "Route " + (routes.size() + 1);
    }
}
