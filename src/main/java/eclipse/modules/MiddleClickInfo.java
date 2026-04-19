package eclipse.modules;

import eclipse.Eclipse;
import eclipse.gui.EclipseToastOverlay;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

public class MiddleClickInfo extends Module {
    private static final int FRIEND_ACCENT = 0x47F2A3;
    private static final int BLOCK_ACCENT = 0x29D6FF;
    private static final int ENTITY_ACCENT = 0xFFCF5A;
    private static final int MISS_ACCENT = 0xB7BAC6;
    private static final Color PLAYER_SIDE = new Color(71, 242, 163, 34);
    private static final Color PLAYER_LINE = new Color(71, 242, 163, 220);
    private static final Color MOB_SIDE = new Color(255, 207, 90, 32);
    private static final Color MOB_LINE = new Color(255, 207, 90, 220);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPreview = settings.createGroup("Preview");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximum middle-click inspect distance.")
        .defaultValue(6.0)
        .range(1.0, 12.0)
        .sliderRange(3.0, 8.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> cancelMiddleClick = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-middle-click")
        .description("Cancels the vanilla middle-click action after handling the target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> previewEntities = sgPreview.add(new BoolSetting.Builder()
        .name("entity-preview")
        .description("Highlights the player or mob currently under the crosshair.")
        .defaultValue(true)
        .build()
    );

    private Entity previewEntity;

    public MiddleClickInfo() {
        super(Eclipse.CATEGORY, "middle-click-info", "Middle-click players to add them as friends, or inspect blocks and entities.");
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (event.action != KeyAction.Press || event.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        if (mc.currentScreen != null) return;
        if (mc.player == null || mc.world == null) return;

        if (cancelMiddleClick.get()) event.cancel();
        inspectTarget();
    }

    private void inspectTarget() {
        HitResult target = mc.crosshairTarget;
        if (target == null || target.getType() == HitResult.Type.MISS) {
            EclipseToastOverlay.show("Middle Click", "No target", MISS_ACCENT);
            return;
        }

        if (target instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (!isWithinRange(entity)) {
                EclipseToastOverlay.show("Middle Click", "Target out of range", MISS_ACCENT);
                return;
            }

            inspectEntity(entity);
            return;
        }

        if (target instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            if (!isWithinRange(pos)) {
                EclipseToastOverlay.show("Middle Click", "Block out of range", MISS_ACCENT);
                return;
            }

            inspectBlock(pos);
        }
    }

    private void inspectEntity(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            Friend friend = new Friend(player);
            String body = playerSummary(player);
            if (Friends.get().add(friend)) {
                EclipseToastOverlay.show("Friend added", body, FRIEND_ACCENT);
            } else {
                EclipseToastOverlay.show("Already a friend", body, FRIEND_ACCENT);
            }
            return;
        }

        String name = entity.getName().getString();
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        EclipseToastOverlay.show("Entity: " + name, id, ENTITY_ACCENT);
    }

    private void inspectBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        String name = state.getBlock().getName().getString();
        String id = Registries.BLOCK.getId(state.getBlock()).toString();
        EclipseToastOverlay.show("Block: " + name, id, new ItemStack(state.getBlock()), BLOCK_ACCENT);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!previewEntities.get()) {
            previewEntity = null;
            return;
        }

        updatePreviewEntity();
        if (previewEntity == null || event.renderer == null) return;

        Box box = previewEntity.getBoundingBox();
        boolean player = previewEntity instanceof PlayerEntity;
        event.renderer.box(
            box.minX,
            box.minY,
            box.minZ,
            box.maxX,
            box.maxY,
            box.maxZ,
            player ? PLAYER_SIDE : MOB_SIDE,
            player ? PLAYER_LINE : MOB_LINE,
            ShapeMode.Both,
            0
        );
    }

    private void updatePreviewEntity() {
        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) {
            previewEntity = null;
            return;
        }

        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) {
            previewEntity = null;
            return;
        }

        Entity entity = hit.getEntity();
        if (!isPreviewable(entity) || !isWithinRange(entity)) {
            previewEntity = null;
            return;
        }

        previewEntity = entity;
    }

    private boolean isPreviewable(Entity entity) {
        return entity != null
            && entity != mc.player
            && !entity.isRemoved()
            && entity.isAlive()
            && (entity instanceof PlayerEntity || entity instanceof LivingEntity);
    }

    private boolean isWithinRange(Entity entity) {
        if (mc.player == null || entity == null) return false;
        double max = range.get();
        return entity.squaredDistanceTo(mc.player) <= max * max;
    }

    private boolean isWithinRange(BlockPos pos) {
        if (mc.player == null || pos == null) return false;
        double max = range.get();
        double dx = mc.player.getX() - (pos.getX() + 0.5);
        double dy = mc.player.getEyeY() - (pos.getY() + 0.5);
        double dz = mc.player.getZ() - (pos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= max * max;
    }

    private String playerSummary(PlayerEntity player) {
        double distance = mc.player == null ? 0.0 : Math.sqrt(player.squaredDistanceTo(mc.player));
        return player.getName().getString()
            + " | "
            + Math.round(player.getHealth() * 10.0) / 10.0
            + " HP | "
            + Math.round(distance * 10.0) / 10.0
            + "m";
    }
}
