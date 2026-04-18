package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerDiagnostics extends Module {
    private static final DateTimeFormatter FOLDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> packetSummary = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-summary")
        .description("Counts sent and received packet types.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetCsv = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-csv")
        .description("Writes a compact packet timeline. Leave off for long sessions.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> packetCsvLimit = sgGeneral.add(new IntSetting.Builder()
        .name("packet-csv-limit")
        .description("Maximum packet rows written per session when packet CSV is enabled.")
        .defaultValue(20000)
        .range(1000, 500000)
        .sliderRange(1000, 100000)
        .build()
    );

    private final Setting<Boolean> positionLog = sgGeneral.add(new BoolSetting.Builder()
        .name("position-log")
        .description("Writes periodic player position samples.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> positionInterval = sgGeneral.add(new IntSetting.Builder()
        .name("position-interval")
        .description("Ticks between position samples.")
        .defaultValue(20)
        .range(1, 200)
        .sliderRange(5, 100)
        .build()
    );

    private final Setting<Integer> summaryInterval = sgGeneral.add(new IntSetting.Builder()
        .name("summary-interval")
        .description("Seconds between summary snapshots.")
        .defaultValue(15)
        .range(5, 300)
        .sliderRange(5, 120)
        .build()
    );

    private final Setting<Boolean> activeModules = sgGeneral.add(new BoolSetting.Builder()
        .name("active-modules")
        .description("Adds active Eclipse module snapshots to summaries and correction events.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> writeAnalysis = sgGeneral.add(new BoolSetting.Builder()
        .name("write-analysis")
        .description("Writes analysis.md with a compact interpretation of the session.")
        .defaultValue(true)
        .build()
    );

    private final Map<String, Integer> receivedPackets = new HashMap<>();
    private final Map<String, Integer> sentPackets = new HashMap<>();

    private BufferedWriter events;
    private BufferedWriter packets;
    private BufferedWriter positions;
    private BufferedWriter summary;
    private Path sessionDir;
    private long startedAt;
    private int ticks;
    private int corrections;
    private int velocityUpdates;
    private int horizontalCollisionSamples;
    private int verticalCollisionSamples;
    private int packetRows;
    private double maxHorizontalSpeed;
    private double maxAbsYVelocity;
    private Vec3d lastPosition;
    private long lastPositionMs;

    public ServerDiagnostics() {
        super(Eclipse.CATEGORY, "server-diagnostics", "Records server corrections, velocity, movement, packets, active modules, and generated analysis.");
    }

    @Override
    public void onActivate() {
        if (mc.world != null) openSession();
    }

    @Override
    public void onDeactivate() {
        closeSession("module disabled");
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        openSession();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        closeSession("left server");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isOpen()) return;
        String packetName = packetName(event.packet);

        if (packetSummary.get()) receivedPackets.merge(packetName, 1, Integer::sum);
        writePacket("rx", packetName);

        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            corrections++;
            log("correction teleportId=" + packet.teleportId()
                + " pos=" + vec(packet.change().position())
                + " velocity=" + vec(packet.change().deltaMovement())
                + " yaw=" + fmt(packet.change().yaw())
                + " pitch=" + fmt(packet.change().pitch())
                + " relatives=" + packet.relatives()
                + " local=" + playerSnapshot()
                + " active=" + activeModuleSnapshot());
        } else if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (mc.player != null && packet.getEntityId() == mc.player.getId()) {
                velocityUpdates++;
                log("player-velocity velocity=" + vec(packet.getVelocity())
                    + " local=" + playerSnapshot()
                    + " active=" + activeModuleSnapshot());
            }
        } else if (event.packet instanceof DisconnectS2CPacket packet) {
            log("disconnect reason=" + packet.reason().getString()
                + " local=" + playerSnapshot()
                + " active=" + activeModuleSnapshot());
        } else if (event.packet instanceof LoginDisconnectS2CPacket packet) {
            log("login-disconnect reason=" + packet.reason().getString());
        } else if (event.packet instanceof ServerMetadataS2CPacket packet) {
            log("server-metadata description=" + packet.description().getString()
                + " favicon=" + packet.favicon().isPresent());
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isOpen()) return;
        String packetName = packetName(event.packet);

        if (packetSummary.get()) sentPackets.merge(packetName, 1, Integer::sum);
        writePacket("tx", packetName);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isOpen() || mc.player == null) return;

        ticks++;
        updateMovementStats();

        if (positionLog.get() && ticks % positionInterval.get() == 0) {
            writeLine(positions, elapsedMs() + ","
                + fmt(mc.player.getX()) + ","
                + fmt(mc.player.getY()) + ","
                + fmt(mc.player.getZ()) + ","
                + fmt(mc.player.getYaw()) + ","
                + fmt(mc.player.getPitch()) + ","
                + mc.player.isOnGround() + ","
                + mc.player.horizontalCollision + ","
                + mc.player.verticalCollision + ","
                + fmt(horizontalSpeed()) + ","
                + fmt(mc.player.getVelocity().y) + ","
                + csv(activeModuleSnapshot()));
        }

        if (ticks % (summaryInterval.get() * 20) == 0) writeSummary();
    }

    private void openSession() {
        closeSession("new session");

        try {
            startedAt = System.currentTimeMillis();
            ticks = 0;
            corrections = 0;
            velocityUpdates = 0;
            horizontalCollisionSamples = 0;
            verticalCollisionSamples = 0;
            packetRows = 0;
            maxHorizontalSpeed = 0.0;
            maxAbsYVelocity = 0.0;
            lastPosition = null;
            lastPositionMs = 0;
            receivedPackets.clear();
            sentPackets.clear();

            Path serverDir = mc.runDirectory.toPath()
                .resolve("eclipse-diagnostics")
                .resolve(serverFolderName());
            sessionDir = serverDir.resolve(LocalDateTime.now().format(FOLDER_TIME));
            Files.createDirectories(sessionDir);

            events = writer("events.log");
            packets = writer("packets.csv");
            positions = writer("positions.csv");
            summary = writer("summary.log");

            writeLine(packets, "elapsed_ms,direction,packet");
            writeLine(positions, "elapsed_ms,x,y,z,yaw,pitch,on_ground,horizontal_collision,vertical_collision,horizontal_speed,y_velocity,active_modules");
            writeSessionInfo();
            log("session-start path=" + sessionDir);
        } catch (IOException exception) {
            error("Failed to start diagnostics: %s", exception.getMessage());
            closeSession("open failed");
        }
    }

    private void closeSession(String reason) {
        if (!isOpen()) return;

        log("session-end reason=" + reason);
        writeSummary();
        if (writeAnalysis.get()) writeAnalysis(reason);
        close(events);
        close(packets);
        close(positions);
        close(summary);
        events = null;
        packets = null;
        positions = null;
        summary = null;
        sessionDir = null;
    }

    private void writeSessionInfo() {
        try (BufferedWriter writer = writer("session.md")) {
            ServerInfo info = mc.getCurrentServerEntry();
            writer.write("# Eclipse Server Diagnostics\n\n");
            writer.write("- Started: " + LocalDateTime.now() + "\n");
            writer.write("- Target: " + serverDisplayName() + "\n");
            writer.write("- Address: " + (info != null ? info.address : "singleplayer/local") + "\n");
            writer.write("- Server type: " + (info != null ? info.getServerType() : "integrated") + "\n");
            writer.write("- Resource pack policy: " + (info != null ? info.getResourcePackPolicy() : "local") + "\n");
            writer.write("- Initial ping: " + (info != null ? info.ping : -1) + "\n");
            writer.write("- Minecraft window: " + mc.getWindow().getScaledWidth() + "x" + mc.getWindow().getScaledHeight() + "\n");
            writer.write("- Files:\n");
            writer.write("  - events.log: corrections, velocity, disconnects, active module context\n");
            writer.write("  - packets.csv: optional packet timeline\n");
            writer.write("  - positions.csv: periodic position, speed, collision, active module samples\n");
            writer.write("  - summary.log: periodic counters and connection snapshots\n");
            writer.write("  - analysis.md: generated session interpretation\n");
        } catch (IOException exception) {
            log("failed-session-info " + exception.getMessage());
        }
    }

    private void writeSummary() {
        if (!isOpen()) return;

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(now()).append("] ");
        builder.append("elapsed=").append(elapsedMs()).append("ms ");
        builder.append("ticks=").append(ticks).append(" ");
        builder.append("corrections=").append(corrections).append(" ");
        builder.append("playerVelocity=").append(velocityUpdates).append(" ");
        builder.append("hCollisionSamples=").append(horizontalCollisionSamples).append(" ");
        builder.append("vCollisionSamples=").append(verticalCollisionSamples).append(" ");
        builder.append("maxHSpeed=").append(fmt(maxHorizontalSpeed)).append(" ");
        builder.append("maxYVel=").append(fmt(maxAbsYVelocity)).append(" ");

        if (mc.getCurrentServerEntry() != null) {
            builder.append("ping=").append(mc.getCurrentServerEntry().ping).append(" ");
        }

        if (mc.getNetworkHandler() != null) {
            int listed = mc.getNetworkHandler().getListedPlayerListEntries().size();
            int playerList = mc.getNetworkHandler().getPlayerList().size();
            builder.append("listedPlayers=").append(listed).append(" ");
            builder.append("playerList=").append(playerList).append(" ");
            builder.append("avgTabPing=").append(avgTabPing());
        }

        writeLine(summary, builder.toString());
        if (activeModules.get()) writeLine(summary, "  active=" + activeModuleSnapshot());

        if (packetSummary.get()) {
            writeLine(summary, "  rx=" + topPackets(receivedPackets));
            writeLine(summary, "  tx=" + topPackets(sentPackets));
        }
    }

    private void updateMovementStats() {
        if (mc.player.horizontalCollision) horizontalCollisionSamples++;
        if (mc.player.verticalCollision) verticalCollisionSamples++;

        Vec3d velocity = mc.player.getVelocity();
        maxHorizontalSpeed = Math.max(maxHorizontalSpeed, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z));
        maxAbsYVelocity = Math.max(maxAbsYVelocity, Math.abs(velocity.y));

        Vec3d current = playerPos();
        long now = elapsedMs();
        if (lastPosition != null && now > lastPositionMs) {
            double seconds = (now - lastPositionMs) / 1000.0;
            double dx = current.x - lastPosition.x;
            double dz = current.z - lastPosition.z;
            maxHorizontalSpeed = Math.max(maxHorizontalSpeed, Math.sqrt(dx * dx + dz * dz) / seconds);
        }
        lastPosition = current;
        lastPositionMs = now;
    }

    private void writeAnalysis(String reason) {
        try (BufferedWriter writer = writer("analysis.md")) {
            writer.write("# Eclipse Diagnostics Analysis\n\n");
            writer.write("- End reason: " + reason + "\n");
            writer.write("- Duration: " + elapsedMs() + " ms\n");
            writer.write("- Ticks: " + ticks + "\n");
            writer.write("- Corrections: " + corrections + "\n");
            writer.write("- Player velocity packets: " + velocityUpdates + "\n");
            writer.write("- Horizontal collision samples: " + horizontalCollisionSamples + "\n");
            writer.write("- Vertical collision samples: " + verticalCollisionSamples + "\n");
            writer.write("- Max horizontal speed: " + fmt(maxHorizontalSpeed) + "\n");
            writer.write("- Max abs Y velocity: " + fmt(maxAbsYVelocity) + "\n");
            writer.write("- Active Eclipse modules at close: " + activeModuleSnapshot() + "\n\n");
            writer.write("## Packet Totals\n\n");
            writer.write("- RX: " + topPackets(receivedPackets) + "\n");
            writer.write("- TX: " + topPackets(sentPackets) + "\n\n");
            writer.write("## Interpretation\n\n");
            if (corrections == 0) {
                writer.write("- No server position corrections were recorded during this session.\n");
            } else if (corrections <= 3) {
                writer.write("- A small number of server position corrections was recorded; compare event timestamps with active modules.\n");
            } else {
                writer.write("- Multiple server position corrections were recorded; reduce movement, phase, or velocity settings and retest one module at a time.\n");
            }
            if (velocityUpdates > 0) {
                writer.write("- The server sent player velocity updates. Test Velocity with packet CSV enabled when tuning knockback.\n");
            }
            if (horizontalCollisionSamples > 0) {
                writer.write("- Horizontal collision was present. PearlPhase and normal movement tests should be separated.\n");
            }
        } catch (IOException exception) {
            log("failed-analysis " + exception.getMessage());
        }
    }

    private int avgTabPing() {
        if (mc.getNetworkHandler() == null) return -1;

        int count = 0;
        int total = 0;
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            total += entry.getLatency();
            count++;
        }

        return count == 0 ? -1 : total / count;
    }

    private String topPackets(Map<String, Integer> map) {
        return map.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(12)
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((a, b) -> a + ", " + b)
            .orElse("none");
    }

    private void writePacket(String direction, String packetName) {
        if (!packetCsv.get()) return;
        if (packetRows >= packetCsvLimit.get()) return;
        packetRows++;
        writeLine(packets, elapsedMs() + "," + direction + "," + packetName);
    }

    private BufferedWriter writer(String file) throws IOException {
        return Files.newBufferedWriter(sessionDir.resolve(file), StandardCharsets.UTF_8);
    }

    private boolean isOpen() {
        return sessionDir != null && events != null;
    }

    private void log(String message) {
        writeLine(events, "[" + now() + "] " + message);
    }

    private void writeLine(BufferedWriter writer, String line) {
        if (writer == null) return;

        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    private void close(BufferedWriter writer) {
        if (writer == null) return;

        try {
            writer.close();
        } catch (IOException ignored) {
        }
    }

    private String serverFolderName() {
        String name = serverDisplayName();
        return name.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private String serverDisplayName() {
        ServerInfo info = mc.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) return info.address;
        if (mc.isInSingleplayer()) return "singleplayer";
        return "unknown-server";
    }

    private String packetName(Packet<?> packet) {
        return packet.getClass().getSimpleName();
    }

    private long elapsedMs() {
        return System.currentTimeMillis() - startedAt;
    }

    private double horizontalSpeed() {
        if (mc.player == null) return 0.0;
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    private String playerSnapshot() {
        if (mc.player == null) return "no-player";

        return "pos=" + vec(playerPos())
            + " vel=" + vec(mc.player.getVelocity())
            + " yaw=" + fmt(mc.player.getYaw())
            + " pitch=" + fmt(mc.player.getPitch())
            + " ground=" + mc.player.isOnGround()
            + " hCol=" + mc.player.horizontalCollision
            + " vCol=" + mc.player.verticalCollision;
    }

    private String activeModuleSnapshot() {
        if (!activeModules.get()) return "disabled";

        return Modules.get().getActive().stream()
            .filter(module -> module.category == Eclipse.CATEGORY)
            .map(module -> module.name)
            .sorted()
            .collect(Collectors.joining("|", "[", "]"));
    }

    private String now() {
        return LocalDateTime.now().format(LOG_TIME);
    }

    private Vec3d playerPos() {
        return new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private String vec(Vec3d vec) {
        return fmt(vec.x) + "/" + fmt(vec.y) + "/" + fmt(vec.z);
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
