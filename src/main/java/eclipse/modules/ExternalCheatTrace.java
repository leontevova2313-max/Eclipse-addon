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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.Packet;

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

public class ExternalCheatTrace extends Module {
    private static final DateTimeFormatter FOLDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilters = settings.createGroup("Filters");

    private final Setting<Boolean> autoSession = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-session")
        .description("Starts a new trace session when joining a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> csvLimit = sgGeneral.add(new IntSetting.Builder()
        .name("csv-limit")
        .description("Maximum rows written per trace session.")
        .defaultValue(60000)
        .range(1000, 500000)
        .sliderRange(5000, 120000)
        .build()
    );

    private final Setting<Boolean> burstWarnings = sgGeneral.add(new BoolSetting.Builder()
        .name("burst-warnings")
        .description("Prints a warning when another module sends many action packets in one tick.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> burstThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("burst-threshold")
        .description("Packets per tick needed for a burst warning.")
        .defaultValue(12)
        .range(2, 120)
        .sliderRange(4, 40)
        .build()
    );

    private final Setting<Boolean> movementPackets = sgFilters.add(new BoolSetting.Builder()
        .name("movement-packets")
        .description("Logs player movement packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> interactionPackets = sgFilters.add(new BoolSetting.Builder()
        .name("interaction-packets")
        .description("Logs block, item, entity, attack, swing, and action packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> inventoryPackets = sgFilters.add(new BoolSetting.Builder()
        .name("inventory-packets")
        .description("Logs slot, click, creative inventory, and carried item packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> otherPackets = sgFilters.add(new BoolSetting.Builder()
        .name("other-packets")
        .description("Logs packets that do not match the main trace groups.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> receivedCorrections = sgFilters.add(new BoolSetting.Builder()
        .name("received-corrections")
        .description("Logs server position correction packets while tracing.")
        .defaultValue(true)
        .build()
    );

    private final Map<String, Integer> sentCounts = new HashMap<>();
    private final Map<String, Integer> receivedCounts = new HashMap<>();

    private BufferedWriter actions;
    private BufferedWriter summary;
    private Path sessionDir;
    private long startedAt;
    private int ticks;
    private int rows;
    private int sentThisTick;

    public ExternalCheatTrace() {
        super(Eclipse.CATEGORY, "external-cheat-trace", "Records packet-level behavior from closed-source client modules loaded in the same instance.");
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
        if (autoSession.get()) openSession();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        closeSession("left server");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;
        if (burstWarnings.get() && sentThisTick >= burstThreshold.get()) {
            warning("Packet burst: %s packets in one tick. Trace: %s", sentThisTick, sessionDir);
        }
        sentThisTick = 0;
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isOpen() || rows >= csvLimit.get()) return;

        String packetName = packetName(event.packet);
        if (!shouldLogSend(packetName)) return;

        sentCounts.merge(packetName, 1, Integer::sum);
        sentThisTick++;
        writeAction("tx", packetName);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isOpen() || rows >= csvLimit.get() || !receivedCorrections.get()) return;

        String packetName = packetName(event.packet);
        if (!packetName.contains("PlayerPositionLook")) return;

        receivedCounts.merge(packetName, 1, Integer::sum);
        writeAction("rx", packetName);
    }

    private void openSession() {
        closeSession("new session");

        try {
            startedAt = System.currentTimeMillis();
            ticks = 0;
            rows = 0;
            sentThisTick = 0;
            sentCounts.clear();
            receivedCounts.clear();

            sessionDir = mc.runDirectory.toPath()
                .resolve("eclipse-external-trace")
                .resolve(serverFolderName())
                .resolve(LocalDateTime.now().format(FOLDER_TIME));
            Files.createDirectories(sessionDir);

            actions = writer("actions.csv");
            summary = writer("summary.txt");
            writeLine(actions, "elapsed_ms,tick,direction,packet,x,y,z,yaw,pitch,on_ground,velocity_x,velocity_y,velocity_z");
            writeLine(summary, "External cheat trace");
            writeLine(summary, "server=" + serverFolderName());
            writeLine(summary, "path=" + sessionDir);
            info("Trace started: %s", sessionDir);
        } catch (IOException exception) {
            error("Failed to start trace: %s", exception.getMessage());
            closeSession("open failed");
        }
    }

    private void closeSession(String reason) {
        if (!isOpen()) return;

        try {
            writeLine(summary, "");
            writeLine(summary, "closed=" + reason);
            writeLine(summary, "duration_ms=" + elapsedMs());
            writeLine(summary, "rows=" + rows);
            writeLine(summary, "");
            writeLine(summary, "sent_packets");
            for (String line : counts(sentCounts)) writeLine(summary, line);
            writeLine(summary, "");
            writeLine(summary, "received_packets");
            for (String line : counts(receivedCounts)) writeLine(summary, line);
            info("Trace closed: %s", sessionDir);
        } finally {
            close(actions);
            close(summary);
            actions = null;
            summary = null;
            sessionDir = null;
        }
    }

    private boolean shouldLogSend(String packetName) {
        if (movementPackets.get() && packetName.contains("PlayerMove")) return true;
        if (interactionPackets.get() && isInteraction(packetName)) return true;
        if (inventoryPackets.get() && isInventory(packetName)) return true;
        return otherPackets.get();
    }

    private boolean isInteraction(String packetName) {
        return packetName.contains("Interact")
            || packetName.contains("Action")
            || packetName.contains("Swing")
            || packetName.contains("Attack")
            || packetName.contains("Command")
            || packetName.contains("UseItem");
    }

    private boolean isInventory(String packetName) {
        return packetName.contains("Slot")
            || packetName.contains("Click")
            || packetName.contains("Inventory")
            || packetName.contains("Selected")
            || packetName.contains("Creative");
    }

    private void writeAction(String direction, String packetName) {
        if (mc.player == null) return;

        writeLine(actions, elapsedMs() + ","
            + ticks + ","
            + direction + ","
            + packetName + ","
            + fmt(mc.player.getX()) + ","
            + fmt(mc.player.getY()) + ","
            + fmt(mc.player.getZ()) + ","
            + fmt(mc.player.getYaw()) + ","
            + fmt(mc.player.getPitch()) + ","
            + mc.player.isOnGround() + ","
            + fmt(mc.player.getVelocity().x) + ","
            + fmt(mc.player.getVelocity().y) + ","
            + fmt(mc.player.getVelocity().z));
        rows++;
    }

    private String packetName(Packet<?> packet) {
        String name = packet.getClass().getSimpleName();
        return name.isEmpty() ? packet.getClass().getName() : name;
    }

    private boolean isOpen() {
        return actions != null && summary != null;
    }

    private BufferedWriter writer(String file) throws IOException {
        return Files.newBufferedWriter(sessionDir.resolve(file), StandardCharsets.UTF_8);
    }

    private void writeLine(BufferedWriter writer, String line) {
        if (writer == null) return;
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            error("Trace write failed: %s", exception.getMessage());
        }
    }

    private void close(BufferedWriter writer) {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException ignored) {
        }
    }

    private long elapsedMs() {
        return System.currentTimeMillis() - startedAt;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.5f", value);
    }

    private String serverFolderName() {
        ServerInfo server = mc.getCurrentServerEntry();
        if (server == null || server.address == null || server.address.isBlank()) return "singleplayer";
        return server.address.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private Iterable<String> counts(Map<String, Integer> counts) {
        return counts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.toList());
    }
}
