package eclipse.client.perf;

public final class PerfSnapshot {
    public long frameAvgNs;
    public long frameMaxNs;
    public long tickAvgNs;
    public long hudAvgNs;
    public long uiAvgNs;
    public int enabledModules;
    public int visibleHudWidgets;
    public int waypointCount;
    public int routeCount;
    public long usedMemoryBytes;
    public long maxMemoryBytes;

    public double frameMs() {
        return frameAvgNs / 1_000_000.0;
    }

    public double tickMs() {
        return tickAvgNs / 1_000_000.0;
    }

    public double hudMs() {
        return hudAvgNs / 1_000_000.0;
    }

    public double uiMs() {
        return uiAvgNs / 1_000_000.0;
    }

    public int estimatedFps() {
        if (frameAvgNs <= 0L) return 0;
        return (int) Math.max(1L, 1_000_000_000L / frameAvgNs);
    }

    public int memoryPercent() {
        if (maxMemoryBytes <= 0L) return 0;
        return (int) ((usedMemoryBytes * 100L) / maxMemoryBytes);
    }
}
