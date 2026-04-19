package eclipse.diagnostics;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class DiagnosticStore {
    private final EnumMap<DiagnosticCategory, ArrayDeque<DiagnosticEvent>> events = new EnumMap<>(DiagnosticCategory.class);
    private final EnumMap<DiagnosticCategory, Integer> totals = new EnumMap<>(DiagnosticCategory.class);
    private final HashMap<String, Integer> packetRx = new HashMap<>();
    private final HashMap<String, Integer> packetTx = new HashMap<>();
    private final HashMap<String, Long> throttle = new HashMap<>();
    private int limit;
    private int throttledEvents;

    public DiagnosticStore(int limit) {
        this.limit = Math.max(32, limit);
        for (DiagnosticCategory category : DiagnosticCategory.values()) {
            events.put(category, new ArrayDeque<>());
            totals.put(category, 0);
        }
    }

    public void reset(int limit) {
        this.limit = Math.max(32, limit);
        for (ArrayDeque<DiagnosticEvent> buffer : events.values()) buffer.clear();
        for (DiagnosticCategory category : DiagnosticCategory.values()) totals.put(category, 0);
        packetRx.clear();
        packetTx.clear();
        throttle.clear();
        throttledEvents = 0;
    }

    public boolean add(DiagnosticEvent event, long throttleMs) {
        totals.merge(event.category(), 1, Integer::sum);
        String key = event.category() + ":" + event.type();
        if (throttleMs > 0) {
            Long last = throttle.get(key);
            if (last != null && event.elapsedMs() - last < throttleMs) {
                throttledEvents++;
                return false;
            }
            throttle.put(key, event.elapsedMs());
        }

        ArrayDeque<DiagnosticEvent> buffer = events.get(event.category());
        while (buffer.size() >= limit) buffer.removeFirst();
        buffer.addLast(event);
        return true;
    }

    public void countPacket(String direction, String packetName) {
        (direction.equals("rx") ? packetRx : packetTx).merge(packetName, 1, Integer::sum);
    }

    public Map<String, Integer> packetRx() {
        return packetRx;
    }

    public Map<String, Integer> packetTx() {
        return packetTx;
    }

    public Map<DiagnosticCategory, ArrayDeque<DiagnosticEvent>> events() {
        return events;
    }

    public int total(DiagnosticCategory category) {
        return totals.getOrDefault(category, 0);
    }

    public int throttledEvents() {
        return throttledEvents;
    }
}
