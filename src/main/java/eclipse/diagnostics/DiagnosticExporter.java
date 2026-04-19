package eclipse.diagnostics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class DiagnosticExporter {
    private DiagnosticExporter() {
    }

    public static void export(Path sessionDir, DiagnosticStore store, DiagnosticSessionStats stats, String reason, String activeModules) throws IOException {
        Files.createDirectories(sessionDir);
        writeSession(sessionDir.resolve("session.md"), stats);
        writeEvents(sessionDir.resolve("events.csv"), store);
        writeSummary(sessionDir.resolve("summary.md"), store, stats, reason, activeModules);
    }

    public static String compactSummary(DiagnosticStore store, DiagnosticSessionStats stats) {
        return "corrections=" + stats.corrections
            + ", velocity=" + stats.localVelocityPackets
            + ", interactions=" + stats.interactionPackets
            + ", anomalies=" + stats.movementAnomalies
            + ", throttled=" + store.throttledEvents();
    }

    private static void writeSession(Path file, DiagnosticSessionStats stats) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo info = mc.getCurrentServerEntry();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("# Eclipse Server Diagnostics\n\n");
            writer.write("- Started: " + LocalDateTime.now() + "\n");
            writer.write("- Target: " + (info != null ? info.address : mc.isInSingleplayer() ? "singleplayer" : "unknown") + "\n");
            writer.write("- Server type: " + (info != null ? info.getServerType() : "integrated") + "\n");
            writer.write("- Initial ping: " + (info != null ? info.ping : -1) + "\n");
            writer.write("- Ticks captured: " + stats.ticks + "\n");
            writer.write("- Files:\n");
            writer.write("  - events.csv: bounded notable events grouped by category\n");
            writer.write("  - summary.md: counters, packet totals, and interpretation\n");
        }
    }

    private static void writeEvents(Path file, DiagnosticStore store) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("elapsed_ms,category,type,detail,module_context\n");
            for (DiagnosticCategory category : DiagnosticCategory.values()) {
                for (DiagnosticEvent event : store.events().get(category)) {
                    writer.write(event.elapsedMs() + ","
                        + category + ","
                        + csv(event.type()) + ","
                        + csv(event.detail()) + ","
                        + csv(event.moduleContext()));
                    writer.newLine();
                }
            }
        }
    }

    private static void writeSummary(Path file, DiagnosticStore store, DiagnosticSessionStats stats, String reason, String activeModules) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("# Session Summary\n\n");
            writer.write("- End reason: " + reason + "\n");
            writer.write("- Duration: " + (System.currentTimeMillis() - stats.startedAt) + " ms\n");
            writer.write("- Ticks: " + stats.ticks + "\n");
            writer.write("- Corrections: " + stats.corrections + "\n");
            writer.write("- Local velocity packets: " + stats.localVelocityPackets + "\n");
            writer.write("- Movement anomalies: " + stats.movementAnomalies + "\n");
            writer.write("- Interactions: " + stats.interactionPackets + "\n");
            writer.write("- Block interactions: " + stats.blockInteractions + "\n");
            writer.write("- Disconnects: " + stats.disconnects + "\n");
            writer.write("- Horizontal collision samples: " + stats.horizontalCollisionSamples + "\n");
            writer.write("- Vertical collision samples: " + stats.verticalCollisionSamples + "\n");
            writer.write("- Max horizontal speed: " + fmt(stats.maxHorizontalSpeed) + "\n");
            writer.write("- Max abs Y velocity: " + fmt(stats.maxAbsYVelocity) + "\n");
            writer.write("- Throttled repetitive events: " + store.throttledEvents() + "\n");
            writer.write("- Active modules at close: " + activeModules + "\n");
            writer.write("- Average tab ping: " + avgTabPing() + "\n\n");

            writer.write("## Category Totals\n\n");
            for (DiagnosticCategory category : DiagnosticCategory.values()) {
                writer.write("- " + category + ": " + store.total(category) + "\n");
            }

            writer.write("\n## Packet Totals\n\n");
            writer.write("- RX: " + topPackets(store.packetRx()) + "\n");
            writer.write("- TX: " + topPackets(store.packetTx()) + "\n\n");

            writer.write("## Interpretation\n\n");
            if (stats.corrections == 0) {
                writer.write("- No server correction packets were captured.\n");
            } else {
                writer.write("- Server corrections were captured. Compare events.csv timestamps with module context snapshots.\n");
            }
            if (stats.movementAnomalies > 0) writer.write("- Movement anomalies were detected from local speed/collision samples.\n");
            if (stats.localVelocityPackets > 0) writer.write("- The server sent velocity updates to the local player.\n");
        }
    }

    private static int avgTabPing() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return -1;
        int count = 0;
        int total = 0;
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            total += entry.getLatency();
            count++;
        }
        return count == 0 ? -1 : total / count;
    }

    private static String topPackets(Map<String, Integer> map) {
        return map.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .limit(12)
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "", map.isEmpty() ? "none" : ""));
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
