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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PearlPhase extends Module {
    public enum SequenceMode {
        AfterThrow,
        AfterTeleport,
        Both
    }

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

    private final Setting<Integer> packets = sgPackets.add(new IntSetting.Builder()
        .name("packets")
        .description("Movement packets sent in each phase burst.")
        .defaultValue(14)
        .range(1, 80)
        .sliderRange(1, 36)
        .build()
    );

    private final Setting<Integer> phaseTicks = sgPackets.add(new IntSetting.Builder()
        .name("phase-ticks")
        .description("Ticks to keep pushing into the wall after the trigger.")
        .defaultValue(16)
        .range(1, 80)
        .sliderRange(4, 40)
        .build()
    );

    private final Setting<Integer> afterThrowDelay = sgPackets.add(new IntSetting.Builder()
        .name("after-throw-delay")
        .description("Ticks to wait before the after-throw phase burst starts.")
        .defaultValue(2)
        .range(0, 30)
        .sliderRange(0, 12)
        .build()
    );

    private final Setting<SequenceMode> sequenceMode = sgPackets.add(new EnumSetting.Builder<SequenceMode>()
        .name("sequence-mode")
        .description("When to run the phase burst.")
        .defaultValue(SequenceMode.Both)
        .build()
    );

    private final Setting<Boolean> centerInWallBlock = sgPackets.add(new BoolSetting.Builder()
        .name("center-in-wall-block")
        .description("Targets the center line of the wall block instead of only nudging forward.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> clipDistance = sgPackets.add(new DoubleSetting.Builder()
        .name("clip-distance")
        .description("Forward phase distance when block-center targeting is off, or extra depth after centering.")
        .defaultValue(0.18)
        .range(0.0, 1.5)
        .sliderRange(0.0, 0.8)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> horizontal = sgPackets.add(new DoubleSetting.Builder()
        .name("horizontal")
        .description("Fallback horizontal offset used when no wall-center target is available.")
        .defaultValue(0.12)
        .range(0.0, 0.35)
        .sliderRange(0.0, 0.18)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Double> vertical = sgPackets.add(new DoubleSetting.Builder()
        .name("vertical")
        .description("Final vertical offset applied across each phase burst.")
        .defaultValue(-0.02)
        .range(-0.35, 0.35)
        .sliderRange(-0.16, 0.16)
        .decimalPlaces(3)
        .build()
    );

    private final Setting<Boolean> setLocalPosition = sgPackets.add(new BoolSetting.Builder()
        .name("set-local-position")
        .description("Moves the local player to the deepest sent packet position while phasing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> faceWall = sgPackets.add(new BoolSetting.Builder()
        .name("face-wall")
        .description("Uses packet yaw facing the selected wall direction.")
        .defaultValue(true)
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

    private final Setting<Integer> disableAfter = sgSafety.add(new IntSetting.Builder()
        .name("disable-after")
        .description("Automatically disables the module after this many ticks. Set to 0 to keep it enabled.")
        .defaultValue(60)
        .range(0, 200)
        .sliderRange(0, 120)
        .build()
    );

    private int ticks;
    private int phaseTicksLeft;
    private int phaseDelayTicks;
    private boolean thrown;
    private Direction wallDirection;
    private Direction phaseDirection;
    private BlockPos phaseTargetBlock;

    public PearlPhase() {
        super(Eclipse.CATEGORY, "pearl-phase", "Throws a pearl near a wall and sends a configurable phase packet sequence.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        phaseTicksLeft = 0;
        phaseDelayTicks = 0;
        thrown = false;
        wallDirection = null;
        phaseDirection = null;
        phaseTargetBlock = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        ticks++;
        wallDirection = findWallDirection();

        if (stopVelocity.get() && phaseTicksLeft > 0) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }

        if (phaseDelayTicks > 0) {
            phaseDelayTicks--;
        } else if (phaseTicksLeft > 0) {
            sendPhaseBurst();
            phaseTicksLeft--;
        }

        if (autoThrow.get() && !thrown && ticks >= throwDelay.get()) {
            if (requireWall.get() && wallDirection == null) return;
            throwPearl();
        }

        if (disableAfter.get() > 0 && ticks >= disableAfter.get()) {
            toggle();
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerInteractItemC2SPacket packet)) return;
        if (mc.player == null || !isPearl(packet.getHand())) return;

        thrown = true;
        phaseDirection = wallDirection != null ? wallDirection : Direction.fromHorizontalDegrees(mc.player.getYaw());
        phaseTargetBlock = targetBlockFor(phaseDirection);

        if (sequenceMode.get() == SequenceMode.AfterThrow || sequenceMode.get() == SequenceMode.Both) {
            phaseDelayTicks = afterThrowDelay.get();
            phaseTicksLeft = Math.max(phaseTicksLeft, phaseTicks.get());
            if (phaseDelayTicks == 0) sendPhaseBurst();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerPositionLookS2CPacket) || mc.player == null || !thrown) return;
        if (sequenceMode.get() != SequenceMode.AfterTeleport && sequenceMode.get() != SequenceMode.Both) return;

        wallDirection = findWallDirection();
        phaseDirection = wallDirection != null ? wallDirection : phaseDirection;
        if (phaseTargetBlock == null && phaseDirection != null) phaseTargetBlock = targetBlockFor(phaseDirection);
        phaseDelayTicks = 0;
        phaseTicksLeft = Math.max(phaseTicksLeft, phaseTicks.get());
        sendPhaseBurst();
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

        mc.interactionManager.interactItem(mc.player, hand);
        if (swing.get()) mc.player.swingHand(hand);
        if (swapped && swapBack.get()) InvUtils.swapBack();
    }

    private void sendPhaseBurst() {
        Direction direction = phaseDirection != null ? phaseDirection : (wallDirection != null ? wallDirection : Direction.fromHorizontalDegrees(mc.player.getYaw()));
        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d target = phaseTarget(start, direction);
        float yaw = faceWall.get() ? yawFor(direction) : mc.player.getYaw();
        Vec3d last = target;

        for (int i = 1; i <= packets.get(); i++) {
            double progress = i / (double) packets.get();
            Vec3d next = start.lerp(target, progress).add(0.0, vertical.get() * progress, 0.0);
            last = next;
            if (fullPackets.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    next.x,
                    next.y,
                    next.z,
                    yaw,
                    mc.player.getPitch(),
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

        if (setLocalPosition.get()) {
            mc.player.setPosition(last.x, last.y, last.z);
        }
    }

    private Vec3d phaseTarget(Vec3d start, Direction direction) {
        Vec3d dir = new Vec3d(direction.getOffsetX(), 0.0, direction.getOffsetZ());

        if (centerInWallBlock.get() && phaseTargetBlock != null) {
            BlockPos targetBlock = phaseTargetBlock;
            double x = start.x;
            double z = start.z;

            if (direction.getOffsetX() != 0) x = targetBlock.getX() + 0.5;
            if (direction.getOffsetZ() != 0) z = targetBlock.getZ() + 0.5;

            return new Vec3d(x, start.y, z).add(dir.multiply(clipDistance.get()));
        }

        double fallback = Math.max(horizontal.get() * packets.get(), clipDistance.get());
        return start.add(dir.multiply(fallback));
    }

    private BlockPos targetBlockFor(Direction direction) {
        return mc.player.getBlockPos().offset(direction);
    }

    private float yawFor(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> mc.player.getYaw();
        };
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

    private boolean isPearl(Hand hand) {
        return mc.player.getStackInHand(hand).isOf(Items.ENDER_PEARL);
    }
}
