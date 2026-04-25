package eclipse.modules.utility;

import eclipse.Eclipse;
import eclipse.client.inventory.InventoryPresetStore;
import eclipse.client.inventory.InventoryPresetStore.InventoryPreset;
import eclipse.client.inventory.InventoryPresetStore.PresetSlot;
import eclipse.gui.EclipseToastOverlay;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryPresets extends Module {
    public enum Action {
        Save,
        Apply
    }

    private static final int OK = 0x47F2A3;
    private static final int WARN = 0xFFCF5A;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Action> action = sgGeneral.add(new EnumSetting.Builder<Action>()
        .name("action")
        .description("Save captures the current layout. Apply moves matching items into the captured slots.")
        .defaultValue(Action.Apply)
        .build()
    );

    private final Setting<String> presetName = sgGeneral.add(new StringSetting.Builder()
        .name("preset-name")
        .description("Inventory preset name.")
        .defaultValue("default")
        .build()
    );

    private final Setting<Boolean> includeHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("include-hotbar")
        .description("Includes hotbar slots 1-9.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> includeMainInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("include-main-inventory")
        .description("Includes the main inventory slots above the hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> actionsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("actions-per-tick")
        .description("Maximum slot swaps per tick while applying a preset.")
        .defaultValue(2)
        .range(1, 12)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Shows Eclipse toasts for save/apply results.")
        .defaultValue(true)
        .build()
    );

    private InventoryPreset activePreset;
    private int cursor;

    public InventoryPresets() {
        super(Eclipse.UTILITY, "inventory-presets", "Saves and reapplies inventory layouts with safe JSON persistence.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        if (action.get() == Action.Save) {
            savePreset();
            toggle();
            return;
        }

        Map<String, InventoryPreset> presets = InventoryPresetStore.load();
        activePreset = presets.get(normalizedName());
        cursor = 0;
        if (activePreset == null) {
            if (notify.get()) EclipseToastOverlay.show("Inventory Presets", "Preset not found: " + normalizedName(), WARN);
            toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || activePreset == null) return;

        int actions = 0;
        while (actions < actionsPerTick.get() && cursor < 36) {
            int targetSlot = cursor++;
            if (!included(targetSlot)) continue;
            PresetSlot wanted = activePreset.slots.get(targetSlot);
            if (wanted == null || wanted.itemId == null || wanted.itemId.equals("minecraft:air")) continue;
            if (slotMatches(targetSlot, wanted.itemId)) continue;

            int source = findItemSlot(wanted.itemId, targetSlot);
            if (source < 0) continue;

            swapInventorySlots(source, targetSlot);
            actions++;
        }

        if (cursor >= 36) {
            if (notify.get()) EclipseToastOverlay.show("Inventory Presets", "Applied: " + activePreset.name, OK);
            activePreset = null;
            toggle();
        }
    }

    private void savePreset() {
        Map<String, InventoryPreset> presets = InventoryPresetStore.load();
        InventoryPreset preset = new InventoryPreset();
        preset.name = normalizedName();

        for (int slot = 0; slot < 36; slot++) {
            if (!included(slot)) continue;
            ItemStack stack = mc.player.getInventory().getStack(slot);
            PresetSlot presetSlot = new PresetSlot();
            presetSlot.itemId = Registries.ITEM.getId(stack.getItem()).toString();
            presetSlot.count = stack.getCount();
            preset.slots.put(slot, presetSlot);
        }

        presets.put(preset.name, preset);
        try {
            InventoryPresetStore.save(presets);
            if (notify.get()) EclipseToastOverlay.show("Inventory Presets", "Saved: " + preset.name, OK);
        } catch (IOException exception) {
            if (notify.get()) EclipseToastOverlay.show("Inventory Presets", "Save failed: " + exception.getClass().getSimpleName(), WARN);
        }
    }

    private boolean included(int slot) {
        if (slot >= 0 && slot <= 8) return includeHotbar.get();
        if (slot >= 9 && slot <= 35) return includeMainInventory.get();
        return false;
    }

    private boolean slotMatches(int slot, String itemId) {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        return Registries.ITEM.getId(stack.getItem()).toString().equals(itemId);
    }

    private int findItemSlot(String itemId, int targetSlot) {
        for (int slot = 0; slot < 36; slot++) {
            if (!included(slot) || slot == targetSlot) continue;
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty() || stack.isOf(Items.AIR)) continue;
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (id.toString().equals(itemId)) return slot;
        }
        return -1;
    }

    private void swapInventorySlots(int source, int target) {
        int syncId = mc.player.currentScreenHandler.syncId;

        if (isHotbar(source)) {
            mc.interactionManager.clickSlot(syncId, toContainerSlot(target), source, SlotActionType.SWAP, mc.player);
            return;
        }

        if (isHotbar(target)) {
            mc.interactionManager.clickSlot(syncId, toContainerSlot(source), target, SlotActionType.SWAP, mc.player);
            return;
        }

        int sourceContainerSlot = toContainerSlot(source);
        int targetContainerSlot = toContainerSlot(target);
        mc.interactionManager.clickSlot(syncId, sourceContainerSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, targetContainerSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, sourceContainerSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private boolean isHotbar(int slot) {
        return slot >= 0 && slot <= 8;
    }

    private int toContainerSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    private String normalizedName() {
        String value = presetName.get();
        if (value == null) return "default";
        value = value.trim();
        return value.isEmpty() ? "default" : value;
    }
}
