package eclipse.modules.movement;

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
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class Fly extends Module {
    public enum Mode {
        Vanilla,
        AirWalk,
        Glide,
        GrimSafe,
        PrismSafe
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMotion = settings.createGroup("Motion");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Vanilla uses built-in flying. AirWalk, Glide, GrimSafe, and PrismSafe control velocity directly.")
        .defaultValue(Mode.AirWalk)
        .onChanged(this::applyModeDefaults)
        .build()
    );

    private final Setting<Boolean> autoTakeoff = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-takeoff")
        .description("Jumps automatically when enabling motion fly on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopOnDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-disable")
        .description("Stops carried motion when the module is disabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgMotion.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal movement speed.")
        .defaultValue(0.82)
        .range(0.05, 3.5)
        .sliderRange(0.1, 1.8)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgMotion.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed while jumping or sneaking.")
        .defaultValue(0.34)
        .range(0.0, 1.5)
        .sliderRange(0.0, 0.8)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> idleDrift = sgMotion.add(new DoubleSetting.Builder()
        .name("idle-drift")
        .description("Vertical drift while no up or down key is pressed.")
        .defaultValue(0.0)
        .range(-0.25, 0.25)
        .sliderRange(-0.08, 0.08)
        .decimalPlaces(3)
        .visible(() -> mode.get() != Mode.Vanilla)
        .build()
    );

    private final Setting<Double> glideFall = sgMotion.add(new DoubleSetting.Builder()
        .name("glide-fall")
        .description("Natural fall speed used by Glide mode.")
        .defaultValue(0.045)
        .range(0.0, 0.25)
        .sliderRange(0.0, 0.12)
        .decimalPlaces(3)
        .visible(() -> mode.get() == Mode.Glide)
        .build()
    );

    private final Setting<Double> acceleration = sgMotion.add(new DoubleSetting.Builder()
        .name("acceleration")
        .description("How quickly the fly velocity reaches the target.")
        .defaultValue(0.70)
        .range(0.05, 1.0)
        .sliderRange(0.1, 1.0)
        .decimalPlaces(2)
        .visible(() -> mode.get() != Mode.Vanilla)
        .build()
    );

    private final Setting<Boolean> packetSync = sgMotion.add(new BoolSetting.Builder()
        .name("packet-sync")
        .description("Sends matching movement packets while flying.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Vanilla)
        .build()
    );

    private final Setting<Boolean> positionSync = sgMotion.add(new BoolSetting.Builder()
        .name("position-sync")
        .description("Moves local position directly instead of relying only on velocity.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Vanilla)
        .build()
    );

    private final Setting<Integer> packetsPerTick = sgMotion.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("How many movement packets are sent per tick while flying.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 4)
        .visible(() -> mode.get() != Mode.Vanilla && packetSync.get())
        .build()
    );

    private final Setting<Boolean> antiKick = sgSafety.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Adds a small periodic descent in motion modes.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.Vanilla)
        .build()
    );

    private final Setting<Double> antiKickDrop = sgSafety.add(new DoubleSetting.Builder()
        .name("anti-kick-drop")
        .description("Downward anti-kick pulse strength.")
        .defaultValue(0.032)
        .range(0.0, 0.12)
        .sliderRange(0.0, 0.06)
        .decimalPlaces(3)
        .visible(() -> mode.get() != Mode.Vanilla && antiKick.get())
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to slow down after a server position correction.")
        .defaultValue(8)
        .range(0, 80)
        .sliderRange(0, 30)
        .visible(() -> mode.get() != Mode.Vanilla)
        .build()
    );

    private final Setting<Double> safeHorizontalLimit = sgSafety.add(new DoubleSetting.Builder()
        .name("safe-horizontal-limit")
        .description("Extra horizontal clamp used by safe profiles.")
        .defaultValue(0.34)
        .range(0.05, 1.2)
        .sliderRange(0.1, 0.6)
        .decimalPlaces(2)
        .visible(this::isSafeMode)
        .build()
    );

    private final Setting<Double> safeVerticalLimit = sgSafety.add(new DoubleSetting.Builder()
        .name("safe-vertical-limit")
        .description("Extra vertical clamp used by safe profiles.")
        .defaultValue(0.12)
        .range(0.0, 0.5)
        .sliderRange(0.02, 0.24)
        .decimalPlaces(2)
        .visible(this::isSafeMode)
        .build()
    );

    private int antiKickTicks;
    private int correctionTicks;
    private boolean vanillaEnabled;

    public Fly() {
        super(Eclipse.MOVEMENT, "fly", "General flight module with vanilla and motion-based profiles.");
    }

    @Override
    public void onActivate() {
        antiKickTicks = 0;
        correctionTicks = 0;
        vanillaEnabled = false;

        if (mc.player == null) return;

        if (mode.get() == Mode.Vanilla) enableVanillaFlight(mc.player);
        else if (autoTakeoff.get() && mc.player.isOnGround()) mc.player.jump();
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null) return;

        if (vanillaEnabled && !mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }

        mc.player.getAbilities().setFlySpeed(0.05F);
        if (stopOnDisable.get() && mode.get() != Mode.Vanilla) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x * 0.15, 0.0, velocity.z * 0.15);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.get() == Mode.Vanilla) {
            enableVanillaFlight(mc.player);
            return;
        }

        applyMotionFly(mc.player);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerPositionLookS2CPacket) || mode.get() == Mode.Vanilla) return;
        correctionTicks = correctionPause.get();
    }

    private void enableVanillaFlight(ClientPlayerEntity player) {
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlySpeed((float) (horizontalSpeed.get() * 0.05));
        vanillaEnabled = true;
    }

    private void applyMotionFly(ClientPlayerEntity player) {
        antiKickTicks++;
        if (correctionTicks > 0) correctionTicks--;

        if (autoTakeoff.get() && player.isOnGround() && !mc.options.sneakKey.isPressed()) {
            player.jump();
        }

        Vec3d current = player.getVelocity();
        if (correctionTicks > 0) {
            Vec3d cooled = new Vec3d(current.x * 0.35, Math.min(current.y, 0.0), current.z * 0.35);
            player.setVelocity(cooled);
            player.fallDistance = 0.0F;
            return;
        }

        Vec3d horizontal = PlayerUtils.isMoving()
            ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get())
            : new Vec3d(current.x * 0.82, 0.0, current.z * 0.82);

        double y = idleVertical(player);
        Vec3d target = new Vec3d(horizontal.x, y, horizontal.z);
        double response = acceleration.get();
        if (isSafeMode()) {
            target = applySafeClamp(target);
        }

        Vec3d velocity = new Vec3d(
            current.x + (target.x - current.x) * response,
            current.y + (target.y - current.y) * response,
            current.z + (target.z - current.z) * response
        );
        player.setVelocity(velocity);
        applyPacketSync(player, velocity);
        player.fallDistance = 0.0F;
    }

    private double idleVertical(ClientPlayerEntity player) {
        if (mc.options.jumpKey.isPressed()) return verticalSpeed.get();
        if (mc.options.sneakKey.isPressed()) return -verticalSpeed.get();

        double y = usesGlidePhysics() ? -glideFall.get() : idleDrift.get();
        if (antiKick.get() && !usesGlidePhysics() && antiKickTicks % 20 == 0) y -= antiKickDrop.get();
        if (antiKick.get() && usesGlidePhysics() && antiKickTicks % 16 == 0) y -= antiKickDrop.get() * 0.6;
        if (player.horizontalCollision && y > 0.0) y = 0.0;
        if (correctionTicks > 0 && y > 0.0) y *= 0.35;
        return y;
    }

    private boolean usesGlidePhysics() {
        return mode.get() == Mode.Glide || mode.get() == Mode.PrismSafe;
    }

    private boolean isSafeMode() {
        return mode.get() == Mode.GrimSafe || mode.get() == Mode.PrismSafe;
    }

    private Vec3d applySafeClamp(Vec3d velocity) {
        if (!isSafeMode()) return velocity;

        double x = velocity.x;
        double z = velocity.z;
        double horizontalLimit = Math.min(horizontalSpeed.get(), safeHorizontalLimit.get());
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > horizontalLimit && horizontal > 0.0) {
            double scale = horizontalLimit / horizontal;
            x *= scale;
            z *= scale;
        }

        double verticalLimit = safeVerticalLimit.get();
        double y = Math.max(-verticalLimit, Math.min(verticalLimit, velocity.y));
        return new Vec3d(x, y, z);
    }

    private void applyPacketSync(ClientPlayerEntity player, Vec3d velocity) {
        if (!packetSync.get() || mc.getNetworkHandler() == null) return;

        Vec3d start = new Vec3d(player.getX(), player.getY(), player.getZ());
        int steps = Math.max(1, packetsPerTick.get());
        for (int i = 1; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3d next = start.add(velocity.multiply(progress));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                next.x,
                next.y,
                next.z,
                player.isOnGround(),
                player.horizontalCollision
            ));
        }

        if (positionSync.get()) {
            Vec3d next = start.add(velocity);
            player.setPosition(next.x, next.y, next.z);
        }
    }

    private void applyModeDefaults(Mode selectedMode) {
        switch (selectedMode) {
            case Vanilla -> {
                autoTakeoff.set(false);
                horizontalSpeed.set(1.0);
                verticalSpeed.set(0.32);
            }
            case AirWalk -> {
                autoTakeoff.set(true);
                horizontalSpeed.set(0.36);
                verticalSpeed.set(0.16);
                idleDrift.set(0.0);
                glideFall.set(0.045);
                acceleration.set(0.42);
                packetSync.set(true);
                positionSync.set(false);
                packetsPerTick.set(1);
                antiKick.set(true);
                antiKickDrop.set(0.036);
                correctionPause.set(14);
                safeHorizontalLimit.set(0.34);
                safeVerticalLimit.set(0.12);
            }
            case Glide -> {
                autoTakeoff.set(true);
                horizontalSpeed.set(0.32);
                verticalSpeed.set(0.14);
                idleDrift.set(0.0);
                glideFall.set(0.055);
                acceleration.set(0.36);
                packetSync.set(true);
                positionSync.set(false);
                packetsPerTick.set(1);
                antiKick.set(true);
                antiKickDrop.set(0.032);
                correctionPause.set(14);
                safeHorizontalLimit.set(0.34);
                safeVerticalLimit.set(0.12);
            }
            case GrimSafe -> {
                autoTakeoff.set(true);
                horizontalSpeed.set(0.28);
                verticalSpeed.set(0.08);
                idleDrift.set(-0.030);
                glideFall.set(0.060);
                acceleration.set(0.26);
                packetSync.set(true);
                positionSync.set(false);
                packetsPerTick.set(1);
                antiKick.set(true);
                antiKickDrop.set(0.038);
                correctionPause.set(22);
                safeHorizontalLimit.set(0.28);
                safeVerticalLimit.set(0.08);
            }
            case PrismSafe -> {
                autoTakeoff.set(true);
                horizontalSpeed.set(0.24);
                verticalSpeed.set(0.06);
                idleDrift.set(-0.034);
                glideFall.set(0.065);
                acceleration.set(0.20);
                packetSync.set(true);
                positionSync.set(false);
                packetsPerTick.set(1);
                antiKick.set(true);
                antiKickDrop.set(0.034);
                correctionPause.set(28);
                safeHorizontalLimit.set(0.24);
                safeVerticalLimit.set(0.06);
            }
        }
    }
}
