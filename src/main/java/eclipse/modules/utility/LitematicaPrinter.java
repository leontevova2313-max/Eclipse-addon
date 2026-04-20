package eclipse.modules.utility;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LitematicaPrinter extends Module {
    private static final int XP_BAR_WIDTH = 182;
    private static final int XP_BAR_HEIGHT = 5;
    private static final Color READY_SIDE = new Color(36, 196, 255, 38);
    private static final Color READY_LINE = new Color(36, 236, 255, 210);
    private static final Color SPECIAL_SIDE = new Color(151, 255, 92, 44);
    private static final Color SPECIAL_LINE = new Color(182, 255, 88, 230);
    private static final Color BREAK_SIDE = new Color(255, 78, 78, 42);
    private static final Color BREAK_LINE = new Color(255, 118, 82, 230);
    private static final Color PORTAL_SIDE = new Color(198, 95, 255, 42);
    private static final Color PORTAL_LINE = new Color(255, 216, 92, 230);
    private static final Color PULSE_SIDE = new Color(255, 255, 255, 74);
    private static final Color PULSE_LINE = new Color(255, 255, 255, 255);
    private static final int DEFAULT_PING_MS = 100;
    private static final int LOW_LATENCY_MS = 80;
    private static final int MID_LATENCY_MS = 150;
    private static final Set<String> IMPORTANT_STATE_PROPERTIES = Set.of(
        "type",
        "half",
        "facing",
        "horizontal_facing",
        "axis",
        "horizontal_axis",
        "shape",
        "rotation",
        "attachment",
        "face",
        "part",
        "hinge",
        "delay"
    );
    private static LitematicaPrinter activeInstance;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacement = settings.createGroup("Placement");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgSafety = settings.createGroup("Safety");

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Maximum placement interactions per tick.")
        .defaultValue(3)
        .range(1, 9)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Integer> horizontalRange = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-range")
        .description("Horizontal scan radius around the player.")
        .defaultValue(5)
        .range(1, 8)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> verticalRange = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-range")
        .description("Vertical scan radius around player eye level.")
        .defaultValue(5)
        .range(1, 8)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> scanLimit = sgGeneral.add(new IntSetting.Builder()
        .name("scan-limit")
        .description("Maximum schematic positions checked per tick for placement candidates.")
        .defaultValue(2200)
        .range(128, 6000)
        .sliderRange(256, 3000)
        .build()
    );

    private final Setting<Double> interactionRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("interaction-range")
        .description("Maximum eye-to-block-center distance.")
        .defaultValue(4.75)
        .range(1.0, 6.0)
        .sliderRange(3.0, 6.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Ticks to wait after a tick that placed at least one block.")
        .defaultValue(1)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<BuildOrder> buildOrder = sgGeneral.add(new EnumSetting.Builder<BuildOrder>()
        .name("build-order")
        .description("How placement candidates are prioritized.")
        .defaultValue(BuildOrder.StableSupport)
        .build()
    );

    private final Setting<Boolean> exactState = sgPlacement.add(new BoolSetting.Builder()
        .name("exact-state")
        .description("Requires the world state to match the schematic state.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> importantState = sgPlacement.add(new BoolSetting.Builder()
        .name("important-state")
        .description("Always checks slab halves, stairs, facing, axis, rotation, and similar placement-critical properties.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> replaceWrongState = sgPlacement.add(new BoolSetting.Builder()
        .name("replace-wrong-state")
        .description("Breaks same-block wrong-state blocks so they can be placed again with the schematic half or rotation.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkPlaceState = sgPlacement.add(new BoolSetting.Builder()
        .name("check-place-state")
        .description("Skips blocks that vanilla placement rules say cannot survive.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> avoidFallingBlocks = sgPlacement.add(new BoolSetting.Builder()
        .name("avoid-falling-blocks")
        .description("Skips falling blocks when they would immediately fall.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreDataBlocks = sgPlacement.add(new BoolSetting.Builder()
        .name("ignore-data-blocks")
        .description("Skips schematic-only technical blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noGrassToDirt = sgPlacement.add(new BoolSetting.Builder()
        .name("no-grass-to-dirt")
        .description("Treats grass block as valid when the schematic asks for dirt.")
        .defaultValue(true)
        .build()
    );

    private final Setting<RotationMode> rotationMode = sgPlacement.add(new EnumSetting.Builder<RotationMode>()
        .name("rotation-mode")
        .description("How to rotate before placement interactions.")
        .defaultValue(RotationMode.Schematic)
        .build()
    );

    private final Setting<Boolean> explicitPlaceSide = sgPlacement.add(new BoolSetting.Builder()
        .name("explicit-place-side")
        .description("Uses schematic properties to choose the clicked side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> meteorAirPlace = sgPlacement.add(new BoolSetting.Builder()
        .name("meteor-air-place")
        .description("Uses Meteor's placement helper, including its air-place fallback when no neighbor can be clicked.")
        .defaultValue(true)
        .build()
    );

    private final Setting<PortalIgniteMode> portalIgnite = sgPlacement.add(new EnumSetting.Builder<PortalIgniteMode>()
        .name("portal-ignite")
        .description("Uses flint and steel when the schematic contains a nether portal block.")
        .defaultValue(PortalIgniteMode.SaveDurability)
        .build()
    );

    private final Setting<BreakMode> breakMode = sgPlacement.add(new EnumSetting.Builder<BreakMode>()
        .name("break-mode")
        .description("Breaks blocks that block schematic placement.")
        .defaultValue(BreakMode.Off)
        .build()
    );

    private final Setting<Boolean> anySign = sgInventory.add(new BoolSetting.Builder()
        .name("any-sign")
        .description("Allows any sign item for schematic sign blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoMoveToHotbar = sgInventory.add(new BoolSetting.Builder()
        .name("auto-move-to-hotbar")
        .description("Moves required blocks from main inventory into the hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> moveCooldownSetting = sgInventory.add(new IntSetting.Builder()
        .name("move-cooldown")
        .description("Ticks to wait after moving an item into the hotbar.")
        .defaultValue(2)
        .range(0, 20)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> swing = sgInventory.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings the hand after an accepted interaction.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgInventory.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches back to the previous hotbar slot after placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderProcess = sgRender.add(new BoolSetting.Builder()
        .name("render-process")
        .description("Highlights the next blocks the printer can place or break.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> renderBlocks = sgRender.add(new IntSetting.Builder()
        .name("render-blocks")
        .description("Maximum highlighted candidate blocks.")
        .defaultValue(36)
        .range(1, 160)
        .sliderRange(8, 96)
        .build()
    );

    private final Setting<Boolean> xpProgressBar = sgRender.add(new BoolSetting.Builder()
        .name("xp-progress-bar")
        .description("Replaces the vanilla experience bar with selected schematic build progress while printing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> progressScanPerTick = sgRender.add(new IntSetting.Builder()
        .name("progress-scan-per-tick")
        .description("How many selected schematic positions are checked per tick for the progress bar.")
        .defaultValue(1200)
        .range(100, 50000)
        .sliderRange(500, 12000)
        .build()
    );

    private final Setting<Boolean> checkEntities = sgSafety.add(new BoolSetting.Builder()
        .name("check-entities")
        .description("Skips placement if an entity blocks the target shape.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnUse = sgSafety.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Pauses while the player is using an item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseWhileGliding = sgSafety.add(new BoolSetting.Builder()
        .name("pause-while-gliding")
        .description("Pauses while elytra gliding.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnCorrection = sgSafety.add(new BoolSetting.Builder()
        .name("pause-on-correction")
        .description("Pauses after a server position correction.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> correctionPause = sgSafety.add(new IntSetting.Builder()
        .name("correction-pause")
        .description("Ticks to pause after a server position correction.")
        .defaultValue(60)
        .range(0, 140)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Double> minTps = sgSafety.add(new DoubleSetting.Builder()
        .name("min-tps")
        .description("Pauses while Meteor's measured server TPS is below this value. Set to 0 to disable.")
        .defaultValue(17.0)
        .range(0.0, 20.0)
        .sliderRange(0.0, 20.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Integer> retryDelay = sgSafety.add(new IntSetting.Builder()
        .name("retry-delay")
        .description("Ticks before retrying the same position after sending an interaction.")
        .defaultValue(5)
        .range(0, 40)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Integer> maxRetries = sgSafety.add(new IntSetting.Builder()
        .name("max-retries")
        .description("Failed verification attempts before temporarily skipping a position.")
        .defaultValue(3)
        .range(1, 10)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Integer> skipImpossibleTicks = sgSafety.add(new IntSetting.Builder()
        .name("skip-impossible-ticks")
        .description("Ticks to skip a target after repeated failed placement verification.")
        .defaultValue(100)
        .range(20, 600)
        .sliderRange(40, 240)
        .build()
    );

    private final Setting<Boolean> pauseWhenMissingBlocks = sgSafety.add(new BoolSetting.Builder()
        .name("pause-when-missing-blocks")
        .description("Pauses briefly when the next valid target has no matching item available.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debug = sgSafety.add(new BoolSetting.Builder()
        .name("debug")
        .description("Prints compact printer status messages.")
        .defaultValue(false)
        .build()
    );

    private final LitematicaBridge bridge = new LitematicaBridge();
    private final Map<BlockPos, PendingAttempt> pendingAttempts = new HashMap<>();
    private final Map<BlockPos, Integer> failedAttempts = new HashMap<>();
    private final Map<BlockPos, Integer> skipCooldowns = new HashMap<>();
    private final List<Candidate> renderCandidates = new ArrayList<>();
    private final ProgressTracker progressTracker = new ProgressTracker();
    private ProgressSnapshot progressSnapshot = ProgressSnapshot.empty();
    private BlockPos lastActionPos;
    private int delayTicks;
    private int correctionTicks;
    private int moveCooldown;
    private int missingItemCooldown;
    private int lastActionTicks;
    private boolean warnedMissingLitematica;
    private boolean warnedBridgeError;

    public LitematicaPrinter() {
        super(Eclipse.UTILITY, "litematica-printer", "Prints the loaded Litematica schematic with server-aware placement pacing.");
    }

    @Override
    public void onActivate() {
        activeInstance = this;
        delayTicks = 0;
        correctionTicks = 0;
        moveCooldown = 0;
        lastActionTicks = 0;
        lastActionPos = null;
        warnedMissingLitematica = false;
        warnedBridgeError = false;
        pendingAttempts.clear();
        failedAttempts.clear();
        skipCooldowns.clear();
        renderCandidates.clear();
        progressTracker.reset();
        progressSnapshot = ProgressSnapshot.empty();
        bridge.clearError();
    }

    @Override
    public void onDeactivate() {
        if (activeInstance == this) activeInstance = null;
        renderCandidates.clear();
        progressSnapshot = ProgressSnapshot.empty();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && pauseOnCorrection.get()) {
            correctionTicks = correctionPause.get();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        tickCooldowns();
        updateProgressSnapshot();
        if (lastActionTicks > 0) lastActionTicks--;
        if (missingItemCooldown > 0) missingItemCooldown--;
        if (!canRunThisTick()) {
            renderCandidates.clear();
            return;
        }

        List<Candidate> candidates = collectCandidates();
        updateRenderCandidates(candidates);
        if (candidates.isEmpty()) return;

        int placed = 0;
        int maxPlacements = effectiveBlocksPerTick();
        boolean brokeBlock = false;
        RotationSpec usedStateRotation = null;

        for (Candidate candidate : candidates) {
            if (placed >= maxPlacements) break;
            if (isTemporarilyBlocked(candidate.pos)) continue;

            BlockState worldState = mc.world.getBlockState(candidate.pos);
            if (!stillNeedsWork(worldState, candidate.state)) continue;

            if (candidate.configureOnly) {
                if (configureBlockState(candidate.pos, candidate.state)) {
                    markAttempt(candidate.pos, candidate.state);
                    markAction(candidate.pos);
                    placed++;
                } else {
                    markImpossible(candidate.pos);
                }
                continue;
            }

            if (candidate.doubleSlab) {
                FindItemResult result = findRequiredItem(candidate.state);
                if (result.found() && interactAt(candidate.pos, result, slabCompletionSide(worldState), null)) {
                    markAttempt(candidate.pos, candidate.state);
                    markAction(candidate.pos);
                    placed++;
                } else if (!result.found()) {
                    handleMissingItem(candidate);
                }
                continue;
            }

            if (candidate.portalIgnite) {
                FindItemResult result = findFlintAndSteel();
                Direction side = findIgniteSide(candidate.pos);
                if (result.found() && side != null && interactAt(candidate.pos.offset(side.getOpposite()), result, side, null)) {
                    markAttempt(candidate.pos, candidate.state);
                    markAction(candidate.pos);
                    placed++;
                } else if (!result.found()) {
                    handleMissingItem(candidate);
                } else {
                    markImpossible(candidate.pos);
                }
                continue;
            }

            if (candidate.breakOnly) {
                if (!brokeBlock && BlockUtils.breakBlock(candidate.pos, swing.get())) {
                    markAttempt(candidate.pos, candidate.state);
                    markAction(candidate.pos);
                    placed++;
                    brokeBlock = true;
                } else if (brokeBlock) {
                    continue;
                } else {
                    markImpossible(candidate.pos);
                }
                continue;
            }

            FindItemResult result = findRequiredItem(candidate.state);
            if (!result.found()) {
                handleMissingItem(candidate);
                if (pauseWhenMissingBlocks.get()) break;
                continue;
            }

            RotationSpec stateRotation = getStateRotation(candidate.state);
            if (stateRotation != null && usedStateRotation != null && !usedStateRotation.equals(stateRotation)) continue;

            Direction side = explicitPlaceSide.get() ? placementSide(candidate.state) : null;
            if (placeAt(candidate.pos, candidate.state, result, side, stateRotation)) {
                if (stateRotation != null) usedStateRotation = stateRotation;
                markAttempt(candidate.pos, candidate.state);
                markAction(candidate.pos);
                placed++;
            } else {
                markImpossible(candidate.pos);
            }
        }

        if (placed > 0) {
            delayTicks = effectiveTickDelay();
            if (debug.get()) info("Placed " + placed + " interaction(s).");
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderProcess.get()) return;

        int limit = Math.min(renderCandidates.size(), renderBlocks.get());
        for (int i = 0; i < limit; i++) {
            Candidate candidate = renderCandidates.get(i);
            Color side = candidateSide(candidate);
            Color line = candidateLine(candidate);
            event.renderer.box(candidate.pos, side, line, ShapeMode.Both, 0);
        }

        if (!renderCandidates.isEmpty()) {
            Vec3d eye = mc.player.getEyePos();
            Vec3d target = renderCandidates.get(0).pos.toCenterPos();
            event.renderer.line(eye.x, eye.y, eye.z, target.x, target.y, target.z, new Color(255, 255, 255, 95));
        }

        if (lastActionPos != null && lastActionTicks > 0) {
            double grow = (10 - lastActionTicks) * 0.018;
            event.renderer.box(
                lastActionPos.getX() - grow,
                lastActionPos.getY() - grow,
                lastActionPos.getZ() - grow,
                lastActionPos.getX() + 1.0 + grow,
                lastActionPos.getY() + 1.0 + grow,
                lastActionPos.getZ() + 1.0 + grow,
                PULSE_SIDE,
                PULSE_LINE,
                ShapeMode.Both,
                0
            );
        }
    }

    private boolean canRunThisTick() {
        if (!FabricLoader.getInstance().isModLoaded("litematica")) {
            if (!warnedMissingLitematica) {
                warning("Litematica is not loaded.");
                warnedMissingLitematica = true;
            }
            return false;
        }

        if (pauseOnUse.get() && mc.player.isUsingItem()) {
            delayTicks = Math.max(delayTicks, 3);
            return false;
        }

        if (pauseWhileGliding.get() && mc.player.isGliding()) return false;

        if (delayTicks > 0) {
            delayTicks--;
            return false;
        }

        if (correctionTicks > 0) {
            correctionTicks--;
            return false;
        }

        if (moveCooldown > 0) {
            moveCooldown--;
            return false;
        }

        if (missingItemCooldown > 0 && pauseWhenMissingBlocks.get()) return false;

        return minTps.get() <= 0.0 || TickRate.INSTANCE.getTickRate() >= minTps.get();
    }

    private List<Candidate> collectCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        BlockPos origin = mc.player.getBlockPos();
        Vec3d eye = mc.player.getEyePos();
        double maxDistanceSq = interactionRange.get() * interactionRange.get();
        int centerY = origin.getY() + 1;
        int hRange = horizontalRange.get();
        int vRange = verticalRange.get();

        SelectedPlacement selected = bridge.getSelectedPlacement();
        if (selected != null && !selected.boxes().isEmpty()) {
            collectSelectedCandidates(candidates, selected, origin, centerY, hRange, vRange, eye, maxDistanceSq);
            candidates.sort(candidateComparator());
            return candidates;
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int scanned = 0;
        for (int x = origin.getX() - hRange; x <= origin.getX() + hRange; x++) {
            for (int y = centerY - vRange; y <= centerY + vRange; y++) {
                for (int z = origin.getZ() - hRange; z <= origin.getZ() + hRange; z++) {
                    if (scanned++ >= scanLimit.get()) break;
                    mutable.set(x, y, z);
                    if (eye.squaredDistanceTo(mutable.toCenterPos()) > maxDistanceSq) continue;

                    BlockPos pos = new BlockPos(mutable);
                    if (isTemporarilyBlocked(pos) || !isLoadedAndBuildable(pos)) continue;

                    BlockState schematicState = bridge.getSchematicState(pos);
                    if (schematicState == null) {
                        warnBridgeError();
                        continue;
                    }

                    addCandidate(candidates, pos, schematicState, eye);
                }
                if (scanned >= scanLimit.get()) break;
            }
            if (scanned >= scanLimit.get()) break;
        }

        candidates.sort(candidateComparator());
        return candidates;
    }

    private void collectSelectedCandidates(List<Candidate> candidates, SelectedPlacement selected, BlockPos origin, int centerY, int hRange, int vRange, Vec3d eye, double maxDistanceSq) {
        int minX = origin.getX() - hRange;
        int maxX = origin.getX() + hRange;
        int minY = centerY - vRange;
        int maxY = centerY + vRange;
        int minZ = origin.getZ() - hRange;
        int maxZ = origin.getZ() + hRange;

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int scanned = 0;
        for (BlockBox box : selected.boxes()) {
            int fromX = Math.max(minX, box.minX());
            int toX = Math.min(maxX, box.maxX());
            int fromY = Math.max(minY, box.minY());
            int toY = Math.min(maxY, box.maxY());
            int fromZ = Math.max(minZ, box.minZ());
            int toZ = Math.min(maxZ, box.maxZ());
            if (fromX > toX || fromY > toY || fromZ > toZ) continue;

            for (int y = fromY; y <= toY; y++) {
                for (int x = fromX; x <= toX; x++) {
                    for (int z = fromZ; z <= toZ; z++) {
                        if (scanned++ >= scanLimit.get()) return;
                        mutable.set(x, y, z);
                        if (eye.squaredDistanceTo(mutable.toCenterPos()) > maxDistanceSq) continue;

                        BlockPos pos = new BlockPos(mutable);
                        if (isTemporarilyBlocked(pos) || !isLoadedAndBuildable(pos)) continue;

                        BlockState schematicState = bridge.getSchematicState(pos);
                        if (schematicState == null) {
                            warnBridgeError();
                            continue;
                        }

                        addCandidate(candidates, pos, schematicState, eye);
                    }
                }
            }
        }
    }

    private void addCandidate(List<Candidate> candidates, BlockPos pos, BlockState schematicState, Vec3d eye) {
        BlockState worldState = mc.world.getBlockState(pos);
        if (ignoreDataBlocks.get() && isDataBlock(schematicState.getBlock())) return;
        if (noGrassToDirt.get() && worldState.isOf(Blocks.GRASS_BLOCK) && schematicState.isOf(Blocks.DIRT)) return;

        if (isDoubleSlabCompletion(worldState, schematicState)) {
            candidates.add(new Candidate(pos, schematicState, eye.squaredDistanceTo(pos.toCenterPos()), supportScore(pos, schematicState), false, true, false, false));
            return;
        }

        if (!stillNeedsWork(worldState, schematicState)) return;

        if (schematicState.isAir()) {
            return;
        }

        if (schematicState.isOf(Blocks.NETHER_PORTAL) && portalIgnite.get() != PortalIgniteMode.Off) {
            if (AbstractFireBlock.canPlaceAt(mc.world, pos, Direction.UP)) {
                candidates.add(new Candidate(pos, schematicState, eye.squaredDistanceTo(pos.toCenterPos()), supportScore(pos, schematicState), false, false, true, false));
            }
            return;
        }

        Item item = schematicState.getBlock().asItem();
        if (item == Items.AIR) return;

        if (canConfigureByUse(worldState, schematicState)) {
            candidates.add(new Candidate(pos, schematicState, eye.squaredDistanceTo(pos.toCenterPos()), supportScore(pos, schematicState), false, false, false, true));
            return;
        }

        if (!worldState.isReplaceable()) {
            if (shouldBreak(pos, worldState, schematicState)) {
                candidates.add(new Candidate(pos, schematicState, eye.squaredDistanceTo(pos.toCenterPos()), supportScore(pos, schematicState), true, false, false, false));
            }
            return;
        }

        if (checkPlaceState.get() && !schematicState.canPlaceAt(mc.world, pos)) return;
        if (avoidFallingBlocks.get()
            && schematicState.getBlock() instanceof FallingBlock
            && FallingBlock.canFallThrough(mc.world.getBlockState(pos.down()))) return;
        if (!BlockUtils.canPlaceBlock(pos, checkEntities.get(), schematicState.getBlock())) return;

        if (getPlacementHit(pos, schematicState, explicitPlaceSide.get() ? placementSide(schematicState) : null) == null && !meteorAirPlace.get()) return;

        candidates.add(new Candidate(pos, schematicState, eye.squaredDistanceTo(pos.toCenterPos()), supportScore(pos, schematicState), false, false, false, false));
    }

    private Comparator<Candidate> candidateComparator() {
        return switch (buildOrder.get()) {
            case Nearest -> Comparator.comparingDouble(Candidate::distanceSq);
            case Layers -> Comparator
                .comparingInt((Candidate candidate) -> candidate.pos.getY())
                .thenComparing((Candidate candidate) -> candidate.breakOnly ? 0 : 1)
                .thenComparingDouble(Candidate::distanceSq);
            case StableSupport -> Comparator
                .comparingInt((Candidate candidate) -> candidate.breakOnly ? 0 : 1)
                .thenComparing(Comparator.comparingInt(Candidate::supportScore).reversed())
                .thenComparingInt(candidate -> candidate.pos.getY())
                .thenComparingDouble(Candidate::distanceSq);
        };
    }

    private int supportScore(BlockPos pos, BlockState state) {
        int score = 0;
        if (!mc.world.getBlockState(pos.down()).isReplaceable()) score += 4;
        if (getPlacementHit(pos, state, explicitPlaceSide.get() ? placementSide(state) : null) != null) score += 3;

        for (Direction direction : Direction.values()) {
            BlockState neighbor = mc.world.getBlockState(pos.offset(direction));
            if (!neighbor.isAir() && neighbor.getFluidState().isEmpty()) score++;
        }

        if (state.getBlock() instanceof FallingBlock && !FallingBlock.canFallThrough(mc.world.getBlockState(pos.down()))) score += 4;
        return score;
    }

    private boolean stillNeedsWork(BlockState worldState, BlockState schematicState) {
        return !stateMatches(worldState, schematicState);
    }

    private boolean shouldBreak(BlockPos pos, BlockState worldState, BlockState schematicState) {
        if (worldState.isAir()) return false;
        if (!BlockUtils.canBreak(pos, worldState)) return false;
        if (needsWrongStateReplacement(worldState, schematicState)) return true;
        if (breakMode.get() == BreakMode.Off) return false;
        return breakMode.get() == BreakMode.All || !schematicState.isAir() || needsWrongStateReplacement(worldState, schematicState);
    }

    private void updateRenderCandidates(List<Candidate> candidates) {
        renderCandidates.clear();
        int limit = Math.min(candidates.size(), renderBlocks.get());
        for (int i = 0; i < limit; i++) renderCandidates.add(candidates.get(i));
    }

    private Color candidateSide(Candidate candidate) {
        if (candidate.breakOnly) return BREAK_SIDE;
        if (candidate.portalIgnite) return PORTAL_SIDE;
        if (candidate.doubleSlab) return SPECIAL_SIDE;
        return READY_SIDE;
    }

    private Color candidateLine(Candidate candidate) {
        if (candidate.breakOnly) return BREAK_LINE;
        if (candidate.portalIgnite) return PORTAL_LINE;
        if (candidate.doubleSlab) return SPECIAL_LINE;
        return READY_LINE;
    }

    private void markAction(BlockPos pos) {
        lastActionPos = new BlockPos(pos);
        lastActionTicks = 10;
    }

    private void updateProgressSnapshot() {
        if (!xpProgressBar.get() || !FabricLoader.getInstance().isModLoaded("litematica")) {
            progressSnapshot = ProgressSnapshot.empty();
            return;
        }

        SelectedPlacement selected = bridge.getSelectedPlacement();
        if (selected == null || selected.boxes().isEmpty()) {
            progressTracker.reset();
            progressSnapshot = ProgressSnapshot.empty();
            return;
        }

        if (selected.identity() != progressTracker.identity) progressTracker.start(selected);
        progressSnapshot = progressTracker.scan(selected, progressScanPerTick.get());
    }

    private boolean isCompleteForProgress(BlockPos pos, BlockState schematicState) {
        BlockState worldState = mc.world.getBlockState(pos);
        if (noGrassToDirt.get() && worldState.isOf(Blocks.GRASS_BLOCK) && schematicState.isOf(Blocks.DIRT)) return true;
        return stateMatches(worldState, schematicState);
    }

    private boolean stateMatches(BlockState worldState, BlockState schematicState) {
        if (exactState.get()) return worldState.equals(schematicState);
        if (worldState.getBlock() != schematicState.getBlock()) return false;
        return !importantState.get() || importantPropertiesMatch(worldState, schematicState);
    }

    private boolean needsWrongStateReplacement(BlockState worldState, BlockState schematicState) {
        return replaceWrongState.get()
            && worldState.getBlock() == schematicState.getBlock()
            && !worldState.equals(schematicState)
            && !importantPropertiesMatch(worldState, schematicState);
    }

    private boolean canConfigureByUse(BlockState worldState, BlockState schematicState) {
        return worldState.getBlock() == schematicState.getBlock()
            && worldState.contains(Properties.DELAY)
            && schematicState.contains(Properties.DELAY)
            && !worldState.get(Properties.DELAY).equals(schematicState.get(Properties.DELAY))
            && importantPropertiesMatchExcept(worldState, schematicState, "delay");
    }

    private boolean importantPropertiesMatch(BlockState worldState, BlockState schematicState) {
        return importantPropertiesMatchExcept(worldState, schematicState);
    }

    private boolean importantPropertiesMatchExcept(BlockState worldState, BlockState schematicState, String... ignoredProperties) {
        for (Property<?> property : schematicState.getProperties()) {
            if (!IMPORTANT_STATE_PROPERTIES.contains(property.getName())) continue;
            if (isIgnoredProperty(property.getName(), ignoredProperties)) continue;
            if (!worldState.contains(property)) return false;
            if (!samePropertyValue(worldState, schematicState, property)) return false;
        }

        return true;
    }

    private boolean isIgnoredProperty(String propertyName, String... ignoredProperties) {
        for (String ignoredProperty : ignoredProperties) {
            if (propertyName.equals(ignoredProperty)) return true;
        }

        return false;
    }

    private <T extends Comparable<T>> boolean samePropertyValue(BlockState worldState, BlockState schematicState, Property<T> property) {
        return worldState.get(property).equals(schematicState.get(property));
    }

    private FindItemResult findRequiredItem(BlockState state) {
        Block block = state.getBlock();
        Predicate<ItemStack> predicate;

        if (anySign.get() && state.isIn(BlockTags.ALL_HANGING_SIGNS)) {
            predicate = stack -> stack.isIn(ItemTags.HANGING_SIGNS);
        } else if (anySign.get() && state.isIn(BlockTags.ALL_SIGNS)) {
            predicate = stack -> stack.isIn(ItemTags.SIGNS);
        } else {
            predicate = stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block;
        }

        return findHotbarOrMove(predicate);
    }

    private FindItemResult findFlintAndSteel() {
        return findHotbarOrMove(stack -> {
            if (!stack.isOf(Items.FLINT_AND_STEEL)) return false;
            return portalIgnite.get() != PortalIgniteMode.SaveDurability || stack.getDamage() < stack.getMaxDamage() - 1;
        });
    }

    private FindItemResult findHotbarOrMove(Predicate<ItemStack> predicate) {
        FindItemResult hotbar = InvUtils.findInHotbar(predicate);
        if (hotbar.found()) return hotbar;
        if (!autoMoveToHotbar.get() || moveCooldown > 0) return hotbar;

        FindItemResult inventory = InvUtils.find(predicate, SlotUtils.MAIN_START, SlotUtils.MAIN_END);
        if (!inventory.found()) return hotbar;

        FindItemResult emptyHotbar = InvUtils.findInHotbar(ItemStack::isEmpty);
        int target = emptyHotbar.found() && emptyHotbar.isHotbar() ? emptyHotbar.slot() : mc.player.getInventory().getSelectedSlot();
        InvUtils.move().from(inventory.slot()).toHotbar(target);
        moveCooldown = effectiveMoveCooldown();
        return new FindItemResult(-1, 0);
    }

    private boolean placeAt(BlockPos pos, BlockState state, FindItemResult result, Direction preferredSide, RotationSpec stateRotation) {
        BlockHitResult hit = getPlacementHit(pos, state, preferredSide);
        if (hit == null && meteorAirPlace.get()) {
            return BlockUtils.place(pos, result, rotationMode.get() != RotationMode.None, 80, swing.get(), checkEntities.get(), swapBack.get());
        }

        if (hit == null) return false;
        if (!withinInteractionRange(hit.getPos())) return false;

        Runnable action = () -> interactWithItem(result, hit);
        RotationSpec rotation = rotationFor(hit, stateRotation);
        if (rotation != null) Rotations.rotate(rotation.yaw, rotation.pitch, 60, action);
        else action.run();
        return true;
    }

    private boolean configureBlockState(BlockPos pos, BlockState targetState) {
        Vec3d hitPos = Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0);
        if (!withinInteractionRange(hitPos)) return false;

        BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, pos, false);
        Runnable action = () -> interactCurrentHand(hit);
        RotationSpec rotation = rotationFor(hit, getStateRotation(targetState));
        if (rotation != null) Rotations.rotate(rotation.yaw, rotation.pitch, 60, action);
        else action.run();
        return true;
    }

    private boolean interactAt(BlockPos clickedPos, FindItemResult result, Direction side, RotationSpec stateRotation) {
        if (!canClickNeighbor(clickedPos)) return false;

        Vec3d hitPos = Vec3d.ofCenter(clickedPos).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        if (!withinInteractionRange(hitPos)) return false;
        BlockHitResult hit = new BlockHitResult(hitPos, side, clickedPos, false);
        Runnable action = () -> interactWithItem(result, hit);
        RotationSpec rotation = rotationFor(hit, stateRotation);
        if (rotation != null) Rotations.rotate(rotation.yaw, rotation.pitch, 60, action);
        else action.run();
        return true;
    }

    private void interactWithItem(FindItemResult result, BlockHitResult hit) {
        if (mc.player == null || mc.interactionManager == null) return;

        Hand hand = result.getHand();
        boolean swapped = false;
        if (hand == null) {
            swapped = InvUtils.swap(result.slot(), swapBack.get());
            if (!swapped) return;
            hand = Hand.MAIN_HAND;
        }

        boolean wasSneaking = mc.player.isSneaking();
        mc.player.setSneaking(false);

        ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, hand, hit);
        if (actionResult.isAccepted()) {
            if (swing.get()) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.setSneaking(wasSneaking);
        if (swapped && swapBack.get()) InvUtils.swapBack();
    }

    private void interactCurrentHand(BlockHitResult hit) {
        if (mc.player == null || mc.interactionManager == null) return;

        Hand hand = Hand.MAIN_HAND;
        boolean wasSneaking = mc.player.isSneaking();
        mc.player.setSneaking(false);

        ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, hand, hit);
        if (actionResult.isAccepted()) {
            if (swing.get()) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.setSneaking(wasSneaking);
    }

    private boolean withinInteractionRange(Vec3d hitPos) {
        return mc.player.getEyePos().squaredDistanceTo(hitPos) <= interactionRange.get() * interactionRange.get();
    }

    private BlockHitResult getPlacementHit(BlockPos pos, BlockState state, Direction preferredSide) {
        if (preferredSide != null) {
            BlockPos neighbor = pos.offset(preferredSide.getOpposite());
            if (canClickNeighbor(neighbor)) {
                Vec3d hitPos = adjustHitPos(pos, Vec3d.ofCenter(neighbor).add(preferredSide.getOffsetX() * 0.5, preferredSide.getOffsetY() * 0.5, preferredSide.getOffsetZ() * 0.5), state);
                return new BlockHitResult(hitPos, preferredSide, neighbor, false);
            }
        }

        Direction fallback = BlockUtils.getPlaceSide(pos);
        if (fallback == null) return null;

        BlockPos neighbor = pos.offset(fallback);
        Direction side = fallback.getOpposite();
        Vec3d hitPos = adjustHitPos(pos, Vec3d.ofCenter(neighbor).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5), state);
        return new BlockHitResult(hitPos, side, neighbor, false);
    }

    private Vec3d adjustHitPos(BlockPos target, Vec3d hitPos, BlockState state) {
        double y = hitPos.y;
        if (state.contains(Properties.SLAB_TYPE)) {
            SlabType type = state.get(Properties.SLAB_TYPE);
            if (type == SlabType.TOP) y = target.getY() + 0.78;
            else if (type == SlabType.BOTTOM) y = target.getY() + 0.22;
        } else if (state.contains(Properties.BLOCK_HALF)) {
            y = state.get(Properties.BLOCK_HALF) == BlockHalf.TOP ? target.getY() + 0.78 : target.getY() + 0.22;
        }

        return new Vec3d(hitPos.x, y, hitPos.z);
    }

    private boolean canClickNeighbor(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && !BlockUtils.isClickable(state.getBlock());
    }

    private boolean isLoadedAndBuildable(BlockPos pos) {
        return mc.world.isInBuildLimit(pos) && mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private RotationSpec rotationFor(BlockHitResult hit, RotationSpec stateRotation) {
        return switch (rotationMode.get()) {
            case None -> null;
            case Hit -> new RotationSpec(Rotations.getYaw(hit.getPos()), Rotations.getPitch(hit.getPos()));
            case Schematic -> stateRotation != null
                ? stateRotation.withFallbackPitch(Rotations.getPitch(hit.getPos()))
                : new RotationSpec(Rotations.getYaw(hit.getPos()), Rotations.getPitch(hit.getPos()));
        };
    }

    private RotationSpec getStateRotation(BlockState state) {
        if (rotationMode.get() != RotationMode.Schematic) return null;

        Direction direction = null;
        boolean reverse = false;
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            direction = state.get(Properties.HORIZONTAL_FACING);
            reverse = usesOppositeHorizontalPlacement(state);
        } else if (state.isOf(Blocks.OBSERVER) && state.contains(Properties.FACING)) {
            direction = state.get(Properties.FACING);
        } else if ((state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON)) && state.contains(Properties.FACING)) {
            direction = state.get(Properties.FACING);
            reverse = true;
        }

        if (direction == null) return null;
        if (reverse) direction = direction.getOpposite();

        return switch (direction) {
            case SOUTH -> new RotationSpec(0.0, Double.NaN);
            case WEST -> new RotationSpec(90.0, Double.NaN);
            case NORTH -> new RotationSpec(180.0, Double.NaN);
            case EAST -> new RotationSpec(-90.0, Double.NaN);
            case DOWN -> new RotationSpec(mc.player.getYaw(), 90.0);
            case UP -> new RotationSpec(mc.player.getYaw(), -90.0);
        };
    }

    private Direction placementSide(BlockState state) {
        if (state.contains(Properties.HORIZONTAL_FACING)) return Direction.UP;
        if (state.contains(Properties.FACING)) return state.get(Properties.FACING);
        if (state.contains(Properties.BLOCK_HALF)) return state.get(Properties.BLOCK_HALF) == BlockHalf.TOP ? Direction.DOWN : Direction.UP;
        if (state.contains(Properties.AXIS)) return Direction.from(state.get(Properties.AXIS), Direction.AxisDirection.POSITIVE);
        if (state.contains(Properties.HORIZONTAL_AXIS)) return Direction.from(state.get(Properties.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
        if (state.contains(Properties.SLAB_TYPE)) return state.get(Properties.SLAB_TYPE) == SlabType.TOP ? Direction.DOWN : Direction.UP;
        return Direction.DOWN;
    }

    private boolean usesOppositeHorizontalPlacement(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block instanceof AbstractRedstoneGateBlock;
    }

    private boolean isDoubleSlabCompletion(BlockState worldState, BlockState schematicState) {
        if (!worldState.isIn(BlockTags.SLABS) || !schematicState.isIn(BlockTags.SLABS)) return false;
        if (worldState.getBlock() != schematicState.getBlock()) return false;
        if (!worldState.contains(Properties.SLAB_TYPE) || !schematicState.contains(Properties.SLAB_TYPE)) return false;

        return schematicState.get(Properties.SLAB_TYPE) == SlabType.DOUBLE
            && worldState.get(Properties.SLAB_TYPE) != SlabType.DOUBLE;
    }

    private Direction slabCompletionSide(BlockState worldState) {
        return worldState.get(Properties.SLAB_TYPE) == SlabType.BOTTOM ? Direction.UP : Direction.DOWN;
    }

    private Direction findIgniteSide(BlockPos portalPos) {
        Direction[] sides = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction side : sides) {
            BlockPos clicked = portalPos.offset(side.getOpposite());
            if (canClickNeighbor(clicked)) return side;
        }

        return null;
    }

    private boolean isDataBlock(Block block) {
        return block == Blocks.STRUCTURE_VOID
            || block == Blocks.STRUCTURE_BLOCK
            || block == Blocks.JIGSAW
            || block == Blocks.BARRIER
            || block == Blocks.COMMAND_BLOCK
            || block == Blocks.CHAIN_COMMAND_BLOCK
            || block == Blocks.REPEATING_COMMAND_BLOCK;
    }

    private void tickCooldowns() {
        skipCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 1);
        skipCooldowns.replaceAll((pos, ticks) -> ticks - 1);

        List<BlockPos> done = new ArrayList<>();
        for (Map.Entry<BlockPos, PendingAttempt> entry : pendingAttempts.entrySet()) {
            PendingAttempt attempt = entry.getValue().tick();
            if (attempt.remainingTicks() > 0) {
                entry.setValue(attempt);
                continue;
            }

            BlockPos pos = entry.getKey();
            BlockState worldState = mc.world.getBlockState(pos);
            if (!stillNeedsWork(worldState, attempt.state())) {
                failedAttempts.remove(pos);
                done.add(pos);
                continue;
            }

            int failures = failedAttempts.merge(pos, 1, Integer::sum);
            if (failures >= maxRetries.get()) {
                skipCooldowns.put(new BlockPos(pos), skipImpossibleTicks.get());
                failedAttempts.remove(pos);
                if (debug.get()) info("Skipping " + pos.toShortString() + " after " + failures + " failed verification checks.");
            }
            done.add(pos);
        }

        for (BlockPos pos : done) pendingAttempts.remove(pos);
    }

    private boolean isTemporarilyBlocked(BlockPos pos) {
        return pendingAttempts.containsKey(pos) || skipCooldowns.containsKey(pos);
    }

    private int effectiveBlocksPerTick() {
        int configured = blocksPerTick.get();
        int ping = currentPing();
        if (ping <= LOW_LATENCY_MS) return clamp(Math.max(configured, 3), 1, 4);
        if (ping <= MID_LATENCY_MS) return clamp(Math.max(configured, 2), 1, 3);
        return Math.min(configured, 2);
    }

    private int effectiveTickDelay() {
        int configured = tickDelay.get();
        int ping = currentPing();
        if (ping <= LOW_LATENCY_MS) return 0;
        if (ping <= MID_LATENCY_MS) return Math.min(configured, 1);
        return configured;
    }

    private int effectiveRetryDelay() {
        int configured = Math.max(1, retryDelay.get());
        int ping = currentPing();
        if (ping <= LOW_LATENCY_MS) return Math.min(configured, 3);
        if (ping <= MID_LATENCY_MS) return Math.min(configured, 5);
        return configured;
    }

    private int effectiveMoveCooldown() {
        int configured = moveCooldownSetting.get();
        if (currentPing() <= MID_LATENCY_MS) return Math.min(configured, 2);
        return configured;
    }

    private int currentPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return DEFAULT_PING_MS;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? Math.max(0, entry.getLatency()) : DEFAULT_PING_MS;
    }

    private void markAttempt(BlockPos pos, BlockState state) {
        pendingAttempts.put(new BlockPos(pos), new PendingAttempt(state, effectiveRetryDelay()));
    }

    private void markImpossible(BlockPos pos) {
        int retry = effectiveRetryDelay();
        int failures = failedAttempts.merge(new BlockPos(pos), 1, Integer::sum);
        if (failures >= maxRetries.get()) {
            skipCooldowns.put(new BlockPos(pos), skipImpossibleTicks.get());
            failedAttempts.remove(pos);
        } else if (retry > 0) {
            skipCooldowns.put(new BlockPos(pos), Math.max(1, retry / 2));
        }
    }

    private void handleMissingItem(Candidate candidate) {
        markImpossible(candidate.pos);
        if (pauseWhenMissingBlocks.get()) {
            missingItemCooldown = Math.max(missingItemCooldown, Math.max(4, effectiveRetryDelay()));
            if (debug.get()) info("Missing item for " + candidate.state.getBlock().getName().getString() + " at " + candidate.pos.toShortString() + ".");
        }
    }

    private void warnBridgeError() {
        if (!warnedBridgeError && bridge.getLastError() != null) {
            warning("Litematica bridge failed: " + bridge.getLastError());
            warnedBridgeError = true;
        }
    }

    public static boolean shouldOverrideExperienceBar() {
        return activeInstance != null
            && activeInstance.isActive()
            && activeInstance.xpProgressBar.get()
            && activeInstance.progressSnapshot.visible();
    }

    public static void renderExperienceProgressBar(DrawContext context) {
        if (!shouldOverrideExperienceBar()) return;

        ProgressSnapshot snapshot = activeInstance.progressSnapshot;
        int x = (context.getScaledWindowWidth() - XP_BAR_WIDTH) / 2;
        int y = context.getScaledWindowHeight() - 24 - XP_BAR_HEIGHT;
        int fill = clamp((int) Math.round(snapshot.progress() * XP_BAR_WIDTH), 0, XP_BAR_WIDTH);

        context.fill(x - 2, y - 2, x + XP_BAR_WIDTH + 2, y + XP_BAR_HEIGHT + 2, 0xAA000000);
        context.fill(x, y, x + XP_BAR_WIDTH, y + XP_BAR_HEIGHT, 0xE0181B20);
        if (fill > 0) {
            context.fillGradient(x, y, x + fill, y + XP_BAR_HEIGHT, 0xFF21D6C9, 0xFFA8FF4D);
            context.fill(x + 1, y + 1, Math.max(x + 1, x + fill - 1), y + 2, 0x88FFFFFF);
        }
    }

    public static void renderExperienceProgressText(DrawContext context, TextRenderer textRenderer) {
        if (!shouldOverrideExperienceBar()) return;

        String label = activeInstance.progressSnapshot.label();
        int width = textRenderer.getWidth(label);
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = context.getScaledWindowHeight() - 38;

        context.fill(x - 4, y - 2, x + width + 4, y + 10, 0x99000000);
        context.drawTextWithShadow(textRenderer, label, x, y, 0xFFEAF7F2);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private final class ProgressTracker {
        private int identity = -1;
        private List<BlockBox> boxes = List.of();
        private ProgressSnapshot lastComplete = ProgressSnapshot.empty();
        private int boxIndex;
        private int x;
        private int y;
        private int z;
        private int passDone;
        private int passTotal;
        private int passScanned;

        private void reset() {
            identity = -1;
            boxes = List.of();
            lastComplete = ProgressSnapshot.empty();
            boxIndex = 0;
            passDone = 0;
            passTotal = 0;
            passScanned = 0;
        }

        private void start(SelectedPlacement selected) {
            identity = selected.identity();
            boxes = List.copyOf(selected.boxes());
            boxIndex = 0;
            passDone = 0;
            passTotal = 0;
            passScanned = 0;
            if (!boxes.isEmpty()) setCursor(boxes.get(0));
        }

        private ProgressSnapshot scan(SelectedPlacement selected, int limit) {
            if (identity != selected.identity()) start(selected);
            if (boxes.isEmpty()) return ProgressSnapshot.empty();

            int scannedThisTick = 0;
            while (scannedThisTick < limit && boxIndex < boxes.size()) {
                BlockBox box = boxes.get(boxIndex);
                BlockPos pos = new BlockPos(x, y, z);
                BlockState schematicState = bridge.getSchematicState(pos);

                if (schematicState != null && !schematicState.isAir() && !(ignoreDataBlocks.get() && isDataBlock(schematicState.getBlock()))) {
                    passTotal++;
                    if (isCompleteForProgress(pos, schematicState)) passDone++;
                }

                passScanned++;
                scannedThisTick++;
                advance(box);
            }

            if (boxIndex >= boxes.size()) {
                lastComplete = new ProgressSnapshot(passDone, passTotal, passScanned, true, true);
                start(selected);
                return lastComplete;
            }

            if (lastComplete.visible()) return lastComplete.asUpdating();
            return new ProgressSnapshot(passDone, passTotal, passScanned, false, passScanned > 0);
        }

        private void setCursor(BlockBox box) {
            x = box.minX();
            y = box.minY();
            z = box.minZ();
        }

        private void advance(BlockBox box) {
            x++;
            if (x <= box.maxX()) return;

            x = box.minX();
            z++;
            if (z <= box.maxZ()) return;

            z = box.minZ();
            y++;
            if (y <= box.maxY()) return;

            boxIndex++;
            if (boxIndex < boxes.size()) setCursor(boxes.get(boxIndex));
        }
    }

    private record ProgressSnapshot(int done, int total, int scanned, boolean completePass, boolean visible) {
        private static ProgressSnapshot empty() {
            return new ProgressSnapshot(0, 0, 0, false, false);
        }

        private double progress() {
            if (total <= 0) return completePass ? 1.0 : 0.0;
            return Math.max(0.0, Math.min(1.0, (double) done / total));
        }

        private String label() {
            if (total <= 0 && !completePass) return "Scanning schematic";
            if (total <= 0) return "No printable blocks";

            int percent = clamp((int) Math.round(progress() * 100.0), 0, 100);
            String prefix = completePass ? "Build " : "Scanning ";
            return prefix + percent + "%  " + done + "/" + total;
        }

        private ProgressSnapshot asUpdating() {
            return new ProgressSnapshot(done, total, scanned, false, visible);
        }
    }

    private record SelectedPlacement(int identity, List<BlockBox> boxes) {
    }

    private record BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    private enum RotationMode {
        Schematic,
        Hit,
        None
    }

    private enum BuildOrder {
        Nearest,
        StableSupport,
        Layers
    }

    private enum BreakMode {
        Off,
        NonAir,
        All
    }

    private enum PortalIgniteMode {
        Off,
        SaveDurability,
        UseAny
    }

    private record Candidate(BlockPos pos, BlockState state, double distanceSq, int supportScore, boolean breakOnly, boolean doubleSlab, boolean portalIgnite, boolean configureOnly) {
    }

    private record RotationSpec(double yaw, double pitch) {
        private RotationSpec withFallbackPitch(double fallbackPitch) {
            return Double.isNaN(pitch) ? new RotationSpec(yaw, fallbackPitch) : this;
        }
    }

    private record PendingAttempt(BlockState state, int remainingTicks) {
        private PendingAttempt tick() {
            return new PendingAttempt(state, remainingTicks - 1);
        }
    }

    private static final class LitematicaBridge {
        private static final String HANDLER_CLASS = "fi.dy.masa.litematica.world.SchematicWorldHandler";
        private static final String DATA_MANAGER_CLASS = "fi.dy.masa.litematica.data.DataManager";
        private static final String REQUIRED_ENABLED_CLASS = "fi.dy.masa.litematica.schematic.placement.SubRegionPlacement$RequiredEnabled";

        private Method getSchematicWorld;
        private Method getSchematicPlacementManager;
        private Object placementEnabled;
        private final Map<Class<?>, Method> getBlockStateMethods = new HashMap<>();
        private final Map<Class<?>, Method> getSelectedPlacementMethods = new HashMap<>();
        private final Map<Class<?>, Method> getSubRegionBoxesMethods = new HashMap<>();
        private final Map<Class<?>, Method> getPos1Methods = new HashMap<>();
        private final Map<Class<?>, Method> getPos2Methods = new HashMap<>();
        private String lastError;
        private boolean resolved;
        private boolean placementResolved;

        private void clearError() {
            lastError = null;
        }

        private String getLastError() {
            return lastError;
        }

        private BlockState getSchematicState(BlockPos pos) {
            try {
                if (!resolve()) return null;

                Object world = getSchematicWorld.invoke(null);
                if (world == null) return null;

                Method method = getBlockStateMethods.computeIfAbsent(world.getClass(), this::findGetBlockStateMethod);
                if (method == null) {
                    lastError = "WorldSchematic#getBlockState was not found.";
                    return null;
                }

                Object value = method.invoke(world, pos);
                if (value instanceof BlockState state) return state;

                lastError = "WorldSchematic#getBlockState returned " + (value == null ? "null" : value.getClass().getName());
                return null;
            } catch (ReflectiveOperationException | RuntimeException e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                return null;
            }
        }

        private SelectedPlacement getSelectedPlacement() {
            try {
                Object manager = getPlacementManager();
                if (manager == null) return null;

                Method selectedMethod = getSelectedPlacementMethods.computeIfAbsent(manager.getClass(), clazz -> findMethod(clazz, "getSelectedSchematicPlacement", 0));
                if (selectedMethod == null) return null;

                Object placement = selectedMethod.invoke(manager);
                if (placement == null) return null;

                Method boxesMethod = getSubRegionBoxesMethods.computeIfAbsent(placement.getClass(), clazz -> findMethod(clazz, "getSubRegionBoxes", 1));
                if (boxesMethod == null) return null;

                Object value = boxesMethod.invoke(placement, placementEnabled);
                if (!(value instanceof Map<?, ?> map)) return null;

                List<BlockBox> boxes = new ArrayList<>();
                for (Object box : map.values()) {
                    BlockBox bounds = toBlockBox(box);
                    if (bounds != null) boxes.add(bounds);
                }

                int identity = 31 * System.identityHashCode(placement) + boxes.hashCode();
                return new SelectedPlacement(identity, boxes);
            } catch (ReflectiveOperationException | RuntimeException e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                return null;
            }
        }

        private boolean resolve() throws ClassNotFoundException, NoSuchMethodException {
            if (resolved) return true;
            Class<?> handler = Class.forName(HANDLER_CLASS);
            getSchematicWorld = handler.getMethod("getSchematicWorld");
            resolved = true;
            return true;
        }

        private Object getPlacementManager() throws ClassNotFoundException, NoSuchMethodException, ReflectiveOperationException {
            if (!placementResolved) {
                Class<?> dataManager = Class.forName(DATA_MANAGER_CLASS);
                Class<?> requiredEnabledClass = Class.forName(REQUIRED_ENABLED_CLASS);
                getSchematicPlacementManager = dataManager.getMethod("getSchematicPlacementManager");
                placementEnabled = enumValue(requiredEnabledClass, "PLACEMENT_ENABLED");
                placementResolved = true;
            }

            return getSchematicPlacementManager.invoke(null);
        }

        private Method findGetBlockStateMethod(Class<?> worldClass) {
            for (Method method : worldClass.getMethods()) {
                if (method.getParameterCount() == 1
                    && (method.getName().equals("getBlockState") || method.getName().equals("method_8320"))) {
                    method.setAccessible(true);
                    return method;
                }
            }

            return null;
        }

        private BlockBox toBlockBox(Object box) throws ReflectiveOperationException {
            if (box == null) return null;

            Method pos1Method = getPos1Methods.computeIfAbsent(box.getClass(), clazz -> findMethod(clazz, "getPos1", 0));
            Method pos2Method = getPos2Methods.computeIfAbsent(box.getClass(), clazz -> findMethod(clazz, "getPos2", 0));
            if (pos1Method == null || pos2Method == null) return null;

            Object pos1Object = pos1Method.invoke(box);
            Object pos2Object = pos2Method.invoke(box);
            if (!(pos1Object instanceof BlockPos pos1) || !(pos2Object instanceof BlockPos pos2)) return null;

            int minX = Math.min(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());
            return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private Method findMethod(Class<?> clazz, String name, int parameters) {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameters) {
                    method.setAccessible(true);
                    return method;
                }
            }

            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private Object enumValue(Class<?> enumClass, String name) {
            return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
        }
    }
}

