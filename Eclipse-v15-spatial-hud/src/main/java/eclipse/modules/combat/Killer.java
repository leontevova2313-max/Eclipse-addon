package eclipse.modules.combat;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Killer extends Module {
    public enum RotationMode {
        Off,
        OnAttack,
        Always
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgTiming = settings.createGroup("Timing");

    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotation")
        .description("How the module rotates to the target.")
        .defaultValue(RotationMode.OnAttack)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to a weapon before attacking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches back after the attack.")
        .defaultValue(true)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<Boolean> onlyWeapons = sgGeneral.add(new BoolSetting.Builder()
        .name("only-weapons")
        .description("Attacks only while holding a weapon, unless auto-switch finds one.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings the hand after attacking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnUse = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Pauses while using items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stickyTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("sticky-target")
        .description("Keeps focusing the current target while it stays valid.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smartWeapon = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-weapon")
        .description("Chooses the strongest hotbar weapon instead of the first one found.")
        .defaultValue(true)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only attacks while the attack key is held.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How targets are sorted.")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    private final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder()
        .name("max-targets")
        .description("How many targets can be attacked per cycle.")
        .defaultValue(1)
        .range(1, 6)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("range")
        .description("Attack range for visible targets.")
        .defaultValue(4.4)
        .range(0.0, 8.0)
        .sliderRange(0.0, 6.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> wallsRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Attack range through walls.")
        .defaultValue(3.2)
        .range(0.0, 8.0)
        .sliderRange(0.0, 6.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> ignoreNamed = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-named")
        .description("Ignores mobs with custom names.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> customDelay = sgTiming.add(new BoolSetting.Builder()
        .name("custom-delay")
        .description("Uses the tick delay instead of the vanilla attack cooldown.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> hitDelay = sgTiming.add(new IntSetting.Builder()
        .name("hit-delay")
        .description("Delay between attacks in ticks.")
        .defaultValue(5)
        .range(0, 20)
        .sliderRange(0, 12)
        .visible(customDelay::get)
        .build()
    );

    private final Setting<Double> cooldown = sgTiming.add(new DoubleSetting.Builder()
        .name("cooldown")
        .description("Minimum vanilla attack cooldown progress.")
        .defaultValue(0.92)
        .range(0.0, 1.0)
        .sliderRange(0.0, 1.0)
        .decimalPlaces(2)
        .visible(() -> !customDelay.get())
        .build()
    );

    private final List<Entity> targets = new ArrayList<>();
    private int delayTimer;
    private LivingEntity focusTarget;

    public Killer() {
        super(Eclipse.COMBAT, "killer", "Fast PvP combat module with ready defaults.");
    }

    @Override
    public void onActivate() {
        targets.clear();
        delayTimer = 0;
        focusTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (pauseOnUse.get() && mc.player.isUsingItem()) return;
        if (onlyOnClick.get() && !mc.options.attackKey.isPressed()) return;
        if (!readyToAttack()) return;

        targets.clear();
        if (stickyTarget.get() && isValidTarget(focusTarget)) targets.add(focusTarget);
        TargetUtils.getList(targets, entity -> entity != focusTarget && isValidTarget(entity), priority.get(), maxTargets.get());
        if (targets.isEmpty()) return;

        focusTarget = (LivingEntity) targets.get(0);

        for (Entity entity : targets) {
            if (attack((LivingEntity) entity)) break;
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (delayTimer > 0) delayTimer--;
        if (rotation.get() != RotationMode.Always || targets.isEmpty()) return;

        Entity target = targets.get(0);
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 70);
    }

    private boolean readyToAttack() {
        if (customDelay.get()) return delayTimer <= 0;
        return mc.player.getAttackCooldownProgress(0.5F) >= cooldown.get();
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity == mc.player || !living.isAlive() || entity.isRemoved()) return false;
        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;

        if (entity instanceof PlayerEntity player) {
            if (player.isCreative() || player.isSpectator()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
        }

        double maxRange = PlayerUtils.canSeeEntity(entity) ? range.get() : wallsRange.get();
        return PlayerUtils.isWithin(entity, maxRange);
    }

    private boolean attack(LivingEntity target) {
        int weaponSlot = findWeaponSlot();
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        boolean shouldSwap = autoSwitch.get() && weaponSlot >= 0 && weaponSlot != selectedSlot;
        if (onlyWeapons.get() && !isWeapon(mc.player.getMainHandStack()) && !shouldSwap) return false;

        Runnable attack = () -> {
            boolean swapped = false;
            if (shouldSwap) swapped = InvUtils.swap(weaponSlot, false);

            mc.interactionManager.attackEntity(mc.player, target);
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);

            if (swapped && swapBack.get()) InvUtils.swapBack();
            delayTimer = Math.max(0, hitDelay.get());
        };

        if (rotation.get() == RotationMode.Off) {
            attack.run();
        } else {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 80, attack);
        }

        return true;
    }

    private int findWeaponSlot() {
        if (!smartWeapon.get()) {
            for (int slot = 0; slot < 9; slot++) {
                if (isWeapon(mc.player.getInventory().getStack(slot))) return slot;
            }
            return -1;
        }

        int bestSlot = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!isWeapon(stack)) continue;

            double score = weaponScore(stack);
            if (slot == selectedSlot) score += 0.15;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private double weaponScore(ItemStack stack) {
        Item item = stack.getItem();

        if (item == Items.NETHERITE_SWORD) return 9.0;
        if (item == Items.DIAMOND_SWORD) return 8.4;
        if (item == Items.IRON_SWORD) return 7.8;
        if (item == Items.STONE_SWORD || item == Items.COPPER_SWORD) return 7.2;
        if (item == Items.GOLDEN_SWORD) return 7.0;
        if (item == Items.WOODEN_SWORD) return 6.6;
        if (item == Items.NETHERITE_AXE) return 8.1;
        if (item == Items.DIAMOND_AXE) return 7.7;
        if (item == Items.IRON_AXE) return 7.2;
        if (item == Items.STONE_AXE || item == Items.COPPER_AXE) return 6.8;
        if (item == Items.GOLDEN_AXE) return 6.6;
        if (item == Items.WOODEN_AXE) return 6.2;
        if (item == Items.MACE) return 7.9;
        if (item == Items.TRIDENT) return 7.4;

        return 0.0;
    }

    private boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.WOODEN_SWORD
            || item == Items.COPPER_SWORD
            || item == Items.STONE_SWORD
            || item == Items.GOLDEN_SWORD
            || item == Items.IRON_SWORD
            || item == Items.DIAMOND_SWORD
            || item == Items.NETHERITE_SWORD
            || item == Items.WOODEN_AXE
            || item == Items.COPPER_AXE
            || item == Items.STONE_AXE
            || item == Items.GOLDEN_AXE
            || item == Items.IRON_AXE
            || item == Items.DIAMOND_AXE
            || item == Items.NETHERITE_AXE
            || item == Items.MACE
            || item == Items.TRIDENT;
    }

}

