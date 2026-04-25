package eclipse.client.perf;

public final class SampledMetric {
    private final long[] values;
    private int index;
    private int count;

    public SampledMetric(int capacity) {
        this.values = new long[Math.max(4, capacity)];
    }

    public void add(long value) {
        values[index] = value;
        index = (index + 1) % values.length;
        if (count < values.length) count++;
    }

    public long average() {
        if (count == 0) return 0L;
        long total = 0L;
        for (int i = 0; i < count; i++) total += values[i];
        return total / count;
    }

    public long max() {
        long max = 0L;
        for (int i = 0; i < count; i++) if (values[i] > max) max = values[i];
        return max;
    }
}
