package eclipse.client.spatial;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered route composed of waypoint ids.
 */
public final class RouteRecord {
    public String id;
    public String name;
    public final List<String> waypointIds = new ArrayList<>();
    public boolean visible = true;

    public RouteRecord() {
    }

    public RouteRecord(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
