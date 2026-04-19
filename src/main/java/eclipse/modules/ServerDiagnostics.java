package eclipse.modules;

import eclipse.Eclipse;
import eclipse.diagnostics.DiagnosticCategory;
import eclipse.diagnostics.DiagnosticEvent;
import eclipse.diagnostics.DiagnosticExporter;
import eclipse.diagnostics.DiagnosticSessionStats;
import eclipse.diagnostics.DiagnosticStore;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

public class ServerDiagnostics extends Module {
    public enum PacketMode {
        Off,
        ImportantOnly,
        Verbose
    }

    private static final DateTimeFormatter FOLDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final double HORIZONTAL_ANOMALY_SPEED = 1.25;
    private static final double VERTICAL_ANOMALY_SPEED = 1.15;

    private final SettingGroup sgCategories = settings.getDefaultGroup();
    private final SettingGroup sgStorage = settings.createGroup("Storage");
    private final SettingGroup sgOutput = settings.createGroup("Output");

    private final Setting<Boolean> networkDiagnostics = sgCategories.add(new BoolSetting.Builder()
        .name("network-diagnostics")
        .description("Captures notable network events and optional packet counters.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> movementDiagnostics = sgCategories.add(new BoolSetting.Builder()
        .name("movement-diagnostics")
        .description("Captures corrections, velocity responses, and movement anomalies.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> combatDiagnostics = sgCategories.add(new BoolSetting.Builder()
        .name("combat-interaction-diagnostics")
        .description("Captures compact attack, entity interaction, and block interaction events.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> moduleContext = sgCategories.add(new BoolSetting.Builder()
        .name("module-context")
        .description("Attaches active Eclipse modules only to important events.")
        .defaultValue(true)
        .build()
    );

    private final Setting<PacketMode> packetMode = sgCategories.add(new EnumSetting.Builder<PacketMode>()
        .name("packet-mode")
        .description("Off, important packet events only, or bounded verbose packet history.")
        .defaultValue(PacketMode.ImportantOnly)
        .build()
    );

    private final Setting<Integer> historyLimit = sgStorage.add(new IntSetting.Builder()
        .name("history-limit")
        .description("Maximum stored events per category.")
        .defaultValue(300)
        .range(50, 5000)
        .sliderRange(100, 1000)
        .build()
    );

    private final Setting<Integer> throttleMs = sgStorage.add(new IntSetting.Builder()
        .name("throttle-ms")
        .description("Minimum time between repetitive stored events of the same type.")
        .defaultValue(750)
        .range(0, 10000)
        .sliderRange(0, 3000)
        .build()
    );

    private final Setting<Integer> movementSampleInterval = sgStorage.add(new IntSetting.Builder()
        .name("movement-sample-interval")
        .description("Ticks between lightweight movement stat samples.")
        .defaultValue(5)
        .range(1, 100)
        .sliderRange(1, 40)
        .build()
    );

    private final Setting<Boolean> autoClearOnWorldChange = sgStorage.add(new BoolSetting.Builder()
        .name("auto-clear-world-change")
        .description("Clears live buffers when joining or leaving a world.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> clearOnEnable = sgStorage.add(new BoolSetting.Builder()
        .name("clear-on-enable")
        .description("Starts a fresh diagnostic session each time the module is enabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> exportOnClose = sgOutput.add(new BoolSetting.Builder()
        .name("export-on-close")
        .description("Writes bounded events and summary files when the session closes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> printSummary = sgOutput.add(new BoolSetting.Builder()
        .name("print-summary")
        .description("Prints one compact summary when diagnostics stop.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debugMode = sgOutput.add(new BoolSetting.Builder()
        .name("debug-mode")
        .description("Prints rare lifecycle messages. Does not print every event.")
        .defaultValue(false)
        .build()
    );

    private final DiagnosticStore store = new DiagnosticStore(300);
    private final DiagnosticSessionStats stats = new DiagnosticSessionStats();
    private Path sessionDir;
    private Vec3d lastPosition;
    private long lastPositionMs;
    private int horizontalCollisionStreak;
    private int verticalCollisionStreak;

    public ServerDiagnostics() {
        super(Eclipse.CATEGORY, "server-diagnostics", "Structured server diagnostics for corrections, packets, interactions, module context, and session summaries.");
    }

    @Override
    public void onActivate() {
        if (clearOnEnable.get()) resetSession();
        if (mc.world != null) openSession("module enabled");
    }

    @Override
    public void onDeactivate() {
        closeSession("module disabled");
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (autoClearOnWorldChange.get()) resetSession();
        openSession("joined world");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        closeSession("left world");
        if (autoClearOnWorldChange.get()) resetSession();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isOpen()) return;

        String packetName = packetName(event.packet);
        if (packetMode.get() != PacketMode.Off) store.countPacket("rx", packetName);

        if (packetMode.get() == PacketMode.Verbose && networkDiagnostics.get()) {
            add(DiagnosticCategory.Network, "packet-rx", packetName, false);
        }

        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            stats.corrections++;
            if (movementDiagnostics.get()) {
                add(DiagnosticCategory.Movement, "server-correction",
                    "teleportId=" + packet.teleportId()
                        + " pos=" + vec(packet.change().position())
                        + " delta=" + vec(packet.change().deltaMovement())
                        + " yaw=" + fmt(packet.change().yaw())
                        + " pitch=" + fmt(packet.change().pitch())
                        + " local=" + playerSnapshot(),
                    true);
            }
        } else if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (mc.player != null && packet.getEntityId() == mc.player.getId()) {
                stats.localVelocityPackets++;
                if (movementDiagnostics.get()) {
                    add(DiagnosticCategory.Movement, "local-velocity",
                        "velocity=" + vec(packet.getVelocity()) + " local=" + playerSnapshot(),
                        true);
                }
            }
        } else if (event.packet instanceof ExplosionS2CPacket packet && packet.playerKnockback().isPresent()) {
            if (movementDiagnostics.get()) {
                add(DiagnosticCategory.Movement, "explosion-knockback",
                    "center=" + vec(packet.center())
                        + " radius=" + fmt(packet.radius())
                        + " knockback=" + vec(packet.playerKnockback().get()),
                    true);
            }
        } else if (networkDiagnostics.get() && event.packet instanceof DisconnectS2CPacket packet) {
            stats.disconnects++;
            add(DiagnosticCategory.Network, "disconnect", packet.reason().getString(), true);
        } else if (networkDiagnostics.get() && event.packet instanceof LoginDisconnectS2CPacket packet) {
            stats.disconnects++;
            add(DiagnosticCategory.Network, "login-disconnect", packet.reason().getString(), true);
        } else if (networkDiagnostics.get() && event.packet instanceof ServerMetadataS2CPacket packet) {
            add(DiagnosticCategory.Network, "server-metadata",
                "description=" + packet.description().getString() + " favicon=" + packet.favicon().isPresent(),
                false);
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isOpen()) return;

        String packetName = packetName(event.packet);
        if (packetMode.get() != PacketMode.Off) store.countPacket("tx", packetName);

        if (packetMode.get() == PacketMode.Verbose && networkDiagnostics.get()) {
            add(DiagnosticCategory.Network, "packet-tx", packetName, false);
        }

        if (!combatDiagnostics.get()) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            stats.interactionPackets++;
            add(DiagnosticCategory.Combat, "entity-interaction",
                "sneaking=" + packet.isPlayerSneaking() + " local=" + playerSnapshot(),
                true);
        } else if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            stats.blockInteractions++;
            add(DiagnosticCategory.Combat, "block-interaction",
                "hand=" + packet.getHand()
                    + " pos=" + packet.getBlockHitResult().getBlockPos()
                    + " side=" + packet.getBlockHitResult().getSide()
                    + " sequence=" + packet.getSequence(),
                true);
        } else if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            stats.interactionPackets++;
            add(DiagnosticCategory.Combat, "item-interaction",
                "hand=" + packet.getHand()
                    + " sequence=" + packet.getSequence()
                    + " yaw=" + fmt(packet.getYaw())
                    + " pitch=" + fmt(packet.getPitch()),
                true);
        } else if (event.packet instanceof PlayerActionC2SPacket packet && isNotableBlockAction(packet)) {
            stats.blockInteractions++;
            add(DiagnosticCategory.Combat, "block-action",
                "action=" + packet.getAction()
                    + " pos=" + packet.getPos()
                    + " direction=" + packet.getDirection()
                    + " sequence=" + packet.getSequence(),
                true);
        } else if (event.packet instanceof HandSwingC2SPacket packet) {
            add(DiagnosticCategory.Combat, "hand-swing", "hand=" + packet.getHand(), false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isOpen() || mc.player == null) return;

        stats.ticks++;
        if (stats.ticks % movementSampleInterval.get() != 0) return;
        updateMovementStats();
    }

    private void openSession(String reason) {
        if (isOpen()) closeSession("new session");

        stats.reset(System.currentTimeMillis());
        store.reset(historyLimit.get());
        lastPosition = null;
        lastPositionMs = 0;
        horizontalCollisionStreak = 0;
        verticalCollisionStreak = 0;
        sessionDir = diagnosticsRoot().resolve(LocalDateTime.now().format(FOLDER_TIME));

        add(DiagnosticCategory.Summary, "session-start", reason + " target=" + serverDisplayName(), true);
        if (debugMode.get()) info("Diagnostics started: %s", serverDisplayName());
    }

    private void closeSession(String reason) {
        if (!isOpen()) return;

        add(DiagnosticCategory.Summary, "session-end", reason, true);
        if (exportOnClose.get()) {
            try {
                DiagnosticExporter.export(sessionDir, store, stats, reason, activeModuleSnapshot());
            } catch (IOException exception) {
                error("Failed to export diagnostics: %s", exception.getMessage());
            }
        }

        if (printSummary.get()) info("Diagnostics summary: %s", DiagnosticExporter.compactSummary(store, stats));
        sessionDir = null;
    }

    private void resetSession() {
        store.reset(historyLimit.get());
        stats.reset(System.currentTimeMillis());
        lastPosition = null;
        lastPositionMs = 0;
        horizontalCollisionStreak = 0;
        verticalCollisionStreak = 0;
    }

    private void updateMovementStats() {
        Vec3d velocity = mc.player.getVelocity();
        double horizontalVelocity = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        stats.maxHorizontalSpeed = Math.max(stats.maxHorizontalSpeed, horizontalVelocity);
        stats.maxAbsYVelocity = Math.max(stats.maxAbsYVelocity, Math.abs(velocity.y));

        if (mc.player.horizontalCollision) {
            stats.horizontalCollisionSamples++;
            horizontalCollisionStreak++;
        } else {
            horizontalCollisionStreak = 0;
        }

        if (mc.player.verticalCollision) {
            stats.verticalCollisionSamples++;
            verticalCollisionStreak++;
        } else {
            verticalCollisionStreak = 0;
        }

        long now = elapsedMs();
        Vec3d current = playerPos();
        if (lastPosition != null && now > lastPositionMs) {
            double seconds = (now - lastPositionMs) / 1000.0;
            double dx = current.x - lastPosition.x;
            double dy = current.y - lastPosition.y;
            double dz = current.z - lastPosition.z;
            double horizontalSpeed = Math.sqrt(dx * dx + dz * dz) / seconds;
            double verticalSpeed = Math.abs(dy / seconds);
            stats.maxHorizontalSpeed = Math.max(stats.maxHorizontalSpeed, horizontalSpeed);
            stats.maxAbsYVelocity = Math.max(stats.maxAbsYVelocity, verticalSpeed);

            if (horizontalSpeed > HORIZONTAL_ANOMALY_SPEED || verticalSpeed > VERTICAL_ANOMALY_SPEED) {
                stats.movementAnomalies++;
                if (movementDiagnostics.get()) {
                    add(DiagnosticCategory.Movement, "movement-anomaly",
                        "hSpeed=" + fmt(horizontalSpeed)
                            + " ySpeed=" + fmt(verticalSpeed)
                            + " from=" + vec(lastPosition)
                            + " to=" + vec(current),
                        true);
                }
            }
        }

        if (horizontalCollisionStreak == 6 || verticalCollisionStreak == 6) {
            stats.movementAnomalies++;
            add(DiagnosticCategory.Movement, "collision-streak",
                "horizontalTicks=" + horizontalCollisionStreak
                    + " verticalTicks=" + verticalCollisionStreak
                    + " local=" + playerSnapshot(),
                true);
        }

        lastPosition = current;
        lastPositionMs = now;
    }

    private void add(DiagnosticCategory category, String type, String detail, boolean withModules) {
        if (category == DiagnosticCategory.ModuleContext && !moduleContext.get()) return;
        String modules = withModules && moduleContext.get() ? activeModuleSnapshot() : "";
        boolean stored = store.add(new DiagnosticEvent(elapsedMs(), category, type, detail, modules), throttleMs.get());
        if (stored && withModules && moduleContext.get()) {
            store.add(new DiagnosticEvent(elapsedMs(), DiagnosticCategory.ModuleContext, type, modules, ""), throttleMs.get());
        }
    }

    private boolean isNotableBlockAction(PlayerActionC2SPacket packet) {
        return switch (packet.getAction()) {
            case START_DESTROY_BLOCK, STOP_DESTROY_BLOCK, ABORT_DESTROY_BLOCK -> true;
            default -> false;
        };
    }

    private boolean isOpen() {
        return sessionDir != null;
    }

    private Path diagnosticsRoot() {
        return mc.runDirectory.toPath()
            .resolve("eclipse-diagnostics")
            .resolve(serverFolderName());
    }

    private String serverFolderName() {
        return serverDisplayName().replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private String serverDisplayName() {
        ServerInfo info = mc.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) return canonicalServerAddress(info.address);
        if (mc.isInSingleplayer()) return "singleplayer";
        return "unknown-server";
    }

    private String canonicalServerAddress(String address) {
        String trimmed = address.trim();
        String host = trimmed;
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (lowerHost.equals("karasique.com")
            || lowerHost.equals("mc.karasique.com")
            || lowerHost.equals("play.karasique.com")) {
            return "karasique.com";
        }

        return trimmed;
    }

    private String packetName(Packet<?> packet) {
        return packet.getClass().getSimpleName();
    }

    private long elapsedMs() {
        return Math.max(0, System.currentTimeMillis() - stats.startedAt);
    }

    private Vec3d playerPos() {
        return new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
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
        if (!moduleContext.get()) return "disabled";
        return Modules.get().getActive().stream()
            .filter(module -> module.category == Eclipse.CATEGORY)
            .map(module -> module.name)
            .sorted()
            .collect(Collectors.joining("|", "[", "]"));
    }

    private String vec(Vec3d vec) {
        return fmt(vec.x) + "/" + fmt(vec.y) + "/" + fmt(vec.z);
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
