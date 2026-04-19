package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
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
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class Velocity extends Module {
    public enum Mode {
        Cancel,
        Scale,
        JumpReset,
        GrimCancel
    }

    public enum CorrectionPolicy {
        Off,
        GrimCancelOnly,
        AllModes
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExplosions = settings.createGroup("Explosions");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How incoming knockback is handled.")
        .defaultValue(Mode.Scale)
        .build()
    );

    private final Setting<Double> horizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal")
        .description("Horizontal knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale || mode.get() == Mode.JumpReset)
        .build()
    );

    private final Setting<Double> vertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical")
        .description("Vertical knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale || mode.get() == Mode.JumpReset)
        .build()
    );

    private final Setting<Boolean> onlyPlayersVelocity = sgGeneral.add(new BoolSetting.Builder()
        .name("only-player-velocity")
        .description("Only handles velocity packets aimed at you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> explosions = sgExplosions.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Also handles explosion knockback.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> explosionHorizontal = sgExplosions.add(new DoubleSetting.Builder()
        .name("explosion-horizontal")
        .description("Horizontal explosion knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale || mode.get() == Mode.JumpReset)
        .build()
    );

    private final Setting<Double> explosionVertical = sgExplosions.add(new DoubleSetting.Builder()
        .name("explosion-vertical")
        .description("Vertical explosion knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(() -> mode.get() == Mode.Scale || mode.get() == Mode.JumpReset)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to pass velocity after a server setback when correction policy allows it.")
        .defaultValue(0)
        .range(0, 60)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<CorrectionPolicy> correctionPolicy = sgSafety.add(new EnumSetting.Builder<CorrectionPolicy>()
        .name("correction-policy")
        .description("Whether server corrections should temporarily bypass Velocity.")
        .defaultValue(CorrectionPolicy.Off)
        .build()
    );

    private final Setting<Integer> grimConfirmDelay = sgSafety.add(new IntSetting.Builder()
        .name("grim-confirm-delay")
        .description("Ticks before sending the extra confirmation packet in GrimCancel.")
        .defaultValue(1)
        .range(0, 10)
        .sliderRange(0, 5)
        .visible(() -> mode.get() == Mode.GrimCancel)
        .build()
    );

    private final Setting<Boolean> stopDestroy = sgSafety.add(new BoolSetting.Builder()
        .name("stop-destroy")
        .description("Sends STOP_DESTROY_BLOCK after GrimCancel.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.GrimCancel)
        .build()
    );

    private int correctionTicks;
    private int confirmTicks;

    public Velocity() {
        super(Eclipse.CATEGORY, "eclipse-velocity", "Direct knockback control: cancel, scale, jump-reset, or Grim confirm.");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        resetState();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        resetState();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            if (shouldPauseAfterCorrection()) correctionTicks = correctionPause.get();
            return;
        }

        if (correctionTicks > 0) return;

        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            handleEntityVelocity(event, packet);
        } else if (explosions.get() && event.packet instanceof ExplosionS2CPacket packet && packet.playerKnockback().isPresent()) {
            handleExplosionVelocity(event, packet);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (correctionTicks > 0) correctionTicks--;

        if (confirmTicks > 0 && --confirmTicks == 0) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
            ));

            if (stopDestroy.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    mc.player.getBlockPos(),
                    Direction.UP
                ));
            }
        }
    }

    private void handleEntityVelocity(PacketEvent.Receive event, EntityVelocityUpdateS2CPacket packet) {
        if (packet.getEntityId() != mc.player.getId()) {
            if (onlyPlayersVelocity.get()) return;
            return;
        }

        Vec3d velocity = packet.getVelocity();
        switch (mode.get()) {
            case Cancel -> {
                event.packet = new EntityVelocityUpdateS2CPacket(packet.getEntityId(), Vec3d.ZERO);
            }
            case Scale -> {
                event.packet = new EntityVelocityUpdateS2CPacket(packet.getEntityId(), scale(velocity, horizontal.get(), vertical.get()));
            }
            case JumpReset -> {
                event.setCancelled(true);
                if (mc.player.isOnGround()) mc.player.jump();
                Vec3d jumpVelocity = mc.player.getVelocity();
                Vec3d scaled = scale(velocity, horizontal.get(), vertical.get());
                mc.player.setVelocity(scaled.x, Math.max(jumpVelocity.y, scaled.y), scaled.z);
            }
            case GrimCancel -> {
                event.setCancelled(true);
                confirmTicks = Math.max(1, grimConfirmDelay.get());
            }
        }
    }

    private void handleExplosionVelocity(PacketEvent.Receive event, ExplosionS2CPacket packet) {
        Vec3d knockback = packet.playerKnockback().orElse(Vec3d.ZERO);
        Vec3d scaled = switch (mode.get()) {
            case Cancel, GrimCancel -> Vec3d.ZERO;
            case Scale, JumpReset -> scale(knockback, explosionHorizontal.get(), explosionVertical.get());
        };

        if (mode.get() == Mode.JumpReset && mc.player.isOnGround()) mc.player.jump();

        event.packet = new ExplosionS2CPacket(
            packet.center(),
            packet.radius(),
            packet.blockCount(),
            Optional.of(scaled),
            packet.explosionParticle(),
            packet.explosionSound(),
            packet.blockParticles()
        );

        if (mode.get() == Mode.GrimCancel) confirmTicks = Math.max(1, grimConfirmDelay.get());
    }

    private boolean shouldPauseAfterCorrection() {
        if (correctionPause.get() <= 0) return false;
        return switch (correctionPolicy.get()) {
            case Off -> false;
            case GrimCancelOnly -> mode.get() == Mode.GrimCancel;
            case AllModes -> true;
        };
    }

    private void resetState() {
        correctionTicks = 0;
        confirmTicks = 0;
    }

    private Vec3d scale(Vec3d velocity, double horizontalPercent, double verticalPercent) {
        double h = horizontalPercent / 100.0;
        double v = verticalPercent / 100.0;
        return new Vec3d(velocity.x * h, velocity.y * v, velocity.z * h);
    }
}
