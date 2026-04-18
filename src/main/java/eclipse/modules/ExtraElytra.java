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

public class ExtraElytra extends Module {
    public enum Mode {
        Grim,
        Legit,
        GroundGlide,
        FakeFly
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMotion = settings.createGroup("Motion");
    private final SettingGroup sgPackets = settings.createGroup("Packets");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Elytra behavior profile. FakeFly keeps your chestplate equipped and simulates elytra-like motion.")
        .defaultValue(Mode.Grim)
        .build()
    );

    private final Setting<Boolean> requireElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("require-elytra")
        .description("Requires a real elytra in the chest slot for Legit and GroundGlide modes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> keepOpen = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-open")
        .description("Refreshes fall-flying state with start flying packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> resetFallDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-fall-distance")
        .description("Resets local fall distance while active.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgMotion.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Target horizontal speed. Keep this conservative while tuning on Grim.")
        .defaultValue(0.32)
        .range(0.05, 2.5)
        .sliderRange(0.1, 1.2)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgMotion.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Up and down speed from jump/sneak.")
        .defaultValue(0.04)
        .range(0.0, 1.5)
        .sliderRange(0.0, 0.6)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> idleFall = sgMotion.add(new DoubleSetting.Builder()
        .name("idle-fall")
        .description("Small downward drift when not ascending or descending.")
        .defaultValue(0.030)
        .range(0.0, 0.25)
        .sliderRange(0.0, 0.08)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> pitchAssist = sgMotion.add(new DoubleSetting.Builder()
        .name("pitch-assist")
        .description("Adds look-direction Y influence, similar to elytra pitch control.")
        .defaultValue(0.08)
        .range(0.0, 1.0)
        .sliderRange(0.0, 0.5)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> groundLift = sgMotion.add(new DoubleSetting.Builder()
        .name("ground-lift")
        .description("Takeoff lift used by GroundGlide and FakeFly while on ground.")
        .defaultValue(0.020)
        .range(0.0, 0.5)
        .sliderRange(0.0, 0.18)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Integer> startInterval = sgPackets.add(new IntSetting.Builder()
        .name("start-interval")
        .description("Ticks between START_FALL_FLYING packets.")
        .defaultValue(20)
        .range(1, 40)
        .sliderRange(1, 16)
        .build()
    );

    private final Setting<Integer> startBurst = sgPackets.add(new IntSetting.Builder()
        .name("start-burst")
        .description("START_FALL_FLYING packets sent per refresh.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Boolean> movementPackets = sgPackets.add(new BoolSetting.Builder()
        .name("movement-packets")
        .description("Sends matching movement packets in FakeFly mode.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoStart = sgPackets.add(new BoolSetting.Builder()
        .name("auto-start")
        .description("Starts real elytra gliding once while falling. Used by the Grim profile.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoFirework = sgPackets.add(new BoolSetting.Builder()
        .name("auto-firework")
        .description("Uses real firework rockets while gliding instead of spoofing movement.")
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

    private final Setting<Double> fireworkMinSpeed = sgMotion.add(new DoubleSetting.Builder()
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
        .description("Forces Grim-compatible real elytra/firework behavior and blocks fake movement packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to stop applying elytra assists after a server position correction.")
        .defaultValue(45)
        .range(0, 160)
        .sliderRange(0, 100)
        .build()
    );

    private int ticks;
    private int correctionTicks;
    private int fireworkTicks;
    private int startTicks;

    public ExtraElytra() {
        super(Eclipse.CATEGORY, "eclipse-elytra", "Elytra fly, ground glide, and chestplate fake-fly tuned for diagnostics.");
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
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        ticks++;
        if (fireworkTicks > 0) fireworkTicks--;
        if (startTicks > 0) startTicks--;

        boolean hasElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        if (serverSafe.get() && correctionTicks > 0) {
            correctionTicks--;
            return;
        }
        if (serverSafe.get() && !hasElytra) return;
        if (mode.get() != Mode.FakeFly && requireElytra.get() && !hasElytra) return;

        if (resetFallDistance.get() && (!serverSafe.get() || mc.player.isGliding())) mc.player.fallDistance = 0.0F;
        if (shouldRefreshFlying(hasElytra)) sendStartFlyingBurst();

        switch (effectiveMode()) {
            case Grim -> applyGrimElytra(hasElytra);
            case Legit -> applyLegitAssist();
            case GroundGlide -> applyGroundGlide();
            case FakeFly -> applyFakeFly();
        }
    }

    private void applyLegitAssist() {
        if (serverSafe.get()) {
            applyGrimElytra(true);
            return;
        }

        if (!mc.player.isGliding()) return;
        Vec3d velocity = elytraVelocity(false);
        mc.player.setVelocity(velocity);
    }

    private void applyGroundGlide() {
        if (serverSafe.get()) {
            applyGrimElytra(true);
            return;
        }

        if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, groundLift.get(), velocity.z);
            sendStartFlyingBurst();
            return;
        }

        if (mc.player.isGliding()) mc.player.setVelocity(elytraVelocity(false));
    }

    private void applyFakeFly() {
        if (serverSafe.get()) {
            applyGrimElytra(true);
            return;
        }

        if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, groundLift.get(), velocity.z);
            sendStartFlyingBurst();
            return;
        }

        Vec3d velocity = elytraVelocity(true);
        mc.player.setVelocity(velocity);

        if (movementPackets.get() && !serverSafe.get()) {
            Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(velocity);
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

    private Vec3d elytraVelocity(boolean fake) {
        Vec3d horizontal = PlayerUtils.isMoving()
            ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get())
            : new Vec3d(mc.player.getVelocity().x * 0.92, 0.0, mc.player.getVelocity().z * 0.92);

        double y;
        if (mc.options.jumpKey.isPressed()) {
            y = verticalSpeed.get();
        } else if (mc.options.sneakKey.isPressed()) {
            y = -verticalSpeed.get();
        } else {
            Vec3d look = mc.player.getRotationVec(1.0F);
            y = Math.max(-idleFall.get(), look.y * pitchAssist.get());
            if (fake && y > verticalSpeed.get()) y = verticalSpeed.get();
        }

        return applyServerSafe(new Vec3d(horizontal.x, y, horizontal.z));
    }

    private Mode effectiveMode() {
        if (serverSafe.get()) return Mode.Grim;
        return mode.get();
    }

    private void applyGrimElytra(boolean hasElytra) {
        if (!hasElytra) return;

        if (autoStart.get() && shouldStartGliding()) {
            sendStartFlyingBurst();
            startTicks = 10;
        }

        if (!mc.player.isGliding()) return;
        if (resetFallDistance.get()) mc.player.fallDistance = 0.0F;
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

    private boolean shouldRefreshFlying(boolean hasElytra) {
        if (!keepOpen.get()) return false;
        if (ticks % startInterval.get() != 0) return false;
        if (!serverSafe.get()) return true;
        return hasElytra && !mc.player.isOnGround() && mc.player.isGliding();
    }

    private Vec3d applyServerSafe(Vec3d velocity) {
        if (!serverSafe.get()) return velocity;

        double x = velocity.x;
        double z = velocity.z;
        double horizontalLimit = mc.player.isGliding() ? 0.360 : 0.220;
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > horizontalLimit && horizontal > 0.0) {
            double scale = horizontalLimit / horizontal;
            x *= scale;
            z *= scale;
        }

        double y = clamp(velocity.y, -0.100, 0.040);
        return new Vec3d(x, y, z);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double horizontalSpeedNow() {
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    private void sendStartFlyingBurst() {
        int repeats = serverSafe.get() ? 1 : startBurst.get();
        for (int i = 0; i < repeats; i++) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }
}
