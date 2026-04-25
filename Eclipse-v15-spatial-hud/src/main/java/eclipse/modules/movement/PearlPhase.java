package eclipse.modules.movement;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PearlPhase extends Module {
    private static PearlPhase INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPackets = settings.createGroup("Packets");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Boolean> autoThrow = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-throw")
        .description("Throws an ender pearl automatically when the module is enabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> requireWall = sgGeneral.add(new BoolSetting.Builder()
        .name("require-wall")
        .description("Only starts when you are touching or facing a solid block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> throwDelay = sgGeneral.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("Ticks to wait after enabling before throwing.")
        .defaultValue(1)
        .range(0, 20)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings the hand after throwing the pearl.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches back to the previous hotbar slot after throwing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> throwPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("throw-pitch")
        .description("Pitch used before throwing the pearl.")
        .defaultValue(80.0)
        .range(-90.0, 90.0)
        .sliderRange(45.0, 90.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> packetPitch = sgPackets.add(new DoubleSetting.Builder()
        .name("packet-pitch")
        .description("Pitch used in full phase movement packets.")
        .defaultValue(80.0)
        .range(-90.0, 90.0)
        .sliderRange(45.0, 90.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> yawOffset = sgPackets.add(new DoubleSetting.Builder()
        .name("yaw-offset")
        .description("Yaw offset added to wall/facing direction in phase packets.")
        .defaultValue(0.0)
        .range(-180.0, 180.0)
        .sliderRange(-45.0, 45.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Integer> packets = sgPackets.add(new IntSetting.Builder()
        .name("packets")
        .description("Amount of movement packets sent after the pearl throw.")
        .defaultValue(8)
        .range(1, 40)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> packetsPerTick = sgPackets.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("How many phase packets are sent per tick while the post-throw sequence is active.")
        .defaultValue(2)
        .range(1, 8)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Double> horizontal = sgPackets.add(new DoubleSetting.Builder()
        .name("horizontal")
        .description("Horizontal offset applied per packet toward the wall.")
        .defaultValue(0.055)
        .range(0.0, 0.35)
        .sliderRange(0.0, 0.18)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> vertical = sgPackets.add(new DoubleSetting.Builder()
        .name("vertical")
        .description("Vertical offset applied per packet.")
        .defaultValue(-0.032)
        .range(-0.35, 0.35)
        .sliderRange(-0.16, 0.16)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Boolean> fullPackets = sgPackets.add(new BoolSetting.Builder()
        .name("full-packets")
        .description("Sends full position and rotation packets instead of position-only packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopVelocity = sgSafety.add(new BoolSetting.Builder()
        .name("stop-velocity")
        .description("Clears player velocity while the phase sequence is running.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cancelVelocityPackets = sgSafety.add(new BoolSetting.Builder()
        .name("cancel-velocity-packets")
        .description("Cancels knockback packets during the phase sequence, without needing Velocity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> collisionBypassTicks = sgSafety.add(new IntSetting.Builder()
        .name("collision-bypass-ticks")
        .description("How long block collisions stay disabled after the pearl throw.")
        .defaultValue(14)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> postThrowHoldTicks = sgSafety.add(new IntSetting.Builder()
        .name("post-throw-hold-ticks")
        .description("Extra ticks to keep the sequence window alive after all phase packets are sent.")
        .defaultValue(8)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> localAssist = sgSafety.add(new BoolSetting.Builder()
        .name("local-assist")
        .description("In singleplayer, keeps nudging the player through the wall while collision bypass is active.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> localAssistSpeed = sgSafety.add(new DoubleSetting.Builder()
        .name("local-assist-speed")
        .description("Horizontal push applied each tick during local phase assist.")
        .defaultValue(0.16)
        .range(0.0, 0.45)
        .sliderRange(0.05, 0.30)
        .decimalPlaces(3)
        .visible(localAssist::get)
        .build()
    );

    private final Setting<Double> localAssistVertical = sgSafety.add(new DoubleSetting.Builder()
        .name("local-assist-vertical")
        .description("Vertical drift applied each tick during local phase assist.")
        .defaultValue(-0.01)
        .range(-0.20, 0.20)
        .sliderRange(-0.08, 0.08)
        .decimalPlaces(3)
        .visible(localAssist::get)
        .build()
    );

    private final Setting<Boolean> continuousLocalPhase = sgSafety.add(new BoolSetting.Builder()
        .name("continuous-local-phase")
        .description("In singleplayer, keeps passing through consecutive solid blocks instead of stopping after the first collision.")
        .defaultValue(true)
        .visible(localAssist::get)
        .build()
    );

    private final Setting<Double> maxContinuousDistance = sgSafety.add(new DoubleSetting.Builder()
        .name("max-continuous-distance")
        .description("Maximum distance for continuous local phase before it shuts down.")
        .defaultValue(6.0)
        .range(1.0, 32.0)
        .sliderRange(2.0, 16.0)
        .decimalPlaces(1)
        .visible(() -> localAssist.get() && continuousLocalPhase.get())
        .build()
    );

    private final Setting<Integer> airExitTicks = sgSafety.add(new IntSetting.Builder()
        .name("air-exit-ticks")
        .description("How many clear ticks in a row are needed before continuous local phase stops.")
        .defaultValue(2)
        .range(1, 8)
        .sliderRange(1, 4)
        .visible(() -> localAssist.get() && continuousLocalPhase.get())
        .build()
    );

    private final Setting<Boolean> holdBypassWhileInside = sgSafety.add(new BoolSetting.Builder()
        .name("hold-bypass-while-inside")
        .description("Keeps the collision bypass window alive a bit longer while the player is still inside solid blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> disableAfter = sgSafety.add(new IntSetting.Builder()
        .name("disable-after")
        .description("Automatically disables the module after this many ticks. Set to 0 to keep it enabled.")
        .defaultValue(0)
        .range(0, 200)
        .sliderRange(0, 120)
        .build()
    );

    private int ticks;
    private int bypassTicks;
    private int pendingPackets;
    private int packetStep;
    private int postThrowTicks;
    private int clearTicks;
    private boolean thrown;
    private Direction wallDirection;
    private Direction phaseDirection;
    private Vec3d phaseStart;

    public PearlPhase() {
        super(Eclipse.MOVEMENT, "pearl-phase", "GrimFolia-style pearl phase with packet, bypass, and optional local tunnel assist controls.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        ticks = 0;
        bypassTicks = 0;
        pendingPackets = 0;
        packetStep = 0;
        postThrowTicks = 0;
        clearTicks = 0;
        thrown = false;
        wallDirection = null;
        phaseDirection = null;
        phaseStart = null;
    }

    @Override
    public void onDeactivate() {
        bypassTicks = 0;
        pendingPackets = 0;
        packetStep = 0;
        postThrowTicks = 0;
        clearTicks = 0;
        phaseDirection = null;
        phaseStart = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        ticks++;
        wallDirection = findWallDirection();

        tickPhaseWindow();

        if (stopVelocity.get() && isSequenceWindowActive()) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }

        if (pendingPackets > 0) sendQueuedPhasePackets();
        else if (postThrowTicks > 0) postThrowTicks--;

        if (isCollisionBypassActive()) {
            tickPhaseAssist();
        }

        if (autoThrow.get() && !thrown && ticks >= throwDelay.get()) {
            if (requireWall.get() && wallDirection == null) return;
            throwPearl();
        }

        if (disableAfter.get() > 0 && ticks >= disableAfter.get()) {
            toggle();
            return;
        }

        if (thrown && !isSequenceWindowActive() && !isCollisionBypassActive()) {
            toggle();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!cancelVelocityPackets.get() || mc.player == null) return;
        if (!isSequenceWindowActive()) return;

        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            event.setCancelled(true);
        } else if (event.packet instanceof ExplosionS2CPacket) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractItemC2SPacket packet)) return;
        if (mc.player == null || !isPearl(packet.getHand())) return;

        thrown = true;
        phaseDirection = wallDirection != null ? wallDirection : Direction.fromHorizontalDegrees(mc.player.getYaw());
        phaseStart = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        pendingPackets = packets.get();
        packetStep = 0;
        postThrowTicks = postThrowHoldTicks.get();
        clearTicks = 0;
        bypassTicks = Math.max(bypassTicks, collisionBypassTicks.get());
    }

    private void throwPearl() {
        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) {
            warning("No ender pearls in hotbar.");
            toggle();
            return;
        }

        Hand hand = pearl.getHand();
        boolean swapped = false;
        if (hand == null) {
            swapped = InvUtils.swap(pearl.slot(), swapBack.get());
            if (!swapped) return;
            hand = Hand.MAIN_HAND;
        }

        Hand finalHand = hand;
        boolean finalSwapped = swapped;
        Rotations.rotate(phaseYaw(), throwPitch.get(), 80, () -> {
            mc.interactionManager.interactItem(mc.player, finalHand);
            if (swing.get()) mc.player.swingHand(finalHand);
            if (finalSwapped && swapBack.get()) InvUtils.swapBack();
        });
    }

    private void sendQueuedPhasePackets() {
        int sends = Math.min(packetsPerTick.get(), pendingPackets);
        for (int i = 0; i < sends; i++) {
            sendPhasePacket(++packetStep);
            pendingPackets--;
        }
    }

    private void sendPhasePacket(int step) {
        Direction direction = phaseDirection != null ? phaseDirection : wallDirection != null ? wallDirection : Direction.fromHorizontalDegrees(mc.player.getYaw());
        Vec3d dir = new Vec3d(direction.getOffsetX(), 0.0, direction.getOffsetZ());
        Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        float yaw = phaseYaw();
        float pitch = packetPitch.get().floatValue();

        Vec3d next = pos.add(dir.multiply(horizontal.get() * step)).add(0.0, vertical.get() * step, 0.0);
        if (fullPackets.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                next.x,
                next.y,
                next.z,
                yaw,
                pitch,
                mc.player.isOnGround(),
                true
            ));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                next.x,
                next.y,
                next.z,
                mc.player.isOnGround(),
                true
            ));
        }
    }

    private void tickPhaseWindow() {
        if (bypassTicks > 0) bypassTicks--;

        if (holdBypassWhileInside.get() && bypassTicks > 0 && isInsideSolid()) {
            bypassTicks = Math.max(bypassTicks, 2);
        }
    }

    private void tickPhaseAssist() {
        if (!canUseLocalAssist()) return;

        Direction direction = wallDirection != null ? wallDirection : Direction.fromHorizontalDegrees(mc.player.getYaw());
        Vec3d push = new Vec3d(direction.getOffsetX(), 0.0, direction.getOffsetZ()).normalize().multiply(localAssistSpeed.get());
        Vec3d current = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d next = current.add(push.x, localAssistVertical.get(), push.z);

        mc.player.setVelocity(0.0, 0.0, 0.0);
        mc.player.setPosition(next.x, next.y, next.z);

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                next.x,
                next.y,
                next.z,
                mc.player.isOnGround(),
                true
            ));
        }

        if (shouldContinuePhase(direction, current)) {
            clearTicks = 0;
            bypassTicks = Math.max(bypassTicks, 2);
        } else {
            clearTicks++;
            if (clearTicks >= airExitTicks.get()) {
                bypassTicks = Math.min(bypassTicks, 1);
            }
        }
    }

    private boolean canUseLocalAssist() {
        return localAssist.get() && mc.isInSingleplayer();
    }

    private Direction findWallDirection() {
        BlockPos feet = mc.player.getBlockPos();
        Direction facing = Direction.fromHorizontalDegrees(mc.player.getYaw());

        if (isSolid(feet.offset(facing)) || isSolid(feet.up().offset(facing))) return facing;

        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (isSolid(feet.offset(direction)) || isSolid(feet.up().offset(direction))) return direction;
        }

        return null;
    }

    private boolean isSolid(BlockPos pos) {
        return !mc.world.getBlockState(pos).isAir() && !mc.world.getBlockState(pos).isReplaceable();
    }

    private boolean isInsideSolid() {
        BlockPos pos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return isSolid(pos) || isSolid(pos.up());
    }

    private boolean shouldKeepContinuousPhase(Direction direction) {
        BlockPos feet = mc.player.getBlockPos();
        BlockPos forward = feet.offset(direction);
        BlockPos forwardUp = forward.up();
        BlockPos forwardTwo = forward.offset(direction);
        BlockPos forwardTwoUp = forwardTwo.up();
        return isInsideSolid()
            || isSolid(forward)
            || isSolid(forwardUp)
            || isSolid(forwardTwo)
            || isSolid(forwardTwoUp);
    }

    private boolean shouldContinuePhase(Direction direction, Vec3d current) {
        if (!continuousLocalPhase.get()) {
            return isInsideSolid()
                || isSolid(mc.player.getBlockPos().offset(direction))
                || isSolid(mc.player.getBlockPos().up().offset(direction));
        }

        if (phaseStart != null && current.distanceTo(phaseStart) >= maxContinuousDistance.get()) {
            return false;
        }

        return shouldKeepContinuousPhase(direction);
    }

    private boolean isPearl(Hand hand) {
        return mc.player.getStackInHand(hand).isOf(Items.ENDER_PEARL);
    }

    private float phaseYaw() {
        Direction direction = phaseDirection != null ? phaseDirection : wallDirection != null ? wallDirection : Direction.fromHorizontalDegrees(mc.player.getYaw());
        return (float) (directionYaw(direction) + yawOffset.get());
    }

    private double directionYaw(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0;
            case WEST -> 90.0;
            case NORTH -> 180.0;
            case EAST -> -90.0;
            default -> mc.player.getYaw();
        };
    }

    public static boolean shouldBypassBlockCollision(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isActive() || !INSTANCE.isCollisionBypassActive() || INSTANCE.mc.player == null || entity == null) {
            return false;
        }

        if (entity == INSTANCE.mc.player) return true;

        return INSTANCE.mc.isInSingleplayer() && entity.getUuid().equals(INSTANCE.mc.player.getUuid());
    }

    private boolean isSequenceWindowActive() {
        return thrown && (pendingPackets > 0 || postThrowTicks > 0 || bypassTicks > 0);
    }

    private boolean isCollisionBypassActive() {
        return bypassTicks > 0;
    }
}

