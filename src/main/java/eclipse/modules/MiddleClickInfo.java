package eclipse.modules;

import eclipse.Eclipse;
import eclipse.gui.EclipseToastOverlay;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class MiddleClickInfo extends Module {
    private static final int FRIEND_ACCENT = 0x47F2A3;
    private static final int BLOCK_ACCENT = 0x29D6FF;
    private static final int ENTITY_ACCENT = 0xFFCF5A;
    private static final int MISS_ACCENT = 0xB7BAC6;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> cancelMiddleClick = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-middle-click")
        .description("Cancels the vanilla middle-click action after handling the target.")
        .defaultValue(true)
        .build()
    );

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
            inspectEntity(entityHit.getEntity());
            return;
        }

        if (target instanceof BlockHitResult blockHit) {
            inspectBlock(blockHit.getBlockPos());
        }
    }

    private void inspectEntity(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            Friend friend = new Friend(player);
            if (Friends.get().add(friend)) {
                EclipseToastOverlay.show("Friend added", player.getName().getString(), FRIEND_ACCENT);
            } else {
                EclipseToastOverlay.show("Already a friend", player.getName().getString(), FRIEND_ACCENT);
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
}
