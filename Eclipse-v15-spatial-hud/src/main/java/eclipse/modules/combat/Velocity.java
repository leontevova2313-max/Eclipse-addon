package eclipse.modules.combat;

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

    private final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
        .name("knockback")
        .description("Handles direct entity velocity packets sent to the player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> horizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal")
        .description("Horizontal knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(this::usesScaling)
        .build()
    );

    private final Setting<Double> vertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical")
        .description("Vertical knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(this::usesScaling)
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
        .visible(this::usesScaling)
        .build()
    );

    private final Setting<Double> explosionVertical = sgExplosions.add(new DoubleSetting.Builder()
        .name("explosion-vertical")
        .description("Vertical explosion knockback multiplier in percent.")
        .defaultValue(0.0)
        .range(0.0, 200.0)
        .sliderRange(0.0, 100.0)
        .decimalPlaces(1)
        .visible(this::usesScaling)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to temporarily pass velocity after a server correction.")
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
        super(Eclipse.COMBAT, "eclipse-velocity", "Meteor-style velocity control with clean scaling, jump reset, and Grim cancel behavior.");
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
            confirmTicks = 0;
            return;
        }

        if (correctionTicks > 0) return;

        if (knockback.get() && event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
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
        if (packet.getEntityId() != mc.player.getId()) return;

        Vec3d velocity = packet.getVelocity();
        switch (mode.get()) {
            case Cancel -> event.packet = new EntityVelocityUpdateS2CPacket(packet.getEntityId(), Vec3d.ZERO);
            case Scale -> event.packet = new EntityVelocityUpdateS2CPacket(packet.getEntityId(), scale(velocity, horizontal.get(), vertical.get()));
            case JumpReset -> {
                event.setCancelled(true);
                applyJumpReset(scale(velocity, horizontal.get(), vertical.get()));
            }
            case GrimCancel -> {
                event.setCancelled(true);
                scheduleGrimConfirm();
            }
        }
    }

    private void handleExplosionVelocity(PacketEvent.Receive event, ExplosionS2CPacket packet) {
        Vec3d knockback = packet.playerKnockback().orElse(Vec3d.ZERO);

        switch (mode.get()) {
            case Cancel -> event.packet = rewriteExplosion(packet, Vec3d.ZERO);
            case Scale -> event.packet = rewriteExplosion(packet, scale(knockback, explosionHorizontal.get(), explosionVertical.get()));
            case JumpReset -> {
                event.setCancelled(true);
                applyJumpReset(scale(knockback, explosionHorizontal.get(), explosionVertical.get()));
                event.packet = rewriteExplosion(packet, Vec3d.ZERO);
            }
            case GrimCancel -> {
                event.packet = rewriteExplosion(packet, Vec3d.ZERO);
                scheduleGrimConfirm();
            }
        }
    }

    private ExplosionS2CPacket rewriteExplosion(ExplosionS2CPacket packet, Vec3d knockback) {
        return new ExplosionS2CPacket(
            packet.center(),
            packet.radius(),
            packet.blockCount(),
            Optional.of(knockback),
            packet.explosionParticle(),
            packet.explosionSound(),
            packet.blockParticles()
        );
    }

    private void applyJumpReset(Vec3d scaled) {
        if (mc.player == null) return;

        if (mc.player.isOnGround()) mc.player.jump();
        Vec3d current = mc.player.getVelocity();
        double y = Math.max(current.y, scaled.y);
        mc.player.setVelocity(scaled.x, y, scaled.z);
    }

    private void scheduleGrimConfirm() {
        confirmTicks = Math.max(confirmTicks, Math.max(1, grimConfirmDelay.get()));
    }

    private boolean shouldPauseAfterCorrection() {
        if (correctionPause.get() <= 0) return false;
        return switch (correctionPolicy.get()) {
            case Off -> false;
            case GrimCancelOnly -> mode.get() == Mode.GrimCancel;
            case AllModes -> true;
        };
    }

    private boolean usesScaling() {
        return mode.get() == Mode.Scale || mode.get() == Mode.JumpReset;
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
