package eclipse.modules.movement;

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
        Prism,
        Legit,
        GroundGlide,
        FakeFly
    }

    public enum FlightState {
        Idle,
        WaitingForTakeoff,
        TakeoffStart,
        ElytraEngaged,
        SustainedFlight,
        Recovery
    }

    public enum CorrectionRecovery {
        Pause,
        RetryTakeoff
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMotion = settings.createGroup("Motion");
    private final SettingGroup sgPackets = settings.createGroup("Packets");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Elytra behavior profile. Legit fully controls real elytra velocity; Grim is conservative.")
        .defaultValue(Mode.Legit)
        .onChanged(this::applyModeDefaults)
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
        .defaultValue(true)
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
        .defaultValue(1.15)
        .range(0.05, 2.5)
        .sliderRange(0.1, 1.2)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgMotion.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Up and down speed from jump/sneak.")
        .defaultValue(0.42)
        .range(0.0, 1.5)
        .sliderRange(0.0, 0.6)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> idleFall = sgMotion.add(new DoubleSetting.Builder()
        .name("idle-fall")
        .description("Small downward drift when not ascending or descending.")
        .defaultValue(0.0)
        .range(0.0, 0.25)
        .sliderRange(0.0, 0.08)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> pitchAssist = sgMotion.add(new DoubleSetting.Builder()
        .name("pitch-assist")
        .description("Adds look-direction Y influence, similar to elytra pitch control.")
        .defaultValue(0.18)
        .range(0.0, 1.0)
        .sliderRange(0.0, 0.5)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> groundLift = sgMotion.add(new DoubleSetting.Builder()
        .name("ground-lift")
        .description("Takeoff lift used by GroundGlide and FakeFly while on ground.")
        .defaultValue(0.42)
        .range(0.0, 0.5)
        .sliderRange(0.0, 0.18)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Integer> startInterval = sgPackets.add(new IntSetting.Builder()
        .name("start-interval")
        .description("Ticks between START_FALL_FLYING packets.")
        .defaultValue(3)
        .range(1, 40)
        .sliderRange(1, 16)
        .build()
    );

    private final Setting<Integer> startBurst = sgPackets.add(new IntSetting.Builder()
        .name("start-burst")
        .description("START_FALL_FLYING packets sent per refresh.")
        .defaultValue(2)
        .range(1, 8)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Boolean> movementPackets = sgPackets.add(new BoolSetting.Builder()
        .name("movement-packets")
        .description("Sends matching movement packets in FakeFly mode.")
        .defaultValue(true)
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
        .defaultValue(false)
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

    private final Setting<Integer> rocketSlot = sgPackets.add(new IntSetting.Builder()
        .name("rocket-slot")
        .description("Hotbar slot used for rockets moved from inventory. 1-9.")
        .defaultValue(9)
        .range(1, 9)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Boolean> moveRocketsToSlot = sgPackets.add(new BoolSetting.Builder()
        .name("move-rockets-to-slot")
        .description("Moves rockets from inventory into Rocket Slot when the hotbar has none.")
        .defaultValue(true)
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

    private final Setting<Double> controlResponse = sgMotion.add(new DoubleSetting.Builder()
        .name("control-response")
        .description("How quickly controlled flight velocity moves toward the target velocity.")
        .defaultValue(1.0)
        .range(0.05, 1.0)
        .sliderRange(0.1, 1.0)
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
        .defaultValue(false)
        .build()
    );

    private final Setting<CorrectionRecovery> correctionRecovery = sgSafety.add(new EnumSetting.Builder<CorrectionRecovery>()
        .name("correction-recovery")
        .description("How ElytraFly recovers after server position corrections.")
        .defaultValue(CorrectionRecovery.RetryTakeoff)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to stop applying elytra assists after a server position correction.")
        .defaultValue(10)
        .range(0, 160)
        .sliderRange(0, 100)
        .build()
    );

    private int ticks;
    private int correctionTicks;
    private int fireworkTicks;
    private int startTicks;
    private int airborneTicks;
    private int glidingTicks;
    private int staleGlideTicks;
    private FlightState state = FlightState.Idle;

    public ExtraElytra() {
        super(Eclipse.MOVEMENT, "eclipse-elytra", "Elytra fly, ground glide, and chestplate fake-fly tuned for diagnostics.");
    }

    @Override
    public void onActivate() {
        migrateOldDefaults();
        resetState(FlightState.Idle);
    }

    @Override
    public void onDeactivate() {
        resetState(FlightState.Idle);
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        resetState(FlightState.Idle);
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        resetState(FlightState.Idle);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            correctionTicks = correctionPause.get();
            state = FlightState.Recovery;
            glidingTicks = 0;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        ticks++;
        if (fireworkTicks > 0) fireworkTicks--;
        if (startTicks > 0) startTicks--;

        boolean hasElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        if (!mc.player.isAlive()) {
            resetState(FlightState.Idle);
            return;
        }

        updateAirState();

        if (correctionTicks > 0) {
            correctionTicks--;
            if (correctionTicks == 0 && correctionRecovery.get() == CorrectionRecovery.RetryTakeoff) {
                state = mc.player.isGliding() ? FlightState.ElytraEngaged : FlightState.WaitingForTakeoff;
            }
            return;
        }

        if (serverSafe.get() && !hasElytra) {
            state = FlightState.Idle;
            return;
        }
        if (mode.get() != Mode.FakeFly && requireElytra.get() && !hasElytra) return;

        updateFlightState(hasElytra);
        if (resetFallDistance.get() && (!isStrictServerMode() || mc.player.isGliding() || mode.get() == Mode.FakeFly)) mc.player.fallDistance = 0.0F;
        if (shouldRefreshFlying(hasElytra)) sendStartFlyingBurst();
        handleTakeoff(hasElytra);

        switch (mode.get()) {
            case Grim -> applyGrimElytra(hasElytra);
            case Prism -> applyPrismElytra(hasElytra);
            case Legit -> applyLegitAssist();
            case GroundGlide -> applyGroundGlide();
            case FakeFly -> applyFakeFly();
        }
    }

    private void applyLegitAssist() {
        if (mc.player.isGliding()) {
            applyControlledFlight(false);
        } else if (!mc.player.isOnGround() && (state == FlightState.TakeoffStart || state == FlightState.WaitingForTakeoff)) {
            applyPreglideControl();
        }
    }

    private void applyGroundGlide() {
        if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, groundLift.get(), velocity.z);
            sendStartFlyingBurst();
            state = FlightState.TakeoffStart;
            return;
        }

        if (mc.player.isGliding()) applyControlledFlight(false);
    }

    private void applyFakeFly() {
        if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, groundLift.get(), velocity.z);
            sendStartFlyingBurst();
            state = FlightState.TakeoffStart;
            return;
        }

        if (serverSafe.get() && !mc.player.isGliding()) return;
        Vec3d velocity = elytraVelocity(true);
        mc.player.setVelocity(velocity);

        if (movementPackets.get() && !isStrictServerMode()) {
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

    private void applyGrimElytra(boolean hasElytra) {
        if (!hasElytra) return;

        if (autoStart.get() && shouldStartGliding()) {
            sendStartFlyingBurst();
            startTicks = 10;
            state = FlightState.TakeoffStart;
        }

        if (!mc.player.isGliding()) return;
        if (resetFallDistance.get()) mc.player.fallDistance = 0.0F;
        applyControlledFlight(false);
        if (autoFirework.get() && shouldUseFirework()) {
            useFirework();
            fireworkTicks = fireworkCooldown.get();
        }
    }

    private void applyPrismElytra(boolean hasElytra) {
        if (!hasElytra) return;

        if (shouldStartGliding()) {
            sendStartFlyingBurst();
            startTicks = 8;
            state = FlightState.TakeoffStart;
        }

        if (!mc.player.isGliding()) return;

        if (mc.player.horizontalCollision || mc.player.verticalCollision) {
            staleGlideTicks = 0;
            return;
        }

        Vec3d target = prismVelocity();
        Vec3d current = mc.player.getVelocity();
        double response = 0.35;
        mc.player.setVelocity(new Vec3d(
            current.x + (target.x - current.x) * response,
            current.y + (target.y - current.y) * 0.25,
            current.z + (target.z - current.z) * response
        ));

        double horizontalSpeed = horizontalSpeedNow();
        if (PlayerUtils.isMoving() && horizontalSpeed < 0.42 && shouldUsePrismFirework()) {
            if (useFirework()) fireworkTicks = Math.max(fireworkCooldown.get(), 42);
        }

        if (horizontalSpeed < 0.08) staleGlideTicks++;
        else staleGlideTicks = 0;

        if (staleGlideTicks >= 16 && !mc.player.isOnGround() && startTicks <= 0) {
            sendStartFlyingBurst();
            startTicks = 12;
            staleGlideTicks = 0;
        }
    }

    private boolean shouldStartGliding() {
        return startTicks <= 0
            && !mc.player.isGliding()
            && !mc.player.isOnGround()
            && mc.player.getVelocity().y < 0.050;
    }

    private boolean shouldUseFirework() {
        if (fireworkTicks > 0) return false;
        if (mc.options.jumpKey.isPressed()) return true;
        return PlayerUtils.isMoving() && horizontalSpeedNow() < fireworkMinSpeed.get();
    }

    private boolean shouldUsePrismFirework() {
        if (fireworkTicks > 0 || !mc.player.isGliding()) return false;
        if (mc.player.horizontalCollision || mc.player.verticalCollision) return false;
        if (mc.options.jumpKey.isPressed()) return true;
        return PlayerUtils.isMoving() && horizontalSpeedNow() < Math.min(0.52, fireworkMinSpeed.get());
    }

    private void handleTakeoff(boolean hasElytra) {
        if (!hasElytra) return;

        if (mc.options.jumpKey.isPressed() && mc.player.isOnGround()) {
            Vec3d horizontal = PlayerUtils.isMoving()
                ? PlayerUtils.getHorizontalVelocity(horizontalSpeed.get())
                : new Vec3d(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
            mc.player.setVelocity(horizontal.x, groundLift.get(), horizontal.z);
            mc.player.jump();
            sendStartFlyingBurst();
            startTicks = 2;
            state = FlightState.TakeoffStart;
            return;
        }

        if (autoStart.get() && startTicks <= 0 && !mc.player.isGliding() && !mc.player.isOnGround() && mc.player.getVelocity().y < 0.05) {
            sendStartFlyingBurst();
            startTicks = mode.get() == Mode.Prism ? 8 : 3;
            state = FlightState.TakeoffStart;
        }

        if (mc.options.jumpKey.isPressed() && mc.player.isGliding() && fireworkTicks <= 0) {
            if (useFirework()) fireworkTicks = fireworkCooldown.get();
        }
    }

    private boolean useFirework() {
        if (mc.interactionManager == null) return false;

        FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!firework.found() && moveRocketsToSlot.get()) {
            moveRocketToHotbar();
            firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        }

        if (!firework.found()) return false;

        Hand hand = firework.getHand();
        boolean swapped = false;
        if (hand == null) {
            swapped = InvUtils.swap(firework.slot(), rocketSwapBack.get());
            if (!swapped) return false;
            hand = Hand.MAIN_HAND;
        }

        mc.interactionManager.interactItem(mc.player, hand);
        if (swapped && rocketSwapBack.get()) InvUtils.swapBack();
        return true;
    }

    private void updateAirState() {
        if (mc.player.isOnGround()) {
            airborneTicks = 0;
            glidingTicks = 0;
            if (state != FlightState.TakeoffStart) state = FlightState.Idle;
            return;
        }

        airborneTicks++;
        if (mc.player.isGliding()) glidingTicks++;
        else glidingTicks = 0;
    }

    private void updateFlightState(boolean hasElytra) {
        if (mode != null && mode.get() == Mode.FakeFly && !serverSafe.get()) {
            if (mc.player.isOnGround()) {
                if (mc.options.jumpKey.isPressed()) state = FlightState.TakeoffStart;
                else state = FlightState.Idle;
            } else if (airborneTicks > 1) {
                state = FlightState.SustainedFlight;
            }
            return;
        }

        if (!hasElytra) {
            state = FlightState.Idle;
            return;
        }

        if (mc.player.isGliding()) {
            state = glidingTicks < 4 ? FlightState.ElytraEngaged : FlightState.SustainedFlight;
            return;
        }

        if (mc.player.isOnGround()) {
            state = mc.options.jumpKey.isPressed() ? FlightState.WaitingForTakeoff : FlightState.Idle;
        } else if (autoStart.get() && airborneTicks >= (mode.get() == Mode.Prism ? 4 : 2)) {
            state = FlightState.WaitingForTakeoff;
        }
    }

    private void applyControlledFlight(boolean fake) {
        Vec3d target = elytraVelocity(fake);
        Vec3d current = mc.player.getVelocity();
        double response = controlResponse.get();
        Vec3d velocity = new Vec3d(
            current.x + (target.x - current.x) * response,
            current.y + (target.y - current.y) * response,
            current.z + (target.z - current.z) * response
        );
        mc.player.setVelocity(applyServerSafe(velocity));
        if (state == FlightState.ElytraEngaged && glidingTicks >= 4) state = FlightState.SustainedFlight;
    }

    private void applyPreglideControl() {
        Vec3d target = elytraVelocity(false);
        Vec3d current = mc.player.getVelocity();
        mc.player.setVelocity(target.x, Math.max(current.y, target.y), target.z);
    }

    private void moveRocketToHotbar() {
        FindItemResult rocket = InvUtils.find(Items.FIREWORK_ROCKET);
        if (!rocket.found() || rocket.isHotbar() || rocket.isOffhand()) return;

        int slot = rocketSlot.get() - 1;
        InvUtils.move().from(rocket.slot()).toHotbar(slot);
    }

    private boolean shouldRefreshFlying(boolean hasElytra) {
        if (!keepOpen.get()) return false;
        if (mode.get() == Mode.Prism) {
            if (!hasElytra || mc.player.isOnGround() || !mc.player.isGliding()) return false;
            return staleGlideTicks >= 12 && startTicks <= 0;
        }

        if (ticks % startInterval.get() != 0) return false;
        if (!isStrictServerMode()) return !mc.player.isOnGround() || mode.get() == Mode.FakeFly;
        return hasElytra && !mc.player.isOnGround() && mc.player.isGliding();
    }

    private Vec3d applyServerSafe(Vec3d velocity) {
        if (!isStrictServerMode()) return velocity;

        double x = velocity.x;
        double z = velocity.z;
        double horizontalLimit = mode.get() == Mode.Prism ? Math.min(0.78, Math.max(0.45, horizontalSpeed.get())) : Math.max(0.90, horizontalSpeed.get());
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > horizontalLimit && horizontal > 0.0) {
            double scale = horizontalLimit / horizontal;
            x *= scale;
            z *= scale;
        }

        double verticalLimit = mode.get() == Mode.Prism ? 0.28 : Math.max(0.25, verticalSpeed.get());
        double y = clamp(velocity.y, -verticalLimit, verticalLimit);
        return new Vec3d(x, y, z);
    }

    private void migrateOldDefaults() {
        if (horizontalSpeed.get() <= 0.36) horizontalSpeed.set(1.15);
        if (verticalSpeed.get() <= 0.05) verticalSpeed.set(0.42);
        if (controlResponse.get() < 0.90) controlResponse.set(1.0);
        if (idleFall.get() > 0.0 && idleFall.get() <= 0.03) idleFall.set(0.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double horizontalSpeedNow() {
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    private void sendStartFlyingBurst() {
        int repeats = mode.get() == Mode.Prism ? 1 : (serverSafe.get() ? 1 : startBurst.get());
        for (int i = 0; i < repeats; i++) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    private boolean isStrictServerMode() {
        return serverSafe.get() || mode.get() == Mode.Prism;
    }

    private Vec3d prismVelocity() {
        double horizontalTarget = clamp(horizontalSpeed.get(), 0.32, 0.78);
        Vec3d horizontal = PlayerUtils.isMoving()
            ? PlayerUtils.getHorizontalVelocity(horizontalTarget)
            : new Vec3d(mc.player.getVelocity().x * 0.985, 0.0, mc.player.getVelocity().z * 0.985);

        double y = -0.035;
        if (mc.options.jumpKey.isPressed()) y = 0.020;
        else if (mc.options.sneakKey.isPressed()) y = -0.090;
        else if (mc.player.getPitch() < -12.0F) y = 0.010;
        else if (mc.player.getPitch() > 18.0F) y = -0.055;

        return applyServerSafe(new Vec3d(horizontal.x, y, horizontal.z));
    }

    private void applyModeDefaults(Mode selectedMode) {
        switch (selectedMode) {
            case Prism -> {
                requireElytra.set(true);
                keepOpen.set(true);
                resetFallDistance.set(true);
                horizontalSpeed.set(0.68);
                verticalSpeed.set(0.20);
                idleFall.set(0.0);
                pitchAssist.set(0.0);
                groundLift.set(0.42);
                startInterval.set(8);
                startBurst.set(1);
                movementPackets.set(false);
                autoStart.set(true);
                autoFirework.set(true);
                fireworkCooldown.set(42);
                fireworkMinSpeed.set(0.46);
                controlResponse.set(0.35);
                rocketSwapBack.set(true);
                serverSafe.set(true);
                correctionRecovery.set(CorrectionRecovery.RetryTakeoff);
                correctionPause.set(18);
            }
            case Grim -> {
                requireElytra.set(true);
                keepOpen.set(true);
                resetFallDistance.set(true);
                horizontalSpeed.set(0.95);
                verticalSpeed.set(0.30);
                idleFall.set(0.01);
                pitchAssist.set(0.08);
                groundLift.set(0.42);
                startInterval.set(4);
                startBurst.set(1);
                movementPackets.set(false);
                autoStart.set(true);
                autoFirework.set(true);
                fireworkCooldown.set(34);
                fireworkMinSpeed.set(0.58);
                controlResponse.set(0.55);
                rocketSwapBack.set(true);
                serverSafe.set(true);
                correctionRecovery.set(CorrectionRecovery.RetryTakeoff);
                correctionPause.set(14);
            }
            case Legit -> {
                requireElytra.set(true);
                keepOpen.set(true);
                resetFallDistance.set(true);
                horizontalSpeed.set(1.15);
                verticalSpeed.set(0.42);
                idleFall.set(0.0);
                pitchAssist.set(0.18);
                groundLift.set(0.42);
                startInterval.set(3);
                startBurst.set(2);
                movementPackets.set(false);
                autoStart.set(true);
                autoFirework.set(false);
                fireworkCooldown.set(34);
                fireworkMinSpeed.set(0.62);
                controlResponse.set(1.0);
                rocketSwapBack.set(true);
                serverSafe.set(false);
                correctionRecovery.set(CorrectionRecovery.RetryTakeoff);
                correctionPause.set(10);
            }
            case GroundGlide -> {
                requireElytra.set(true);
                keepOpen.set(true);
                resetFallDistance.set(true);
                horizontalSpeed.set(0.78);
                verticalSpeed.set(0.24);
                idleFall.set(0.02);
                pitchAssist.set(0.10);
                groundLift.set(0.42);
                startInterval.set(4);
                startBurst.set(1);
                movementPackets.set(false);
                autoStart.set(true);
                autoFirework.set(false);
                fireworkCooldown.set(34);
                fireworkMinSpeed.set(0.55);
                controlResponse.set(0.65);
                rocketSwapBack.set(true);
                serverSafe.set(true);
                correctionRecovery.set(CorrectionRecovery.RetryTakeoff);
                correctionPause.set(12);
            }
            case FakeFly -> {
                requireElytra.set(false);
                keepOpen.set(true);
                resetFallDistance.set(true);
                horizontalSpeed.set(0.75);
                verticalSpeed.set(0.35);
                idleFall.set(0.0);
                pitchAssist.set(0.12);
                groundLift.set(0.42);
                startInterval.set(3);
                startBurst.set(2);
                movementPackets.set(true);
                autoStart.set(true);
                autoFirework.set(false);
                fireworkCooldown.set(34);
                fireworkMinSpeed.set(0.62);
                controlResponse.set(0.85);
                rocketSwapBack.set(true);
                serverSafe.set(false);
                correctionRecovery.set(CorrectionRecovery.RetryTakeoff);
                correctionPause.set(10);
            }
        }
    }

    private void resetState(FlightState nextState) {
        ticks = 0;
        correctionTicks = 0;
        fireworkTicks = 0;
        startTicks = 0;
        airborneTicks = 0;
        glidingTicks = 0;
        staleGlideTicks = 0;
        state = nextState;
    }
}

