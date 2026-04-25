package eclipse.client.hud;

public final class HudWidgetBinding {
    private final String widgetId;
    private final String moduleId;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible;
    private final HudAnchor anchor;

    public HudWidgetBinding(String widgetId, String moduleId, int x, int y, int width, int height, boolean visible, HudAnchor anchor) {
        this.widgetId = widgetId;
        this.moduleId = moduleId;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = visible;
        this.anchor = anchor;
    }

    public String widgetId() { return widgetId; }
    public String moduleId() { return moduleId; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public boolean visible() { return visible; }
    public HudAnchor anchor() { return anchor; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
