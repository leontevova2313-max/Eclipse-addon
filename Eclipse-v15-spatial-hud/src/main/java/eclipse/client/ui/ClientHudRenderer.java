package eclipse.client.ui;

import eclipse.client.hud.HudWidgetBinding;
import eclipse.client.perf.PerfSnapshot;
import eclipse.client.runtime.ClientRuntime;
import eclipse.gui.client.EclipseClientTheme;
import eclipse.modules.visuals.CrosshairInfo;
import eclipse.modules.visuals.LightMeter;
import eclipse.modules.visuals.ScreenshotGrid;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ClientHudRenderer {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int HUD_SIDE_MARGIN = 20;
    private static final int HUD_TOP_MARGIN = 14;
    private static final int PANEL_TEXT_X = 6;
    private static final int PANEL_TEXT_Y = 4;
    private static long lastFrameAtNs;

    private ClientHudRenderer() {
    }

    public static void render(DrawContext context, MinecraftClient client) {
        long start = System.nanoTime();
        if (lastFrameAtNs != 0L) ClientRuntime.sampleFrame(start - lastFrameAtNs);
        lastFrameAtNs = start;
        renderWidgets(context, client);
        renderModuleOverlays(context, client);
        ClientRuntime.sampleHud(System.nanoTime() - start);
    }

    private static void renderWidgets(DrawContext context, MinecraftClient client) {
        List<HudWidgetBinding> widgets = ClientRuntime.hud().visibleWidgets(moduleId -> true);
        for (HudWidgetBinding binding : widgets) {
            switch (binding.widgetId()) {
                case "coordinates-hud" -> renderCoordinates(context, client, binding);
                case "clock-hud" -> renderClock(context, client, binding);
                case "nearest-waypoint" -> renderNearestWaypoint(context, client, binding);
                case "active-modules" -> renderActiveModules(context, client, binding);
                case "debug-overlay" -> renderPerf(context, client, binding);
                default -> {
                }
            }
        }
    }

    private static void renderCoordinates(DrawContext context, MinecraftClient client, HudWidgetBinding binding) {
        if (client.player == null) return;
        String coords = String.format("XYZ %.1f / %.1f / %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
        int width = Math.max(136, client.textRenderer.getWidth(coords) + 10);
        int height = 16;
        int x = clampHudX(context, binding.x(), width);
        int y = clampHudY(context, binding.y(), height);
        EclipseClientTheme.drawHudPanel(context, x, y, width, height);
        EclipseClientTheme.drawText(client.textRenderer, context, coords, x + PANEL_TEXT_X, y + PANEL_TEXT_Y);
    }

    private static void renderClock(DrawContext context, MinecraftClient client, HudWidgetBinding binding) {
        String value = LocalTime.now().format(CLOCK_FORMAT);
        int width = Math.max(76, client.textRenderer.getWidth(value) + 10);
        int height = 16;
        int x = clampHudX(context, binding.x(), width);
        int y = clampHudY(context, binding.y(), height);
        EclipseClientTheme.drawHudPanel(context, x, y, width, height);
        EclipseClientTheme.drawText(client.textRenderer, context, value, x + PANEL_TEXT_X, y + PANEL_TEXT_Y);
    }

    private static void renderNearestWaypoint(DrawContext context, MinecraftClient client, HudWidgetBinding binding) {
        var waypoint = ClientRuntime.spatial().nearestVisibleWaypoint();
        if (waypoint == null) return;
        double dx = client.player == null ? 0.0 : client.player.getX() - waypoint.x;
        double dz = client.player == null ? 0.0 : client.player.getZ() - waypoint.z;
        int blocks = (int) Math.sqrt(dx * dx + dz * dz);
        String text = "WP " + trim(waypoint.name, 14) + " - " + blocks + "m";
        int width = Math.max(118, client.textRenderer.getWidth(text) + 10);
        int height = 16;
        int x = clampHudX(context, binding.x(), width);
        int y = clampHudY(context, binding.y(), height);
        EclipseClientTheme.drawHudPanel(context, x, y, width, height);
        EclipseClientTheme.drawText(client.textRenderer, context, text, x + PANEL_TEXT_X, y + PANEL_TEXT_Y);
    }

    private static void renderPerf(DrawContext context, MinecraftClient client, HudWidgetBinding binding) {
        PerfSnapshot snapshot = ClientRuntime.perf().snapshot();
        String frame = "frame avg: " + nsToMs(snapshot.frameAvgNs);
        String tick = "tick avg: " + nsToMs(snapshot.tickAvgNs);
        String mem = "mem: " + memoryPercent(snapshot) + "% - wp " + snapshot.waypointCount;
        int width = Math.max(144, Math.max(client.textRenderer.getWidth(frame), Math.max(client.textRenderer.getWidth(tick), client.textRenderer.getWidth(mem))) + 10);
        int height = 42;
        int x = clampHudX(context, binding.x(), width);
        int y = clampHudY(context, binding.y(), height);
        EclipseClientTheme.drawHudPanel(context, x, y, width, height);
        EclipseClientTheme.drawText(client.textRenderer, context, "Perf", x + PANEL_TEXT_X, y + 3);
        EclipseClientTheme.drawMutedText(client.textRenderer, context, frame, x + PANEL_TEXT_X, y + 13);
        EclipseClientTheme.drawMutedText(client.textRenderer, context, tick, x + PANEL_TEXT_X, y + 22);
        EclipseClientTheme.drawMutedText(client.textRenderer, context, mem, x + PANEL_TEXT_X, y + 31);
    }

    private static void renderActiveModules(DrawContext context, MinecraftClient client, HudWidgetBinding binding) {
        List<Module> modules = ClientRuntime.modules().modules(eclipse.client.runtime.ClientSection.OVERVIEW);
        int visible = 0;
        int maxNameWidth = client.textRenderer.getWidth("Active");
        for (Module module : modules) {
            if (!module.isActive()) continue;
            visible++;
            if (visible <= 8) maxNameWidth = Math.max(maxNameWidth, client.textRenderer.getWidth(module.title));
        }

        if (visible <= 0) return;

        int rows = Math.min(visible, 8);
        int width = Math.max(118, maxNameWidth + 18);
        int height = 14 + rows * 10;
        int x = clampHudX(context, binding.x(), width);
        int y = clampHudY(context, binding.y(), height);

        EclipseClientTheme.drawHudPanel(context, x, y, width, height);
        EclipseClientTheme.drawText(client.textRenderer, context, "Active", x + PANEL_TEXT_X, y + 3);

        int lineY = y + 14;
        int drawn = 0;
        for (Module module : modules) {
            if (!module.isActive()) continue;
            if (drawn >= 8) break;
            EclipseClientTheme.drawMutedText(client.textRenderer, context, trim(module.title, 18), x + PANEL_TEXT_X, lineY);
            lineY += 10;
            drawn++;
        }
    }

    private static void renderModuleOverlays(DrawContext context, MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        ScreenshotGrid grid = Modules.get().get(ScreenshotGrid.class);
        if (grid != null && grid.isActive()) renderScreenshotGrid(context, grid);
        LightMeter lightMeter = Modules.get().get(LightMeter.class);
        if (lightMeter != null && lightMeter.isActive()) renderLightMeter(context, client, lightMeter);
        CrosshairInfo crosshairInfo = Modules.get().get(CrosshairInfo.class);
        if (crosshairInfo != null && crosshairInfo.isActive()) renderCrosshairInfo(context, client, crosshairInfo);
    }

    private static void renderScreenshotGrid(DrawContext context, ScreenshotGrid grid) {
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int line = EclipseClientTheme.alpha(0xFFFFFFFF, 0.14f);
        for (int i = 1; i < grid.columns(); i++) {
            int x = (sw * i) / grid.columns();
            context.fill(x, 0, x + 1, sh, line);
        }
        for (int i = 1; i < grid.rows(); i++) {
            int y = (sh * i) / grid.rows();
            context.fill(0, y, sw, y + 1, line);
        }
        if (grid.centerCross()) {
            int cx = sw / 2;
            int cy = sh / 2;
            context.fill(cx - 10, cy, cx + 11, cy + 1, line);
            context.fill(cx, cy - 10, cx + 1, cy + 11, line);
        }
    }

    private static void renderLightMeter(DrawContext context, MinecraftClient client, LightMeter lightMeter) {
        if (!(client.crosshairTarget instanceof BlockHitResult bhr)) return;
        BlockPos pos = bhr.getBlockPos();
        if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return;
        int panelWidth = 112;
        int panelHeight = lightMeter.showBlock() && lightMeter.showSky() ? 36 : 24;
        int x = context.getScaledWindowWidth() - HUD_SIDE_MARGIN - panelWidth;
        int y = HUD_TOP_MARGIN;
        EclipseClientTheme.drawHudPanel(context, x, y, panelWidth, panelHeight);
        EclipseClientTheme.drawText(client.textRenderer, context, "Light Meter", x + PANEL_TEXT_X, y + 4);
        int lineY = y + 14;
        if (lightMeter.showBlock()) {
            int block = client.world.getLightLevel(pos);
            EclipseClientTheme.drawMutedText(client.textRenderer, context, "Block: " + block, x + PANEL_TEXT_X, lineY);
            lineY += 9;
        }
        if (lightMeter.showSky()) {
            int sky = client.world.getLightLevel(net.minecraft.world.LightType.SKY, pos);
            String status = sky >= lightMeter.safeThreshold() ? "safe" : "low";
            EclipseClientTheme.drawMutedText(client.textRenderer, context, "Sky: " + sky + " (" + status + ")", x + PANEL_TEXT_X, lineY);
        }
    }

    private static void renderCrosshairInfo(DrawContext context, MinecraftClient client, CrosshairInfo crosshairInfo) {
        HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult bhr)) return;
        BlockPos pos = bhr.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        String blockName = state.getBlock().getName().getString();
        int width = Math.max(102, client.textRenderer.getWidth(blockName) + 10);
        int height = crosshairInfo.showPosition() ? 24 : 16;
        int x = Math.min(context.getScaledWindowWidth() / 2 + 12, context.getScaledWindowWidth() - HUD_SIDE_MARGIN - width);
        int y = Math.min(context.getScaledWindowHeight() / 2 + 14, context.getScaledWindowHeight() - 24 - height);
        EclipseClientTheme.drawHudPanel(context, x, y, width, height);
        if (crosshairInfo.showBlockName()) {
            EclipseClientTheme.drawMutedText(client.textRenderer, context, trim(blockName, 22), x + PANEL_TEXT_X, y + 4);
        }
        if (crosshairInfo.showPosition()) {
            EclipseClientTheme.drawFaintText(client.textRenderer, context, pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), x + PANEL_TEXT_X, y + 13);
        }
    }

    private static int clampHudX(DrawContext context, int x, int width) {
        return Math.max(HUD_SIDE_MARGIN, Math.min(x, context.getScaledWindowWidth() - HUD_SIDE_MARGIN - width));
    }

    private static int clampHudY(DrawContext context, int y, int height) {
        return Math.max(HUD_TOP_MARGIN, Math.min(y, context.getScaledWindowHeight() - 24 - height));
    }

    private static String trim(String input, int max) {
        return input.length() <= max ? input : input.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String nsToMs(long ns) {
        return String.format("%.2f ms", ns / 1_000_000.0);
    }

    private static int memoryPercent(PerfSnapshot snapshot) {
        if (snapshot.maxMemoryBytes <= 0) return 0;
        return (int) ((snapshot.usedMemoryBytes * 100L) / snapshot.maxMemoryBytes);
    }
}

