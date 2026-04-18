package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class EclipseMove extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgJump = settings.createGroup("Jump");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Boolean> groundStrafe = sgGeneral.add(new BoolSetting.Builder()
        .name("ground-strafe")
        .description("Applies a small controlled horizontal speed while moving on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> groundSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("ground-speed")
        .description("Horizontal ground speed. Conservative defaults reduce correction spikes.")
        .defaultValue(0.255)
        .range(0.05, 0.7)
        .sliderRange(0.1, 0.45)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Boolean> airStrafe = sgGeneral.add(new BoolSetting.Builder()
        .name("air-strafe")
        .description("Keeps a configurable amount of horizontal control in air.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> airSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("air-speed")
        .description("Horizontal air speed.")
        .defaultValue(0.120)
        .range(0.05, 0.7)
        .sliderRange(0.1, 0.4)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Boolean> autoSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-sprint")
        .description("Keeps vanilla sprint enabled when Grim-compatible movement is possible.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoJump = sgJump.add(new BoolSetting.Builder()
        .name("auto-jump")
        .description("Jumps when moving on ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> lowHop = sgJump.add(new DoubleSetting.Builder()
        .name("low-hop")
        .description("Optional low-hop Y velocity. Set to 0 to use vanilla jump.")
        .defaultValue(0.0)
        .range(0.0, 0.42)
        .sliderRange(0.0, 0.42)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Boolean> pauseInLiquids = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-in-liquids")
        .description("Stops movement changes in water or lava.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> serverSafe = sgSafety.add(new BoolSetting.Builder()
        .name("server-safe")
        .description("Caps movement to conservative server-friendly values and pauses after server corrections.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to stop applying movement after a server position correction.")
        .defaultValue(20)
        .range(0, 100)
        .sliderRange(0, 60)
        .build()
    );

    private int correctionTicks;

    public EclipseMove() {
        super(Eclipse.CATEGORY, "eclipse-move", "Conservative configurable movement tuning for testing server limits.");
    }

    @Override
    public void onActivate() {
        correctionTicks = 0;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            correctionTicks = correctionPause.get();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (pauseInLiquids.get() && (mc.player.isTouchingWater() || mc.player.isInLava())) return;
        if (serverSafe.get() && mc.player.isGliding()) return;
        if (serverSafe.get() && correctionTicks > 0) {
            correctionTicks--;
            return;
        }
        if (serverSafe.get() && mc.player.horizontalCollision) return;
        if (!PlayerUtils.isMoving()) return;

        boolean onGround = mc.player.isOnGround();
        if (serverSafe.get()) {
            applyGrimMove(onGround);
            return;
        }

        if (autoJump.get() && onGround) {
            if (lowHop.get() > 0.0) {
                Vec3d current = mc.player.getVelocity();
                mc.player.setVelocity(current.x, serverSafe.get() ? Math.min(lowHop.get(), 0.300) : lowHop.get(), current.z);
            } else {
                mc.player.jump();
            }
        }

        if ((onGround && groundStrafe.get()) || (!onGround && airStrafe.get())) {
            double speed = effectiveSpeed(onGround);
            Vec3d horizontal = PlayerUtils.getHorizontalVelocity(speed);
            Vec3d current = mc.player.getVelocity();
            mc.player.setVelocity(horizontal.x, current.y, horizontal.z);
        }
    }

    private void applyGrimMove(boolean onGround) {
        boolean forward = mc.options.forwardKey.isPressed();
        boolean canSprint = autoSprint.get()
            && forward
            && !mc.player.isSneaking()
            && !mc.player.isUsingItem()
            && !mc.player.horizontalCollision
            && !mc.player.isTouchingWater()
            && !mc.player.isInLava();

        if (canSprint && !mc.player.isSprinting()) mc.player.setSprinting(true);
        if (autoJump.get() && onGround && canSprint) mc.player.jump();
    }

    private double effectiveSpeed(boolean onGround) {
        double speed = onGround ? groundSpeed.get() : airSpeed.get();
        if (!serverSafe.get()) return speed;
        return Math.min(speed, onGround ? 0.265 : 0.150);
    }
}
