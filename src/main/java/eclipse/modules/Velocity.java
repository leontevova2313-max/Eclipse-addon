package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Velocity extends Module {
    public enum Mode {
        Scale,
        GrimCancel,
        GrimSkip
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExplosions = settings.createGroup("Explosions");
    private final SettingGroup sgGrim = settings.createGroup("Grim");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Velocity handling mode.")
        .defaultValue(Mode.Scale)
        .build()
    );

    private final Setting<Double> horizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal")
        .description("Horizontal knockback multiplier in percent.")
        .defaultValue(75.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale)
        .build()
    );

    private final Setting<Double> vertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical")
        .description("Vertical knockback multiplier in percent.")
        .defaultValue(100.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale)
        .build()
    );

    private final Setting<Boolean> explosions = sgExplosions.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Applies the same handling to explosion knockback.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> explosionHorizontal = sgExplosions.add(new DoubleSetting.Builder()
        .name("explosion-horizontal")
        .description("Horizontal explosion knockback multiplier in percent.")
        .defaultValue(80.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale)
        .build()
    );

    private final Setting<Double> explosionVertical = sgExplosions.add(new DoubleSetting.Builder()
        .name("explosion-vertical")
        .description("Vertical explosion knockback multiplier in percent.")
        .defaultValue(100.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale)
        .build()
    );

    private final Setting<Integer> confirmDelay = sgGrim.add(new IntSetting.Builder()
        .name("confirm-delay")
        .description("Ticks before sending GrimCancel confirmation packets.")
        .defaultValue(1)
        .range(0, 10)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.GrimCancel)
        .build()
    );

    private final Setting<Integer> skipPackets = sgGrim.add(new IntSetting.Builder()
        .name("skip-packets")
        .description("Movement packets skipped after cancelling knockback in GrimSkip.")
        .defaultValue(6)
        .range(1, 20)
        .sliderRange(1, 10)
        .visible(() -> mode.get() == Mode.GrimSkip)
        .build()
    );

    private final Setting<Boolean> sendStopDestroy = sgGrim.add(new BoolSetting.Builder()
        .name("stop-destroy")
        .description("Sends STOP_DESTROY_BLOCK after GrimCancel velocity cancellation.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.GrimCancel)
        .build()
    );

    private final Setting<Boolean> serverSafe = sgSafety.add(new BoolSetting.Builder()
        .name("server-safe")
        .description("Keeps old aggressive saved values from cancelling all server velocity on strict servers.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> passCorrections = sgSafety.add(new BoolSetting.Builder()
        .name("pass-corrections")
        .description("Lets server correction velocity packets through so knockback handling does not fight position setbacks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> correctionWindow = sgSafety.add(new IntSetting.Builder()
        .name("correction-window")
        .description("Ticks after a server position correction where player velocity packets are trusted.")
        .defaultValue(8)
        .range(0, 60)
        .sliderRange(0, 30)
        .build()
    );

    private boolean damageWindow;
    private int confirmTicks;
    private int skipTicks;
    private int correctionTicks;

    public Velocity() {
        super(Eclipse.CATEGORY, "eclipse-velocity", "Scales knockback or applies Grim-style velocity cancellation modes.");
    }

    @Override
    public void onActivate() {
        damageWindow = false;
        confirmTicks = 0;
        skipTicks = 0;
        correctionTicks = 0;
    }

    @Override
    public void onDeactivate() {
        damageWindow = false;
        confirmTicks = 0;
        skipTicks = 0;
        correctionTicks = 0;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.packet instanceof EntityDamageS2CPacket packet && packet.entityId() == mc.player.getId()) {
            damageWindow = true;
            return;
        }

        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            damageWindow = false;
            confirmTicks = 0;
            skipTicks = 0;
            correctionTicks = correctionWindow.get();
            return;
        }

        EntityVelocityUpdateS2CPacket playerVelocityPacket = null;
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            playerVelocityPacket = packet;
        }
        boolean playerVelocity = playerVelocityPacket != null;
        boolean explosionVelocity = explosions.get() && event.packet instanceof ExplosionS2CPacket;
        if (!playerVelocity && !explosionVelocity) return;
        if (playerVelocity && passCorrections.get() && shouldTrustServerVelocity(playerVelocityPacket.getVelocity())) return;

        switch (effectiveMode()) {
            case Scale -> handleScale(event);
            case GrimCancel -> handleGrimCancel(event);
            case GrimSkip -> handleGrimSkip(event);
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (mode.get() != Mode.GrimSkip || skipTicks <= 0) return;
        if (event.packet instanceof PlayerMoveC2SPacket) {
            skipTicks--;
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (correctionTicks > 0) correctionTicks--;
        if (confirmTicks <= 0) return;

        confirmTicks--;
        if (confirmTicks > 0) return;

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            mc.player.getYaw(),
            mc.player.getPitch(),
            mc.player.isOnGround(),
            mc.player.horizontalCollision
        ));

        if (sendStopDestroy.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                mc.player.getBlockPos(),
                Direction.UP
            ));
        }
    }

    private void handleScale(PacketEvent.Receive event) {
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            double horizontalPercent = effectiveHorizontal(horizontal.get());
            double verticalPercent = effectiveVertical(vertical.get());
            if (horizontalPercent == 100.0 && verticalPercent == 100.0) return;
            Vec3d scaled = scale(packet.getVelocity(), horizontalPercent, verticalPercent);
            event.setCancelled(true);
            mc.player.setVelocity(scaled);
        } else if (event.packet instanceof ExplosionS2CPacket packet && packet.playerKnockback().isPresent()) {
            double horizontalPercent = effectiveHorizontal(explosionHorizontal.get());
            double verticalPercent = effectiveVertical(explosionVertical.get());
            if (horizontalPercent == 100.0 && verticalPercent == 100.0) return;
            Vec3d scaled = scale(packet.playerKnockback().get(), horizontalPercent, verticalPercent);
            event.setCancelled(true);
            if (scaled.lengthSquared() > 0.0) mc.player.addVelocity(scaled);
        }
    }

    private void handleGrimCancel(PacketEvent.Receive event) {
        if (!damageWindow) return;
        event.setCancelled(true);
        confirmTicks = Math.max(1, confirmDelay.get());
        damageWindow = false;
    }

    private void handleGrimSkip(PacketEvent.Receive event) {
        if (!damageWindow) return;
        event.setCancelled(true);
        skipTicks = skipPackets.get();
        damageWindow = false;
    }

    private Vec3d scale(Vec3d velocity, double horizontalPercent, double verticalPercent) {
        double h = horizontalPercent / 100.0;
        double v = verticalPercent / 100.0;
        return new Vec3d(velocity.x * h, velocity.y * v, velocity.z * h);
    }

    private double effectiveHorizontal(double value) {
        return serverSafe.get() ? Math.max(value, 75.0) : value;
    }

    private double effectiveVertical(double value) {
        return serverSafe.get() ? Math.max(value, 100.0) : value;
    }

    private Mode effectiveMode() {
        return serverSafe.get() ? Mode.Scale : mode.get();
    }

    private boolean shouldTrustServerVelocity(Vec3d velocity) {
        return correctionTicks > 0 || looksLikeCorrectionVelocity(velocity);
    }

    private boolean looksLikeCorrectionVelocity(Vec3d velocity) {
        return Math.abs(velocity.x) < 0.003
            && Math.abs(velocity.z) < 0.003
            && velocity.y < 0.0
            && velocity.y >= -0.120;
    }
}
