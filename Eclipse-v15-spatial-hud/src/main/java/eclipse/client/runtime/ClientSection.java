package eclipse.client.runtime;

/**
 * Top-level sections used by the Eclipse client workspace. These sections are part of the
 * standalone client layer and intentionally avoid exposing Meteor's category model directly.
 */
public enum ClientSection {
    OVERVIEW("Overview"),
    VISUALS("Visuals"),
    MOVEMENT("Movement"),
    COMBAT("Combat"),
    NETWORK("Network"),
    UTILITY("Utility"),
    CHAT("Chat");

    private final String title;

    ClientSection(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return switch (this) {
            case OVERVIEW -> "High-level status, activity, and quick client context.";
            case VISUALS -> "Rendering, HUD, and visual polish controls.";
            case MOVEMENT -> "Traversal tools, speed tuning, and motion behavior.";
            case COMBAT -> "Targeting, damage flow, and combat automation controls.";
            case NETWORK -> "Packet-facing features, routing, and sync behavior.";
            case UTILITY -> "General workflow tools, automation, and support modules.";
            case CHAT -> "Messaging, filters, and communication helpers.";
        };
    }
}
