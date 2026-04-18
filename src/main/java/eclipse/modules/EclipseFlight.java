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
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class EclipseFlight extends Module {
    public enum Mode {
        GrimElytra,
        PacketFly,
        FullFlight,
        Glide,
        Boost,
        Jetpack
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPackets = settings.createGroup("Packets");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Movement profile.")
        .defaultValue(Mode.GrimElytra)
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal movement speed.")
        .defaultValue(0.22)
        .range(0.0, 2.0)
        .sliderRange(0.0, 0.8)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical movement speed.")
        .defaultValue(0.04)
        .range(0.0, 2.0)
        .sliderRange(0.0, 0.8)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> glideFall = sgGeneral.add(new DoubleSetting.Builder()
        .name("glide-fall")
        .description("Downward speed used by Glide.")
        .defaultValue(0.025)
        .range(0.0, 0.5)
        .sliderRange(0.0, 0.16)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> boostMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("boost-multiplier")
        .description("Multiplier used by Boost mode.")
        .defaultValue(1.10)
        .range(1.0, 5.0)
        .sliderRange(1.0, 2.5)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Boolean> antiKick = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Adds a small downward pulse at a configurable interval.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> antiKickInterval = sgGeneral.add(new IntSetting.Builder()
        .name("anti-kick-interval")
        .description("Ticks between anti-kick pulses.")
        .defaultValue(40)
        .range(2, 100)
        .sliderRange(5, 40)
        .build()
    );

    private final Setting<Boolean> sendPackets = sgPackets.add(new BoolSetting.Builder()
        .name("send-packets")
        .description("Sends movement packets matching the local movement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> packetRepeats = sgPackets.add(new IntSetting.Builder()
        .name("packet-repeats")
        .description("Movement packet repeats per tick.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Boolean> autoStartElytra = sgPackets.add(new BoolSetting.Builder()
        .name("auto-start-elytra")
        .description("Starts real elytra gliding while falling in GrimElytra mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoFirework = sgPackets.add(new BoolSetting.Builder()
        .name("auto-firework")
        .description("Uses real firework rockets for GrimElytra flight.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fireworkCooldown = sgPackets.add(new IntSetting.Builder()
        .name("firework-cooldown")
        .description("Ticks between automatic firework uses.")
        .defaultValue(34)
        .range(5, 120)
        .sliderRange(10, 80)
        .build()
    );

    private final Setting<Double> fireworkMinSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("firework-min-speed")
        .description("Uses a firework below this horizontal speed while moving.")
        .defaultValue(0.62)
        .range(0.05, 2.5)
        .sliderRange(0.1, 1.5)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Boolean> rocketSwapBack = sgPackets.add(new BoolSetting.Builder()
        .name("rocket-swap-back")
        .description("Switches back to the previous hotbar slot after using a firework.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> serverSafe = sgSafety.add(new BoolSetting.Builder()
        .name("server-safe")
        .description("Forces Grim-compatible real elytra/firework flight and blocks spoofed movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to stop applying flight after a server position correction.")
        .defaultValue(35)
        .range(0, 120)
        .sliderRange(0, 80)
        .build()
    );

    private int ticks;
    private int correctionTicks;
    private int fireworkTicks;
    private int startTicks;

    public EclipseFlight() {
        super(Eclipse.CATEGORY, "eclipse-flight", "PacketFly, flight, glide, boost, and jetpack style movement profiles for server testing.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        correctionTicks = 0;
        fireworkTicks = 0;
        startTicks = 0;
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

        ticks++;
        if (fireworkTicks > 0) fireworkTicks--;
        if (startTicks > 0) startTicks--;

        if (serverSafe.get() && correctionTicks > 0) {
            correctionTicks--;
            return;
        }

        if (effectiveMode() == Mode.GrimElytra) {
            applyGrimElytraFlight();
            return;
        }

        Vec3d velocity = switch (mode.get()) {
            case GrimElytra -> Vec3d.ZERO;
            case PacketFly -> packetFlyVelocity();
            case FullFlight -> fullFlightVelocity();
            case Glide -> glideVelocity();
            case Boost -> boostVelocity();
            case Jetpack -> jetpackVelocity();
        };

        if (antiKick.get() && ticks % antiKickInterval.get() == 0) {
            velocity = new Vec3d(velocity.x, Math.min(velocity.y, -0.04), velocity.z);
        }

        velocity = applyServerSafe(velocity);

        mc.player.setVelocity(velocity);
        if (sendPackets.get() && !serverSafe.get()) sendMovementPackets(velocity);
    }

    private Vec3d packetFlyVelocity() {
        Vec3d horizontal = PlayerUtils.isMoving() ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get()) : Vec3d.ZERO;
        return new Vec3d(horizontal.x, verticalInput(), horizontal.z);
    }

    private Vec3d fullFlightVelocity() {
        Vec3d horizontal = PlayerUtils.isMoving() ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get()) : Vec3d.ZERO;
        return new Vec3d(horizontal.x, verticalInput(), horizontal.z);
    }

    private Vec3d glideVelocity() {
        Vec3d horizontal = PlayerUtils.isMoving() ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get()) : Vec3d.ZERO;
        double y = verticalInput();
        if (y == 0.0) y = -glideFall.get();
        return new Vec3d(horizontal.x, y, horizontal.z);
    }

    private Vec3d boostVelocity() {
        Vec3d current = mc.player.getVelocity();
        Vec3d horizontal = PlayerUtils.isMoving()
            ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get() * boostMultiplier.get())
            : new Vec3d(current.x * boostMultiplier.get(), 0.0, current.z * boostMultiplier.get());
        return new Vec3d(horizontal.x, current.y, horizontal.z);
    }

    private Vec3d jetpackVelocity() {
        Vec3d current = mc.player.getVelocity();
        Vec3d horizontal = PlayerUtils.isMoving() ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get()) : new Vec3d(current.x, 0.0, current.z);
        double y = mc.options.jumpKey.isPressed() ? verticalSpeed.get() : current.y;
        return new Vec3d(horizontal.x, y, horizontal.z);
    }

    private double verticalInput() {
        double speed = serverSafe.get() ? Math.min(verticalSpeed.get(), 0.04) : verticalSpeed.get();
        if (mc.options.jumpKey.isPressed()) return speed;
        if (mc.options.sneakKey.isPressed()) return -speed;
        return 0.0;
    }

    private Vec3d applyServerSafe(Vec3d velocity) {
        if (!serverSafe.get()) return velocity;

        double x = velocity.x;
        double z = velocity.z;
        if (mc.player.horizontalCollision) {
            x = 0.0;
            z = 0.0;
        }

        double horizontalLimit = mc.player.isOnGround() ? 0.240 : 0.220;
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > horizontalLimit && horizontal > 0.0) {
            double scale = horizontalLimit / horizontal;
            x *= scale;
            z *= scale;
        }

        double y = clamp(velocity.y, -0.080, 0.040);
        if (mc.player.isOnGround() && y < 0.0) y = 0.0;
        return new Vec3d(x, y, z);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Mode effectiveMode() {
        return serverSafe.get() ? Mode.GrimElytra : mode.get();
    }

    private void applyGrimElytraFlight() {
        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return;

        if (autoStartElytra.get() && shouldStartGliding()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            startTicks = 10;
        }

        if (!mc.player.isGliding()) return;
        if (autoFirework.get() && shouldUseFirework()) {
            useFirework();
            fireworkTicks = fireworkCooldown.get();
        }
    }

    private boolean shouldStartGliding() {
        return startTicks <= 0
            && !mc.player.isGliding()
            && !mc.player.isOnGround()
            && mc.player.getVelocity().y < -0.070;
    }

    private boolean shouldUseFirework() {
        if (fireworkTicks > 0) return false;
        if (mc.options.jumpKey.isPressed()) return true;
        return PlayerUtils.isMoving() && horizontalSpeedNow() < fireworkMinSpeed.get();
    }

    private void useFirework() {
        if (mc.interactionManager == null) return;

        FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!firework.found()) return;

        Hand hand = firework.getHand();
        boolean swapped = false;
        if (hand == null) {
            swapped = InvUtils.swap(firework.slot(), rocketSwapBack.get());
            if (!swapped) return;
            hand = Hand.MAIN_HAND;
        }

        mc.interactionManager.interactItem(mc.player, hand);
        if (swapped && rocketSwapBack.get()) InvUtils.swapBack();
    }

    private double horizontalSpeedNow() {
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    private void sendMovementPackets(Vec3d velocity) {
        Vec3d base = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        for (int i = 1; i <= packetRepeats.get(); i++) {
            Vec3d pos = base.add(velocity.multiply(i));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                pos.x,
                pos.y,
                pos.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                false,
                mc.player.horizontalCollision
            ));
        }
    }
}
