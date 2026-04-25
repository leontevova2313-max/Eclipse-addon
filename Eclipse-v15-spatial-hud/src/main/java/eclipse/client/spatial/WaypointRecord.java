package eclipse.client.spatial;

/**
 * Lightweight waypoint model persisted by the standalone client layer.
 */
public final class WaypointRecord {
    public String id;
    public String name;
    public String dimension;
    public int x;
    public int y;
    public int z;
    public int color;
    public boolean visible = true;

    public WaypointRecord() {
    }

    public WaypointRecord(String id, String name, String dimension, int x, int y, int z, int color) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }
}
