package eclipse.modules.movement;

import eclipse.Eclipse;
import eclipse.gui.EclipseToastOverlay;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * One-shot firework key module.
 *
 * Bind this module to any key in Meteor's module keybind UI. When the key is pressed
 * the module activates, finds a firework rocket anywhere in the player inventory,
 * uses it, then disables itself. This keeps the binding simple and avoids a custom
 * key listener that could conflict with Meteor's keybind system.
 */
public class AutoFireworks extends Module {
    private static final int OK = 0x47F2A3;
    private static final int WARN = 0xFFCF5A;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> fallbackSlot = sgGeneral.add(new IntSetting.Builder()
        .name("fallback-hotbar-slot")
        .description("Hotbar slot used temporarily when rockets are only in the main inventory. 1-9.")
        .defaultValue(9)
        .range(1, 9)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swaps back to the previous selected hotbar slot after using a rocket.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> restoreInventorySlot = sgGeneral.add(new BoolSetting.Builder()
        .name("restore-inventory-slot")
        .description("When a rocket is moved from the main inventory, moves it back after use.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Shows a small Eclipse toast when a rocket was used or not found.")
        .defaultValue(false)
        .build()
    );

    public AutoFireworks() {
        super(Eclipse.MOVEMENT, "auto-fireworks", "Uses a firework rocket from anywhere in inventory when the module keybind is pressed.");
    }

    @Override
    public void onActivate() {
        try {
            useRocket();
        } finally {
            if (isActive()) toggle();
        }
    }

    private void useRocket() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        FindItemResult rocket = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!rocket.found()) rocket = InvUtils.find(Items.FIREWORK_ROCKET);

        if (!rocket.found()) {
            if (notify.get()) EclipseToastOverlay.show("Auto Fireworks", "No firework rockets found", WARN);
            return;
        }

        if (rocket.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            if (notify.get()) EclipseToastOverlay.show("Auto Fireworks", "Used offhand rocket", OK);
            return;
        }

        if (rocket.isHotbar()) {
            boolean swapped = InvUtils.swap(rocket.slot(), swapBack.get());
            if (!swapped) return;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (swapBack.get()) InvUtils.swapBack();
            if (notify.get()) EclipseToastOverlay.show("Auto Fireworks", "Used hotbar rocket", OK);
            return;
        }

        int targetHotbarSlot = fallbackSlot.get() - 1;
        InvUtils.move().from(rocket.slot()).toHotbar(targetHotbarSlot);

        boolean swapped = InvUtils.swap(targetHotbarSlot, swapBack.get());
        if (!swapped) return;

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (swapBack.get()) InvUtils.swapBack();

        if (restoreInventorySlot.get()) {
            InvUtils.move().fromHotbar(targetHotbarSlot).to(rocket.slot());
        }

        if (notify.get()) EclipseToastOverlay.show("Auto Fireworks", "Used inventory rocket", OK);
    }
}
