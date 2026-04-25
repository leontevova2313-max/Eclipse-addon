package eclipse.client.perf;

public final class PerfInspectorService {
    private final SampledMetric frameNs = new SampledMetric(180);
    private final SampledMetric tickNs = new SampledMetric(180);
    private final SampledMetric hudNs = new SampledMetric(180);
    private final SampledMetric uiNs = new SampledMetric(180);
    private final PerfSnapshot snapshot = new PerfSnapshot();
    private long lastPublishAtNs;

    public void sampleFrame(long frameNsValue) {
        frameNs.add(frameNsValue);
        publishMaybe();
    }

    public void sampleTick(long tickNsValue, int enabledModules, int visibleHudWidgets, int waypointCount, int routeCount) {
        tickNs.add(tickNsValue);
        snapshot.enabledModules = enabledModules;
        snapshot.visibleHudWidgets = visibleHudWidgets;
        snapshot.waypointCount = waypointCount;
        snapshot.routeCount = routeCount;
        publishMaybe();
    }

    public void sampleHud(long hudNsValue) {
        hudNs.add(hudNsValue);
        publishMaybe();
    }

    public void sampleUi(long uiNsValue) {
        uiNs.add(uiNsValue);
        publishMaybe();
    }

    private void publishMaybe() {
        long now = System.nanoTime();
        if (now - lastPublishAtNs < 250_000_000L) return;
        snapshot.frameAvgNs = frameNs.average();
        snapshot.frameMaxNs = frameNs.max();
        snapshot.tickAvgNs = tickNs.average();
        snapshot.hudAvgNs = hudNs.average();
        snapshot.uiAvgNs = uiNs.average();
        Runtime rt = Runtime.getRuntime();
        snapshot.usedMemoryBytes = rt.totalMemory() - rt.freeMemory();
        snapshot.maxMemoryBytes = rt.maxMemory();
        lastPublishAtNs = now;
    }

    public PerfSnapshot snapshot() {
        return snapshot;
    }
}
