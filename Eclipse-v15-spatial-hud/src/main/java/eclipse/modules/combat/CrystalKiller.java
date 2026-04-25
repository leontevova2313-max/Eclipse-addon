package eclipse.modules.combat;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class CrystalKiller extends Module {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    public enum SwitchMode {
        Off,
        Normal,
        Silent
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgFacePlace = settings.createGroup("Face Place");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Range for selecting player targets.")
        .defaultValue(10.0)
        .range(0.0, 16.0)
        .sliderRange(0.0, 14.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How player targets are sorted.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<SwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("auto-switch")
        .description("Switches to crystals before placing.")
        .defaultValue(SwitchMode.Silent)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side before placing or breaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings the hand after placing or breaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stickyTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("sticky-target")
        .description("Keeps working on the current target while it stays valid.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> killSound = sgGeneral.add(new BoolSetting.Builder()
        .name("crystal-kill-sound")
        .description("Plays a crystal kill sound after a target dies.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> doPlace = sgPlace.add(new BoolSetting.Builder()
        .name("place")
        .description("Places end crystals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay between crystal placements in ticks.")
        .defaultValue(1)
        .range(0, 20)
        .sliderRange(0, 10)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Range for placing crystals.")
        .defaultValue(4.8)
        .range(0.0, 7.0)
        .sliderRange(0.0, 6.0)
        .decimalPlaces(1)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-walls-range")
        .description("Range for placing crystals through walls.")
        .defaultValue(3.8)
        .range(0.0, 7.0)
        .sliderRange(0.0, 6.0)
        .decimalPlaces(1)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Integer> searchRadius = sgPlace.add(new IntSetting.Builder()
        .name("search-radius")
        .description("Block search radius. Lower values are lighter on weak laptops.")
        .defaultValue(5)
        .range(1, 7)
        .sliderRange(1, 6)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Integer> searchHeight = sgPlace.add(new IntSetting.Builder()
        .name("search-height")
        .description("Vertical block search radius.")
        .defaultValue(3)
        .range(1, 5)
        .sliderRange(1, 4)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("Minimum target damage for placing.")
        .defaultValue(5.5)
        .range(0.0, 36.0)
        .sliderRange(0.0, 16.0)
        .decimalPlaces(1)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Boolean> placement112 = sgPlace.add(new BoolSetting.Builder()
        .name("1.12-placement")
        .description("Requires two air blocks above the base block.")
        .defaultValue(false)
        .visible(doPlace::get)
        .build()
    );

    private final Setting<Boolean> doBreak = sgBreak.add(new BoolSetting.Builder()
        .name("break")
        .description("Breaks end crystals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("Delay between crystal breaks in ticks.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 10)
        .visible(doBreak::get)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("Range for breaking crystals.")
        .defaultValue(5.0)
        .range(0.0, 7.0)
        .sliderRange(0.0, 6.0)
        .decimalPlaces(1)
        .visible(doBreak::get)
        .build()
    );

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-walls-range")
        .description("Range for breaking crystals through walls.")
        .defaultValue(4.0)
        .range(0.0, 7.0)
        .sliderRange(0.0, 6.0)
        .decimalPlaces(1)
        .visible(doBreak::get)
        .build()
    );

    private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder()
        .name("face-place")
        .description("Allows lower damage when the target is low or has weak armor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder()
        .name("health")
        .description("Face-place when the target health is below this value.")
        .defaultValue(8.0)
        .range(0.0, 36.0)
        .sliderRange(0.0, 20.0)
        .decimalPlaces(1)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Double> facePlaceDamage = sgFacePlace.add(new DoubleSetting.Builder()
        .name("damage")
        .description("Minimum damage while face-place is active.")
        .defaultValue(2.0)
        .range(0.0, 10.0)
        .sliderRange(0.0, 8.0)
        .decimalPlaces(1)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Integer> armorDurability = sgFacePlace.add(new IntSetting.Builder()
        .name("armor-durability")
        .description("Face-place when an armor piece has lower durability percent. 0 disables this check.")
        .defaultValue(20)
        .range(0, 100)
        .sliderRange(0, 80)
        .visible(facePlace::get)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgSafety.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("Maximum self damage from a crystal.")
        .defaultValue(6.0)
        .range(0.0, 36.0)
        .sliderRange(0.0, 18.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgSafety.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Prevents actions that could kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> selfDamageWeight = sgSafety.add(new DoubleSetting.Builder()
        .name("self-damage-weight")
        .description("How strongly self damage lowers placement and break scores.")
        .defaultValue(1.35)
        .range(0.0, 4.0)
        .sliderRange(0.0, 3.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Boolean> ignoreNakeds = sgSafety.add(new BoolSetting.Builder()
        .name("ignore-nakeds")
        .description("Ignores players without armor.")
        .defaultValue(false)
        .build()
    );

    private int placeTimer;
    private int breakTimer;
    private final List<Entity> targets = new ArrayList<>();
    private PlayerEntity target;
    private PlayerEntity lastTarget;

    public CrystalKiller() {
        super(Eclipse.COMBAT, "crystal-killer", "Crystal combat module with ready defaults and sounds.");
    }

    @Override
    public void onActivate() {
        placeTimer = 0;
        breakTimer = 0;
        target = null;
        lastTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        playKillSoundIfNeeded();
        target = findTarget();
        if (!isValidTarget(target)) return;

        lastTarget = target;

        if (breakTimer > 0) breakTimer--;
        if (placeTimer > 0) placeTimer--;

        if (doBreak.get() && breakTimer <= 0 && breakCrystal()) {
            breakTimer = Math.max(0, breakDelay.get());
            return;
        }

        if (doPlace.get() && placeTimer <= 0 && placeCrystal()) {
            placeTimer = Math.max(0, placeDelay.get());
        }
    }

    private boolean breakCrystal() {
        EndCrystalEntity bestCrystal = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive()) continue;

            Vec3d pos = crystal.getEntityPos();
            if (!isInRange(pos, breakRange.get(), breakWallsRange.get())) continue;

            float targetDamage = DamageUtils.crystalDamage(target, pos);
            float selfDamage = DamageUtils.crystalDamage(mc.player, pos);
            if (!safeSelfDamage(selfDamage)) continue;
            if (targetDamage < requiredDamage()) continue;

            double score = crystalScore(targetDamage, selfDamage, pos);
            if (score > bestScore) {
                bestScore = score;
                bestCrystal = crystal;
            }
        }

        if (bestCrystal == null) return false;

        EndCrystalEntity crystal = bestCrystal;
        Runnable action = () -> {
            mc.interactionManager.attackEntity(mc.player, crystal);
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        };

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(crystal), Rotations.getPitch(crystal), 90, action);
        else action.run();

        return true;
    }

    private boolean placeCrystal() {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found() && autoSwitch.get() != SwitchMode.Off) return false;
        if (!crystal.found() && !isCrystal(mc.player.getMainHandStack()) && !isCrystal(mc.player.getOffHandStack())) return false;

        BlockPos bestPos = null;
        Vec3d bestCrystalPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos center = mc.player.getBlockPos();
        int radius = searchRadius.get();
        int height = searchHeight.get();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -height; y <= height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (!canPlaceAt(pos)) continue;

                    Vec3d crystalPos = Vec3d.ofBottomCenter(pos.up());
                    if (!isInRange(crystalPos, placeRange.get(), placeWallsRange.get())) continue;

                    float targetDamage = DamageUtils.crystalDamage(target, crystalPos, false, pos);
                    float selfDamage = DamageUtils.crystalDamage(mc.player, crystalPos, false, pos);
                    if (!safeSelfDamage(selfDamage)) continue;
                    if (targetDamage < requiredDamage()) continue;

                    double score = crystalScore(targetDamage, selfDamage, crystalPos);
                    if (score > bestScore) {
                        bestScore = score;
                        bestPos = pos;
                        bestCrystalPos = crystalPos;
                    }
                }
            }
        }

        if (bestPos == null) return false;

        BlockPos placePos = bestPos;
        Vec3d lookPos = bestCrystalPos;
        Runnable action = () -> placeCrystalAt(placePos);

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(lookPos), Rotations.getPitch(lookPos), 90, action);
        else action.run();

        return true;
    }

    private void placeCrystalAt(BlockPos pos) {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        Hand hand = crystal.getHand();
        boolean swapped = false;

        if (hand == null) {
            if (autoSwitch.get() == SwitchMode.Off) return;
            if (!crystal.found()) return;
            swapped = InvUtils.swap(crystal.slot(), autoSwitch.get() == SwitchMode.Silent);
            if (!swapped) return;
            hand = Hand.MAIN_HAND;
        }

        BlockHitResult hit = new BlockHitResult(Vec3d.ofBottomCenter(pos.up()), Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, hand, hit);
        if (swing.get()) mc.player.swingHand(hand);

        if (swapped && autoSwitch.get() == SwitchMode.Silent) InvUtils.swapBack();
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (placement112.get() && !mc.world.getBlockState(pos.up(2)).isAir()) return false;

        double top = pos.getY() + (placement112.get() ? 3.0 : 2.0);
        Box box = new Box(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, top, pos.getZ() + 1.0);
        return mc.world.getOtherEntities(null, box).isEmpty();
    }

    private boolean isInRange(Vec3d pos, double visibleRange, double wallRange) {
        double range = canSee(pos) ? visibleRange : wallRange;
        return PlayerUtils.isWithin(pos, range);
    }

    private boolean canSee(Vec3d pos) {
        Vec3d eye = mc.player.getEyePos();
        HitResult result = mc.world.raycast(new RaycastContext(
            eye,
            pos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        ));

        return result.getType() == HitResult.Type.MISS || result.getPos().squaredDistanceTo(pos) < 0.09;
    }

    private boolean safeSelfDamage(float selfDamage) {
        if (selfDamage > maxSelfDamage.get()) return false;
        return !antiSuicide.get() || selfDamage < getHealth(mc.player) - 0.5F;
    }

    private double requiredDamage() {
        if (!facePlace.get() || target == null) return minDamage.get();
        if (getHealth(target) <= facePlaceHealth.get()) return facePlaceDamage.get();
        if (armorDurability.get() > 0 && hasWeakArmor(target)) return facePlaceDamage.get();
        return minDamage.get();
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player == null || player == mc.player || !player.isAlive() || player.isRemoved()) return false;
        if (!PlayerUtils.isWithin(player, targetRange.get())) return false;
        if (player.isCreative() || player.isSpectator()) return false;
        if (!Friends.get().shouldAttack(player)) return false;
        return !ignoreNakeds.get() || !isNaked(player);
    }

    private PlayerEntity findTarget() {
        if (stickyTarget.get() && isValidTarget(target)) return target;

        targets.clear();
        TargetUtils.getList(targets, entity -> entity instanceof PlayerEntity player && isValidTarget(player), priority.get(), 1);
        return targets.isEmpty() ? null : (PlayerEntity) targets.get(0);
    }

    private double crystalScore(float targetDamage, float selfDamage, Vec3d pos) {
        double score = targetDamage - selfDamage * selfDamageWeight.get();
        score -= Math.sqrt(mc.player.squaredDistanceTo(pos)) * 0.12;

        if (target != null) {
            if (targetDamage >= getHealth(target) + 0.25F) score += 20.0;
            else if (getHealth(target) <= facePlaceHealth.get()) score += 2.0;
            if (armorDurability.get() > 0 && hasWeakArmor(target)) score += 1.2;
        }

        if (selfDamage <= 1.0F) score += 0.5;
        return score;
    }

    private boolean hasWeakArmor(PlayerEntity player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            int left = stack.getMaxDamage() - stack.getDamage();
            int pct = MathHelper.floor(left * 100.0F / stack.getMaxDamage());
            if (pct <= armorDurability.get()) return true;
        }

        return false;
    }

    private boolean isNaked(PlayerEntity player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) return false;
        }

        return true;
    }

    private float getHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private boolean isCrystal(ItemStack stack) {
        return stack != null && stack.isOf(Items.END_CRYSTAL);
    }

    private void playKillSoundIfNeeded() {
        if (!killSound.get() || lastTarget == null) return;
        if (lastTarget.isAlive() && !lastTarget.isRemoved()) return;

        EclipseCombatSounds.playCrystalKill();
        lastTarget = null;
    }
}

