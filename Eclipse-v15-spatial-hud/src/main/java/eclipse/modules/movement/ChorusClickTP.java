package eclipse.modules.movement;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ChorusClickTP extends Module {
    public enum ClickButton {
        Left,
        Right,
        Middle
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTeleport = settings.createGroup("Teleport");

    private final Setting<ClickButton> clickButton = sgGeneral.add(new EnumSetting.Builder<ClickButton>()
        .name("click-button")
        .description("Mouse button used to save the click-tp target.")
        .defaultValue(ClickButton.Middle)
        .build()
    );

    private final Setting<Boolean> cancelClick = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-click")
        .description("Cancels the selected mouse click after setting the target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Disables after one chorus control teleport.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> captureOnUse = sgGeneral.add(new BoolSetting.Builder()
        .name("capture-on-use")
        .description("Captures the current crosshair target automatically when chorus use starts.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxDistance = sgTeleport.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance to the saved click-tp target.")
        .defaultValue(24.0)
        .range(1.0, 128.0)
        .sliderRange(4.0, 48.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> clampDistance = sgTeleport.add(new BoolSetting.Builder()
        .name("clamp-distance")
        .description("Clamps the target to Max Distance instead of failing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> packets = sgTeleport.add(new IntSetting.Builder()
        .name("packets")
        .description("Movement packets used to step toward the target.")
        .defaultValue(6)
        .range(1, 32)
        .sliderRange(2, 12)
        .build()
    );

    private final Setting<Double> fallbackLookDistance = sgTeleport.add(new DoubleSetting.Builder()
        .name("fallback-look-distance")
        .description("Distance used straight along the look direction if there is no block target.")
        .defaultValue(6.0)
        .range(1.0, 32.0)
        .sliderRange(2.0, 12.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> exactHit = sgTeleport.add(new BoolSetting.Builder()
        .name("exact-hit")
        .description("Uses the exact clicked point instead of the block top center.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> yOffset = sgTeleport.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("Vertical offset added to the saved target.")
        .defaultValue(0.0)
        .range(-2.0, 3.0)
        .sliderRange(-1.0, 1.5)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Boolean> setPosition = sgTeleport.add(new BoolSetting.Builder()
        .name("set-position")
        .description("Updates local position after sending click-tp packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> teleportDelay = sgTeleport.add(new IntSetting.Builder()
        .name("teleport-delay")
        .description("Ticks to wait after chorus use finishes before applying click-tp.")
        .defaultValue(2)
        .range(0, 20)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Boolean> safePackets = sgTeleport.add(new BoolSetting.Builder()
        .name("safe-packets")
        .description("Uses position-only packets for a quieter chorus click-tp.")
        .defaultValue(true)
        .build()
    );

    private Vec3d targetPos;
    private boolean usingChorus;
    private boolean chorusPrimed;
    private int pendingTeleportTicks;

    public ChorusClickTP() {
        super(Eclipse.MOVEMENT, "chorus-clicktp", "Controls chorus teleports by routing them into a saved click-tp target.");
    }

    @Override
    public void onActivate() {
        usingChorus = false;
        chorusPrimed = false;
        pendingTeleportTicks = -1;
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.action != KeyAction.Press || event.button() != toGlfw(clickButton.get())) return;
        if (mc.currentScreen != null || mc.player == null || mc.world == null) return;

        Vec3d clicked = currentTarget();
        if (clicked == null) return;

        targetPos = clicked.add(0.0, yOffset.get(), 0.0);
        if (cancelClick.get()) event.cancel();
        info("Saved chorus target at " + fmt(targetPos.x) + ", " + fmt(targetPos.y) + ", " + fmt(targetPos.z));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        boolean nowUsingChorus = isUsingChorus();
        if (nowUsingChorus) {
            usingChorus = true;
            if (captureOnUse.get()) {
                Vec3d fallbackTarget = resolveTargetFromCrosshair();
                if (fallbackTarget != null) targetPos = fallbackTarget.add(0.0, yOffset.get(), 0.0);
            }
            chorusPrimed = targetPos != null;
            return;
        }

        if (usingChorus) {
            usingChorus = false;
            if (chorusPrimed && targetPos != null) pendingTeleportTicks = teleportDelay.get();
            chorusPrimed = false;
        }

        if (pendingTeleportTicks >= 0) {
            if (pendingTeleportTicks == 0) {
                pendingTeleportTicks = -1;
                if (targetPos != null) teleportToTarget();
            } else {
                pendingTeleportTicks--;
            }
        }
    }

    private void teleportToTarget() {
        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d end = targetPos;
        double distance = start.distanceTo(end);
        if (distance > maxDistance.get() && clampDistance.get()) {
            Vec3d delta = end.subtract(start).normalize().multiply(maxDistance.get());
            end = start.add(delta);
            distance = start.distanceTo(end);
        }

        if (distance > maxDistance.get()) {
            warning("Saved target is too far for click-tp.");
            return;
        }

        int steps = Math.max(1, packets.get());
        for (int i = 1; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3d next = lerp(start, end, progress);
            if (safePackets.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    next.x,
                    next.y,
                    next.z,
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                ));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    next.x,
                    next.y,
                    next.z,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                ));
            }
        }

        if (setPosition.get()) {
            mc.player.setPosition(end.x, end.y, end.z);
            mc.player.setVelocity(0.0, 0.0, 0.0);
            mc.player.fallDistance = 0.0F;
        }

        targetPos = end;
        info("Chorus click-tp -> " + fmt(end.x) + ", " + fmt(end.y) + ", " + fmt(end.z));
        if (autoDisable.get()) toggle();
    }

    private boolean isUsingChorus() {
        if (!mc.player.isUsingItem()) return false;
        ItemStack stack = mc.player.getActiveItem();
        return stack != null && stack.isOf(Items.CHORUS_FRUIT);
    }

    private Vec3d currentTarget() {
        HitResult target = mc.crosshairTarget;
        if (!(target instanceof BlockHitResult blockHit)) return null;

        if (exactHit.get()) return blockHit.getPos();

        BlockPos pos = blockHit.getBlockPos();
        return Vec3d.ofBottomCenter(pos.up());
    }

    private Vec3d resolveTargetFromCrosshair() {
        Vec3d clicked = currentTarget();
        if (clicked != null) return clicked;

        Vec3d look = mc.player.getRotationVec(1.0F);
        return new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ())
            .add(look.multiply(fallbackLookDistance.get()));
    }

    private Vec3d lerp(Vec3d start, Vec3d end, double delta) {
        return new Vec3d(
            start.x + (end.x - start.x) * delta,
            start.y + (end.y - start.y) * delta,
            start.z + (end.z - start.z) * delta
        );
    }

    private int toGlfw(ClickButton button) {
        return switch (button) {
            case Left -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case Right -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case Middle -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        };
    }

    private String fmt(double value) {
        return String.format("%.2f", value);
    }
}
