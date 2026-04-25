package eclipse.gui.client;

import eclipse.client.hud.HudWidgetBinding;
import eclipse.client.perf.PerfSnapshot;
import eclipse.client.runtime.ClientRuntime;
import eclipse.client.runtime.ClientSection;
import eclipse.client.spatial.RouteRecord;
import eclipse.client.spatial.WaypointRecord;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class EclipseClientScreen extends Screen {
    private static final int ROOT_MARGIN = 14;
    private static final int SIDEBAR_W = 0;
    private static final int GUTTER = 12;
    private static final int HEADER_H = 46;
    private static final int ROW_H = 32;
    private static final int ROW_GAP = 5;
    private static final int FOOTER_H = 22;
    private static final int TOP_BAR_H = 28;
    private static final int PERF_SAMPLE_INTERVAL = 4;
    private static final int SETTING_ROW_H = 22;
    private static final long GUI_ANIMATION_MS = 200L;
    private static final double BASE_UI_SCALE = 2.0;

    private enum ViewMode {
        WORKSPACE("Workspace"),
        SPATIAL("Spatial"),
        HUD("HUD Editor"),
        INSPECTOR("Inspector");

        final String title;

        ViewMode(String title) {
            this.title = title;
        }
    }

    private final Screen parent;
    private ViewMode viewMode = ViewMode.WORKSPACE;
    private ClientSection section = ClientSection.VISUALS;
    private Module selectedModule;
    private String selectedWaypointId;
    private String selectedRouteId;
    private boolean inspectRouteDetails;
    private TextFieldWidget searchField;
    private int moduleScroll;
    private int settingsScroll;
    private int hudSelectedIndex;
    private int inspectorMode;
    private int perfTicker;
    private int spatialWaypointScroll;
    private int spatialRouteScroll;
    private int draggingHudIndex = -1;
    private int dragOffsetX;
    private int dragOffsetY;
    private final int hudSnap = 8;
    private boolean macDock = true;
    private boolean scriptLabels = true;
    private boolean dockMagnify = true;
    private boolean alertRails = true;
    private Module bindingModule;

    private final List<Module> cachedModules = new ArrayList<>(64);
    private final List<SettingEntry> cachedSettings = new ArrayList<>(64);
    private final List<Rect> cachedModuleRows = new ArrayList<>(16);
    private final List<SidebarModeEntry> cachedSidebarModes = new ArrayList<>(4);
    private final List<SidebarSectionEntry> cachedSidebarSections = new ArrayList<>(8);
    private final List<HeaderChip> cachedHeaderChips = new ArrayList<>(8);
    private final List<ViewChip> cachedViewChips = new ArrayList<>(4);
    private final List<ModuleColumnEntry> cachedModuleColumnRows = new ArrayList<>(64);
    private final List<HudWidgetDraft> hudWidgets = new ArrayList<>(12);
    private boolean modulesDirty = true;
    private boolean settingsDirty = true;
    private boolean layoutDirty = true;
    private long openedAtMs;
    private long closingStartedAtMs;
    private boolean closing;

    private final int[] fpsSamples = new int[48];
    private final int[] moduleSamples = new int[48];
    private final int[] memorySamples = new int[48];
    private int perfSamples;

    public EclipseClientScreen(Screen parent) {
        super(Text.literal("Eclipse Client"));
        this.parent = parent;
        this.openedAtMs = System.currentTimeMillis();
    }

    public static void open(Screen parent) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null) return;
        client.setScreen(new EclipseClientScreen(parent));
    }

    public static void toggle() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null) return;
        if (client.currentScreen instanceof EclipseClientScreen screen) screen.close();
        else client.setScreen(new EclipseClientScreen(client.currentScreen));
    }

    @Override
    protected void init() {
        ClientRuntime.bootstrap();
        layoutDirty = true;
        rebuildLayout();
        searchField = new TextFieldWidget(textRenderer, modulesPanelRect().x + 18, modulesPanelRect().y + 42, modulesPanelRect().w - 90, 24, Text.literal("Search"));
        searchField.setDrawsBackground(false);
        searchField.setPlaceholder(Text.literal("Search Eclipse modules"));
        searchField.setChangedListener(value -> {
            moduleScroll = 0;
            modulesDirty = true;
            settingsDirty = true;
        });
        searchField.setMaxLength(64);
        ensureHudWidgets();
        modulesDirty = true;
        settingsDirty = true;
        ensureSelection();
    }

    @Override
    public void tick() {
        ensureCaches();
        if (bindingModule != null && !Modules.get().isBinding()) bindingModule = null;
        if (++perfTicker >= PERF_SAMPLE_INTERVAL) {
            perfTicker = 0;
            samplePerformance();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        closingStartedAtMs = System.currentTimeMillis();
    }

    private void finishClose() {
        syncHudWidgetsToRuntime();
        ClientRuntime.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.getKeycode() == 256) {
            if (bindingModule != null) bindingModule = null;
            close();
            return true;
        }
        if (bindingModule != null && selectedModule != null && selectedModule != bindingModule) bindingModule = null;
        if (searchField != null && viewMode == ViewMode.WORKSPACE && searchField.keyPressed(keyInput)) return true;
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput chr) {
        return viewMode == ViewMode.WORKSPACE && searchField != null && searchField.charTyped(chr) || super.charTyped(chr);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (closing) return true;
        double mx = toUiX(click.x());
        double my = toUiY(click.y());

        if (false && (viewMode == ViewMode.WORKSPACE || viewMode == ViewMode.SPATIAL) && searchField != null) {
            Rect searchBounds = new Rect(searchField.getX(), searchField.getY(), searchField.getWidth(), searchField.getHeight());
            if (searchBounds.contains(mx, my)) {
                searchField.setFocused(true);
                return true;
            }
            searchField.setFocused(false);
        }

        for (SidebarModeEntry entry : cachedSidebarModes) {
            if (entry.bounds.contains(mx, my)) {
                viewMode = entry.mode;
                moduleScroll = 0;
                settingsDirty = true;
                layoutDirty = true;
                return true;
            }
        }

        Rect themeChip = themeChipRect();
        if (themeChip.contains(mx, my)) {
            ClientRuntime.theme().cycle();
            return true;
        }

        Rect closeChip = closeChipRect();
        if (closeChip.contains(mx, my)) {
            close();
            return true;
        }

        if (viewMode == ViewMode.WORKSPACE) {
            for (ModuleColumnEntry entry : cachedModuleColumnRows) {
                Rect moduleToggle = moduleColumnToggleRect(entry.bounds);
                Rect arrow = moduleArrowRect(entry.bounds);
                if (moduleToggle.contains(mx, my)) {
                    entry.module.toggle();
                    selectedModule = entry.module;
                    settingsScroll = 0;
                    settingsDirty = true;
                    return true;
                }
                if (entry.bounds.contains(mx, my) || arrow.contains(mx, my)) {
                    selectedModule = entry.module;
                    settingsScroll = 0;
                    settingsDirty = true;
                    return true;
                }
            }

            if (selectedModule != null) {
                Rect closeDetails = detailCloseRect();
                if (closeDetails.contains(mx, my)) {
                    selectedModule = null;
                    cachedSettings.clear();
                    return true;
                }
                Rect moduleToggle = detailToggleRect();
                if (moduleToggle.contains(mx, my)) {
                    selectedModule.toggle();
                    return true;
                }
                Rect bindRect = detailBindRect();
                Rect clearBindRect = detailBindClearRect();
                if (bindRect.contains(mx, my)) {
                    Modules.get().setModuleToBind(selectedModule);
                    Modules.get().awaitKeyRelease();
                    bindingModule = selectedModule;
                    return true;
                }
                if (clearBindRect.contains(mx, my)) {
                    selectedModule.keybind.set(Keybind.none());
                    bindingModule = null;
                    return true;
                }
                if (clickSetting(mx, my)) return true;
            }
        }
        else if (viewMode == ViewMode.SPATIAL) {
            if (clickSpatial(mx, my)) return true;
        }
        else if (viewMode == ViewMode.HUD) {
            if (clickHud(mx, my)) return true;
        }
        else if (viewMode == ViewMode.INSPECTOR) {
            if (clickInspector(mx, my)) return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) return true;
        mouseX = toUiX(mouseX);
        mouseY = toUiY(mouseY);
        if (viewMode == ViewMode.WORKSPACE) {
            Rect detailsPanel = detailsPanelRect();

            if (detailsPanel.contains(mouseX, mouseY) && selectedModule != null) {
                int maxScroll = Math.max(0, cachedSettings.size() - 9);
                settingsScroll = MathHelper.clamp(settingsScroll - (int) Math.signum(verticalAmount), 0, maxScroll);
                return true;
            }
        }
        else if (viewMode == ViewMode.SPATIAL) {
            Rect waypoints = spatialWaypointsRect();
            Rect routes = spatialRoutesRect();
            if (waypoints.contains(mouseX, mouseY)) {
                int maxScroll = Math.max(0, ClientRuntime.spatial().waypoints().size() - 9);
                spatialWaypointScroll = MathHelper.clamp(spatialWaypointScroll - (int) Math.signum(verticalAmount), 0, maxScroll);
                return true;
            }
            if (routes.contains(mouseX, mouseY)) {
                int maxScroll = Math.max(0, ClientRuntime.spatial().routes().size() - 6);
                spatialRouteScroll = MathHelper.clamp(spatialRouteScroll - (int) Math.signum(verticalAmount), 0, maxScroll);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (closing) return true;
        if (viewMode == ViewMode.HUD && draggingHudIndex >= 0) {
            HudWidgetDraft selected = hudWidgets.get(draggingHudIndex);
            Rect placement = hudPlacementRect();
            double sx = Math.max(0.0001, placement.w / (double) Math.max(1, hudScreenWidth()));
            double sy = Math.max(0.0001, placement.h / (double) Math.max(1, hudScreenHeight()));
            int targetX = (int) Math.round((((int) toUiX(click.x()) - placement.x - dragOffsetX) / sx));
            int targetY = (int) Math.round((((int) toUiY(click.y()) - placement.y - dragOffsetY) / sy));
            selected.x = snap(MathHelper.clamp(targetX, 0, Math.max(0, hudScreenWidth() - selected.w)), hudSnap);
            selected.y = snap(MathHelper.clamp(targetY, 0, Math.max(0, hudScreenHeight() - selected.h)), hudSnap);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingHudIndex >= 0) {
            draggingHudIndex = -1;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long startedAt = System.nanoTime();
        ensureCaches();

        float animation = animationProgress();
        if (closing && animation >= 1.0f) {
            finishClose();
            return;
        }

        float alpha = closing ? 1.0f - animation : animation;
        float eased = smooth(alpha);
        float animatedScale = 0.965f + eased * 0.035f;
        EclipseClientTheme.renderAlpha(eased);

        EclipseClientTheme.drawWorkspaceBackground(context, width, height);

        float drawScale = (float) drawScale();
        int uiMouseX = (int) toUiX(mouseX);
        int uiMouseY = (int) toUiY(mouseY);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(drawScale, drawScale);
        float cx = uiWidth() * 0.5f;
        float cy = uiHeight() * 0.5f;
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(animatedScale, animatedScale);
        context.getMatrices().translate(-cx, -cy);

        Rect root = root();
        renderTopMenu(context, uiMouseX, uiMouseY);

        switch (viewMode) {
            case WORKSPACE -> {
                renderWorkspaceColumns(context, uiMouseX, uiMouseY);
                if (selectedModule != null) renderDetailsPanel(context, uiMouseX, uiMouseY);
            }
            case SPATIAL -> renderSpatialWorkspace(context, uiMouseX, uiMouseY);
            case HUD -> renderHudEditor(context, uiMouseX, uiMouseY);
            case INSPECTOR -> renderInspector(context, uiMouseX, uiMouseY);
        }

        context.getMatrices().popMatrix();
        EclipseClientTheme.renderAlpha(1.0f);
        ClientRuntime.sampleUi(System.nanoTime() - startedAt);
    }

    private void ensureCaches() {
        if (layoutDirty) rebuildLayout();
        if (modulesDirty) rebuildVisibleModules();
        if (settingsDirty) rebuildVisibleSettings();
        ensureSelection();
    }

    private void rebuildLayout() {
        layoutDirty = false;

        cachedSidebarModes.clear();
        Rect root = root();
        int chip = 20;
        int gap = 5;
        int total = ViewMode.values().length;
        int dockW = total * chip + Math.max(0, total - 1) * gap;
        int modeX = root.x + (root.w - dockW) / 2;
        int modeY = root.y + 4;
        for (ViewMode value : ViewMode.values()) {
            cachedSidebarModes.add(new SidebarModeEntry(value, new Rect(modeX, modeY, chip, chip)));
            modeX += chip + gap;
        }

        cachedSidebarSections.clear();

        cachedViewChips.clear();
        cachedHeaderChips.clear();
        rebuildWorkspaceColumnRows();
    }

    private void rebuildVisibleModules() {
        modulesDirty = false;
        cachedModules.clear();
        List<Module> source = ClientRuntime.modules().modules(section);
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        for (Module module : source) {
            if (query.isEmpty()) cachedModules.add(module);
            else {
                String title = module.title == null ? "" : module.title.toLowerCase();
                String desc = module.description == null ? "" : module.description.toLowerCase();
                if (title.contains(query) || desc.contains(query)) cachedModules.add(module);
            }
        }
        int maxScroll = Math.max(0, cachedModules.size() - cachedModuleRows.size());
        moduleScroll = Math.min(moduleScroll, maxScroll);
    }

    private void rebuildVisibleSettings() {
        settingsDirty = false;
        cachedSettings.clear();
        if (selectedModule == null) return;
        for (SettingGroup group : selectedModule.settings) {
            boolean groupAdded = false;
            for (Setting<?> setting : group) {
                if (!setting.isVisible()) continue;
                cachedSettings.add(new SettingEntry(groupAdded ? null : group.name, setting));
                groupAdded = true;
            }
        }
        int maxScroll = Math.max(0, cachedSettings.size() - 9);
        settingsScroll = Math.min(settingsScroll, maxScroll);
    }

    private void ensureSelection() {
        if (viewMode == ViewMode.WORKSPACE) {
            if (selectedModule == null) cachedSettings.clear();
            else if (settingsDirty) rebuildVisibleSettings();
            return;
        }

        if (modulesDirty) rebuildVisibleModules();
        if (cachedModules.isEmpty()) {
            selectedModule = null;
            cachedSettings.clear();
            return;
        }
        boolean contains = false;
        if (selectedModule != null) {
            for (Module module : cachedModules) {
                if (module == selectedModule) {
                    contains = true;
                    break;
                }
            }
        }
        if (!contains) {
            selectedModule = cachedModules.get(0);
            settingsDirty = true;
        }
        if (settingsDirty) rebuildVisibleSettings();
    }

    private void ensureHudWidgets() {
        if (!hudWidgets.isEmpty()) return;
        for (HudWidgetBinding binding : ClientRuntime.hud().widgets()) {
            hudWidgets.add(new HudWidgetDraft(binding.widgetId(), prettyName(binding.widgetId()), binding.x(), binding.y(), binding.width(), binding.height(), binding.visible()));
        }
    }

    private void samplePerformance() {
        int fps = Math.max(1, MinecraftClient.getInstance().getCurrentFps());
        int modules = ClientRuntime.modules().activeCount();
        long used = ClientRuntime.perf().snapshot().usedMemoryBytes;
        long max = Math.max(1L, ClientRuntime.perf().snapshot().maxMemoryBytes);
        int memPercent = (int) ((used * 100L) / max);
        pushSample(fpsSamples, fps);
        pushSample(moduleSamples, modules);
        pushSample(memorySamples, memPercent);
        perfSamples = Math.min(fpsSamples.length, perfSamples + 1);
    }

    private void pushSample(int[] samples, int value) {
        System.arraycopy(samples, 1, samples, 0, samples.length - 1);
        samples[samples.length - 1] = value;
    }

    private void renderTopMenu(DrawContext context, int mouseX, int mouseY) {
        Rect root = root();
        Rect theme = themeChipRect();
        Rect close = closeChipRect();
        int leftStatusX = root.x + 4;
        int baselineY = root.y + 8;

        EclipseClientTheme.drawFaintText(textRenderer, context, "ECL", leftStatusX, baselineY);
        EclipseClientTheme.drawFaintText(textRenderer, context, "FPS " + MinecraftClient.getInstance().getCurrentFps(), leftStatusX + 24, baselineY);

        String hoverLabel = null;
        if (macDock) {
            int trayX = Math.max(root.x + 120, cachedSidebarModes.isEmpty() ? root.x + 120 : cachedSidebarModes.get(0).bounds.x - 14);
            int trayRight = close.x + close.w + 6;
            int trayW = Math.max(148, trayRight - trayX + 8);
            EclipseClientTheme.drawDockTray(context, trayX, root.y + 1, trayW, 28);
            float signal = smooth(closing ? 1.0f - animationProgress() : animationProgress());
            int progress = Math.max(12, Math.round((trayW - 20) * signal));
            context.fill(trayX + 10, root.y + 24, trayX + 10 + progress, root.y + 25, EclipseClientTheme.alpha(EclipseClientTheme.ACCENT, 0.42f));

            for (SidebarModeEntry entry : cachedSidebarModes) {
                boolean active = entry.mode == viewMode;
                boolean hovered = entry.bounds.contains(mouseX, mouseY);
                int size = entry.bounds.w + (dockMagnify ? (hovered ? 5 : active ? 2 : 0) : 0);
                int rx = entry.bounds.x - (size - entry.bounds.w) / 2;
                int ry = entry.bounds.y - (size - entry.bounds.h) - (hovered ? 1 : 0);
                EclipseClientTheme.drawIconButton(context, textRenderer, rx, ry, size, active || hovered, modeIcon(entry.mode));
                if (hovered) hoverLabel = entry.mode.title;
            }

            boolean themeHover = theme.contains(mouseX, mouseY);
            boolean closeHover = close.contains(mouseX, mouseY);
            int themeSize = theme.w + (dockMagnify && themeHover ? 5 : 0);
            int closeSize = close.w + (dockMagnify && closeHover ? 5 : 0);
            EclipseClientTheme.drawIconButton(context, textRenderer, theme.x - (themeSize - theme.w) / 2, theme.y - (themeSize - theme.h), themeSize, themeHover, themeIcon());
            EclipseClientTheme.drawIconButton(context, textRenderer, close.x - (closeSize - close.w) / 2, close.y - (closeSize - close.h), closeSize, closeHover, "close");
            if (themeHover) hoverLabel = "Theme";
            if (closeHover) hoverLabel = "Close";

            if (hoverLabel != null) {
                int labelW = Math.max(46, textRenderer.getWidth(hoverLabel) + 18);
                int labelX;
                if (closeHover) labelX = close.x + close.w / 2 - labelW / 2;
                else if (themeHover) labelX = theme.x + theme.w / 2 - labelW / 2;
                else {
                    SidebarModeEntry hoveredEntry = null;
                    for (SidebarModeEntry entry : cachedSidebarModes) if (entry.bounds.contains(mouseX, mouseY)) { hoveredEntry = entry; break; }
                    labelX = hoveredEntry == null ? trayX + trayW / 2 - labelW / 2 : hoveredEntry.bounds.x + hoveredEntry.bounds.w / 2 - labelW / 2;
                }
                int labelY = root.y - 12;
                EclipseClientTheme.drawGlassPanel(context, labelX, labelY, labelW, 16, false);
                if (scriptLabels) EclipseClientTheme.drawScriptCentered(textRenderer, context, hoverLabel, labelX, labelY + 4, labelW, EclipseClientTheme.textInvert());
                else EclipseClientTheme.drawCentered(textRenderer, context, hoverLabel, labelX, labelY + 4, labelW, EclipseClientTheme.textInvert());
            }
        }
        else {
            Rect themeDock = new Rect(root.x + root.w - 76, root.y + 4, 20, 20);
            Rect closeDock = new Rect(root.x + root.w - 50, root.y + 4, 20, 20);
            EclipseClientTheme.drawGlassPanel(context, root.x + 124, root.y + 2, 164, 24, false);
            for (SidebarModeEntry entry : cachedSidebarModes) {
                boolean active = entry.mode == viewMode;
                boolean hovered = entry.bounds.contains(mouseX, mouseY);
                EclipseClientTheme.drawIconButton(context, textRenderer, entry.bounds.x, entry.bounds.y, entry.bounds.w, active || hovered, modeIcon(entry.mode));
                if (hovered) hoverLabel = entry.mode.title;
            }
            boolean themeHover = themeDock.contains(mouseX, mouseY);
            boolean closeHover = closeDock.contains(mouseX, mouseY);
            EclipseClientTheme.drawIconButton(context, textRenderer, themeDock.x, themeDock.y, themeDock.w, themeHover, themeIcon());
            EclipseClientTheme.drawIconButton(context, textRenderer, closeDock.x, closeDock.y, closeDock.w, closeHover, "close");
        }
    }

    private void renderWorkspaceColumns(DrawContext context, int mouseX, int mouseY) {
        rebuildWorkspaceColumnRows();
        ClientSection[] sections = workspaceSections();
        int total = sections.length;
        for (int i = 0; i < total; i++) {
            ClientSection clientSection = sections[i];
            Rect column = workspaceColumnRect(i, total);
            EclipseClientTheme.drawGlassPanel(context, column.x, column.y, column.w, column.h, false);
            EclipseClientTheme.drawCentered(textRenderer, context, columnTitle(clientSection), column.x, column.y + 8, column.w, EclipseClientTheme.text());

            int rowY = column.y + 25;
            int rowH = 17;
            int maxRows = Math.max(1, (column.h - 32) / (rowH + 3));
            List<Module> modules = modulesForColumn(clientSection);
            for (int rowIndex = 0; rowIndex < modules.size() && rowIndex < maxRows; rowIndex++) {
                Module module = modules.get(rowIndex);
                Rect row = new Rect(column.x + 6, rowY, column.w - 12, rowH);
                boolean selected = module == selectedModule;
                boolean hovered = row.contains(mouseX, mouseY);
                boolean activeModule = module.isActive();
                EclipseClientTheme.drawGlassPanel(context, row.x, row.y, row.w, row.h, selected);
                if (hovered && !selected) {
                    context.fill(row.x + 1, row.y + 1, row.x + row.w - 1, row.y + row.h - 1, EclipseClientTheme.alpha(EclipseClientTheme.ACCENT, 0.08f));
                    context.fill(row.x + 1, row.y + row.h - 2, row.x + row.w - 1, row.y + row.h - 1, EclipseClientTheme.alpha(EclipseClientTheme.ACCENT, 0.24f));
                }
                if (activeModule && alertRails) {
                    context.fill(row.x + row.w - 3, row.y + 2, row.x + row.w - 1, row.y + row.h - 2, EclipseClientTheme.alpha(EclipseClientTheme.ALERT, 0.65f));
                }
                Rect smallToggle = moduleColumnToggleRect(row);
                context.fill(smallToggle.x, smallToggle.y, smallToggle.x + smallToggle.w, smallToggle.y + smallToggle.h, activeModule ? EclipseClientTheme.accent() : EclipseClientTheme.alpha(0xFFFFFFFF, 0.20f));
                int titleMaxChars = Math.max(6, (row.w - 34) / 6);
                if (activeModule) EclipseClientTheme.drawText(textRenderer, context, trim(textRenderer, module.title, titleMaxChars), row.x + 12, row.y + 5);
                else EclipseClientTheme.drawMutedText(textRenderer, context, trim(textRenderer, module.title, titleMaxChars), row.x + 12, row.y + 5);
                EclipseClientTheme.drawFaintText(textRenderer, context, hovered ? ">>" : ">", row.x + row.w - 12, row.y + 5);
                rowY += rowH + 3;
            }
        }
    }

    private void rebuildWorkspaceColumnRows() {
        cachedModuleColumnRows.clear();
        ClientSection[] sections = workspaceSections();
        for (int i = 0; i < sections.length; i++) {
            Rect column = workspaceColumnRect(i, sections.length);
            int rowY = column.y + 25;
            int rowH = 17;
            int maxRows = Math.max(1, (column.h - 32) / (rowH + 3));
            List<Module> modules = modulesForColumn(sections[i]);
            for (int rowIndex = 0; rowIndex < modules.size() && rowIndex < maxRows; rowIndex++) {
                cachedModuleColumnRows.add(new ModuleColumnEntry(modules.get(rowIndex), new Rect(column.x + 6, rowY, column.w - 12, rowH)));
                rowY += rowH + 3;
            }
        }
    }

    private List<Module> modulesForColumn(ClientSection clientSection) {
        return ClientRuntime.modules().modules(clientSection);
    }

    private ClientSection[] workspaceSections() {
        return new ClientSection[] {
            ClientSection.COMBAT,
            ClientSection.MOVEMENT,
            ClientSection.UTILITY,
            ClientSection.NETWORK,
            ClientSection.VISUALS,
            ClientSection.CHAT
        };
    }

    private String columnTitle(ClientSection clientSection) {
        return switch (clientSection) {
            case UTILITY -> "Player";
            case NETWORK -> "Misc";
            default -> clientSection.title();
        };
    }

    private String modeIcon(ViewMode mode) {
        return switch (mode) {
            case WORKSPACE -> "workspace";
            case SPATIAL -> "spatial";
            case HUD -> "hud";
            case INSPECTOR -> "inspector";
        };
    }

    private String themeIcon() {
        return ClientRuntime.theme().current() == eclipse.client.theme.ClientThemeId.DARK_MONO ? "theme-dark" : "theme-light";
    }

    private void renderSidebar(DrawContext context) {
        Rect side = sidebarRect();
        EclipseClientTheme.drawRaisedPanel(context, side.x, side.y, side.w, side.h);
        EclipseClientTheme.drawText(textRenderer, context, "Eclipse Client", side.x + 16, side.y + 16);
        EclipseClientTheme.drawMutedText(textRenderer, context, "Refined control surface", side.x + 16, side.y + 29);

        for (SidebarModeEntry entry : cachedSidebarModes) {
            boolean active = entry.mode == viewMode;
            EclipseClientTheme.drawHeaderChip(context, entry.bounds.x, entry.bounds.y, entry.bounds.w, entry.bounds.h, active);
            EclipseClientTheme.drawText(textRenderer, context, entry.mode.title, entry.bounds.x + 12, entry.bounds.y + 9);
            EclipseClientTheme.drawStatusDot(context, entry.bounds.x + entry.bounds.w - 14, entry.bounds.y + 11, active);
        }

        boolean sectionVisible = viewMode == ViewMode.WORKSPACE;
        if (sectionVisible) {
            EclipseClientTheme.drawDivider(context, side.x + 12, side.y + 204, side.w - 24);
            EclipseClientTheme.drawFaintText(textRenderer, context, "Workspace", side.x + 12, side.y + 214);
            for (SidebarSectionEntry entry : cachedSidebarSections) {
                boolean active = entry.section == section;
                EclipseClientTheme.drawHeaderChip(context, entry.bounds.x, entry.bounds.y, entry.bounds.w, entry.bounds.h, active);
                EclipseClientTheme.drawText(textRenderer, context, entry.section.title(), entry.bounds.x + 11, entry.bounds.y + 6);
            }
        }

        int statX = side.x + 12;
        int statY = side.y + side.h - 94;
        int statW = side.w - 24;
        EclipseClientTheme.drawInsetPanel(context, statX, statY, statW, 74);
        EclipseClientTheme.drawFaintText(textRenderer, context, "Session", statX + 12, statY + 10);
        EclipseClientTheme.drawText(textRenderer, context, username(), statX + 12, statY + 22);
        EclipseClientTheme.drawMutedText(textRenderer, context, profileLabel() + "  |  " + viewMode.title, statX + 12, statY + 37);
        EclipseClientTheme.drawFaintText(textRenderer, context, ClientRuntime.modules().activeCount() + " live modules  |  Right Shift", statX + 12, statY + 52);
    }

    private void renderHeader(DrawContext context) {
        int titleX = modulesPanelRect().x;
        int titleY = root().y + 16;
        EclipseClientTheme.drawText(textRenderer, context, viewMode.title, titleX, titleY);
        EclipseClientTheme.drawMutedText(textRenderer, context, headerSubtitle(), titleX, titleY + 13);
        if (viewMode != ViewMode.WORKSPACE) {
            Rect note = new Rect(modulesPanelRect().x, root().y + 14, 250, 20);
            EclipseClientTheme.drawInsetPanel(context, note.x, note.y, note.w, note.h);
            EclipseClientTheme.drawMutedText(textRenderer, context, headerNote(), note.x + 10, note.y + 6);
        }

        Rect theme = themeChipRect();
        EclipseClientTheme.drawMiniChip(context, theme.x, theme.y, theme.w, theme.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, ClientRuntime.theme().current() == eclipse.client.theme.ClientThemeId.DARK_MONO ? "Mono" : "Light", theme.x, theme.y + 6, theme.w, EclipseClientTheme.text());

        Rect close = closeChipRect();
        EclipseClientTheme.drawDarkButton(context, close.x, close.y, close.w, close.h);
        EclipseClientTheme.drawCentered(textRenderer, context, "Exit", close.x, close.y + 7, close.w, EclipseClientTheme.textInvert());
    }

    private void renderModulesPanel(DrawContext context, int mouseX, int mouseY) {
        Rect panel = modulesPanelRect();
        EclipseClientTheme.drawRaisedPanel(context, panel.x, panel.y, panel.w, panel.h);
        EclipseClientTheme.drawText(textRenderer, context, section == ClientSection.OVERVIEW ? "Module Library" : section.title(), panel.x + 16, panel.y + 14);
        EclipseClientTheme.drawMutedText(textRenderer, context, cachedModules.size() + " modules", panel.x + panel.w - 86, panel.y + 14);

        EclipseClientTheme.drawSearchCapsule(context, searchField.getX() - 8, searchField.getY() - 3, searchField.getWidth() + 16, 28);
        searchField.renderWidget(context, mouseX, mouseY, 0);

        Rect clear = clearSearchRect();
        EclipseClientTheme.drawMiniChip(context, clear.x, clear.y, clear.w, clear.h, false);
            EclipseClientTheme.drawCentered(textRenderer, context, "Clear", clear.x, clear.y + 8, clear.w, EclipseClientTheme.textMuted());

        context.enableScissor(panel.x + 8, panel.y + 64, panel.x + panel.w - 8, panel.y + panel.h - 14);
        for (int i = 0; i < cachedModuleRows.size() && i + moduleScroll < cachedModules.size(); i++) {
            Module module = cachedModules.get(i + moduleScroll);
            Rect row = cachedModuleRows.get(i);
            boolean selected = module == selectedModule;
            EclipseClientTheme.drawCard(context, row.x, row.y, row.w, row.h, selected);
            EclipseClientTheme.drawStatusDot(context, row.x + 10, row.y + 13, module.isActive());
            EclipseClientTheme.drawText(textRenderer, context, module.title, row.x + 22, row.y + 5);
            EclipseClientTheme.drawMutedText(textRenderer, context, trim(textRenderer, module.description, 24), row.x + 22, row.y + 16);

            Rect badge = new Rect(row.x + row.w - 80, row.y + 7, 40, 17);
            EclipseClientTheme.drawMiniChip(context, badge.x, badge.y, badge.w, badge.h, false);
            EclipseClientTheme.drawCentered(textRenderer, context, shortLabel(categoryLabel(module)), badge.x, badge.y + 5, badge.w, EclipseClientTheme.textMuted());

            Rect toggle = rowToggleRect(row);
            EclipseClientTheme.drawToggle(context, toggle.x, toggle.y, toggle.w, toggle.h, module.isActive());
        }
        context.disableScissor();

        if (cachedModules.isEmpty()) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "Nothing matches the current section or search query.", panel.x + 18, panel.y + 102);
        }
    }

    private void renderDetailsPanel(DrawContext context, int mouseX, int mouseY) {
        Rect panel = detailsPanelRect();
        EclipseClientTheme.drawRaisedPanel(context, panel.x, panel.y, panel.w, panel.h);

        if (selectedModule == null) {
            renderOverview(context, panel);
            return;
        }

        EclipseClientTheme.drawText(textRenderer, context, trimToWidth(selectedModule.title, Math.max(80, panel.w - 150)), panel.x + 18, panel.y + 16);
        EclipseClientTheme.drawMutedText(textRenderer, context, trimToWidth(selectedModule.description, Math.max(110, panel.w - 150)), panel.x + 18, panel.y + 29);

        Rect closeDetails = detailCloseRect();
        EclipseClientTheme.drawMiniChip(context, closeDetails.x, closeDetails.y, closeDetails.w, closeDetails.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, "x", closeDetails.x, closeDetails.y + 5, closeDetails.w, EclipseClientTheme.textMuted());

        Rect stateChip = new Rect(panel.x + panel.w - 116, panel.y + 12, 58, 18);
        EclipseClientTheme.drawMiniChip(context, stateChip.x, stateChip.y, stateChip.w, stateChip.h, selectedModule.isActive());
        if (selectedModule.isActive()) EclipseClientTheme.drawInvertText(textRenderer, context, "On", stateChip.x + 24, stateChip.y + 5);
        else EclipseClientTheme.drawMutedText(textRenderer, context, "Off", stateChip.x + 22, stateChip.y + 5);

        Rect categoryChip = new Rect(panel.x + panel.w - 52, panel.y + 34, 36, 18);
        EclipseClientTheme.drawMiniChip(context, categoryChip.x, categoryChip.y, categoryChip.w, categoryChip.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, shortLabel(categoryLabel(selectedModule)), categoryChip.x, categoryChip.y + 5, categoryChip.w, EclipseClientTheme.textMuted());

        Rect toggle = detailToggleRect();
        EclipseClientTheme.drawToggle(context, toggle.x, toggle.y, toggle.w, toggle.h, selectedModule.isActive());

        Rect bindRect = detailBindRect();
        Rect clearBindRect = detailBindClearRect();
        EclipseClientTheme.drawMiniChip(context, bindRect.x, bindRect.y, bindRect.w, bindRect.h, bindingModule == selectedModule || Modules.get().isBinding());
        String bindText = bindingModule == selectedModule || Modules.get().isBinding() ? "Press key..." : "Bind: " + bindLabel(selectedModule);
        EclipseClientTheme.drawCentered(textRenderer, context, trimToWidth(bindText, Math.max(42, bindRect.w - 10)), bindRect.x, bindRect.y + 6, bindRect.w, (bindingModule == selectedModule || Modules.get().isBinding()) ? EclipseClientTheme.textInvert() : EclipseClientTheme.textMuted());
        EclipseClientTheme.drawMiniChip(context, clearBindRect.x, clearBindRect.y, clearBindRect.w, clearBindRect.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, "Clear", clearBindRect.x, clearBindRect.y + 6, clearBindRect.w, EclipseClientTheme.textMuted());

        int metricsY = panel.y + 92;
        int cardW = (panel.w - 50) / 4;
        renderMetricCard(context, panel.x + 14, metricsY, cardW, "Settings", String.valueOf(cachedSettings.size()));
        renderMetricCard(context, panel.x + 20 + cardW, metricsY, cardW, "Status", selectedModule.isActive() ? "Running" : "Standby");
        renderMetricCard(context, panel.x + 26 + cardW * 2, metricsY, cardW, "Section", section.title());
        renderMetricCard(context, panel.x + 32 + cardW * 3, metricsY, cardW, "Bind", bindLabel(selectedModule));

        int contentY = detailSettingsContentY();
        int contentBottom = panel.y + panel.h - FOOTER_H - 14;
        context.enableScissor(panel.x + 8, contentY, panel.x + panel.w - 8, contentBottom);
        int skipped = settingsScroll;
        int y = contentY;
        for (int i = 0; i < cachedSettings.size(); i++) {
            SettingEntry entry = cachedSettings.get(i);
            if (skipped > 0) {
                skipped--;
                continue;
            }
            if (entry.groupName != null) {
                if (y + 18 > contentBottom) break;
                EclipseClientTheme.drawFaintText(textRenderer, context, entry.groupName, panel.x + 18, y);
                y += 16;
            }
            if (y + SETTING_ROW_H > contentBottom) break;
            renderSettingRow(context, panel, entry.setting, y);
            y += SETTING_ROW_H + 4;
        }
        context.disableScissor();

        Rect footer = new Rect(panel.x + 12, panel.y + panel.h - FOOTER_H - 8, panel.w - 24, FOOTER_H);
        EclipseClientTheme.drawInsetPanel(context, footer.x, footer.y, footer.w, footer.h);
        EclipseClientTheme.drawMutedText(textRenderer, context, "Select values to tune. Scroll to reveal more controls.", footer.x + 12, footer.y + 7);
    }

    private void renderHudEditor(DrawContext context, int mouseX, int mouseY) {
        Rect left = hudListRect();
        Rect canvas = hudCanvasRect();
        Rect inspector = hudInspectorRect();
        Rect clientEditor = hudClientEditorRect();

        EclipseClientTheme.drawGlassPanel(context, left.x, left.y, left.w, left.h, false);
        EclipseClientTheme.drawGlassPanel(context, canvas.x, canvas.y, canvas.w, canvas.h, false);
        EclipseClientTheme.drawGlassPanel(context, inspector.x, inspector.y, inspector.w, inspector.h, false);

        if (scriptLabels) EclipseClientTheme.drawScriptText(textRenderer, context, "HUD Studio", left.x + 12, left.y + 10, EclipseClientTheme.text());
        else EclipseClientTheme.drawText(textRenderer, context, "HUD", left.x + 12, left.y + 11);
        EclipseClientTheme.drawFaintText(textRenderer, context, hudWidgets.size() + " widgets", left.x + left.w - 62, left.y + 12);
        if (scriptLabels) EclipseClientTheme.drawScriptText(textRenderer, context, "Preview", canvas.x + 14, canvas.y + 10, EclipseClientTheme.text());
        else EclipseClientTheme.drawText(textRenderer, context, "Preview", canvas.x + 14, canvas.y + 11);
        EclipseClientTheme.drawFaintText(textRenderer, context, "Full scaled screen", canvas.x + 72, canvas.y + 12);
        if (scriptLabels) EclipseClientTheme.drawScriptText(textRenderer, context, "Widget", inspector.x + 14, inspector.y + 10, EclipseClientTheme.text());
        else EclipseClientTheme.drawText(textRenderer, context, "Widget", inspector.x + 14, inspector.y + 11);

        int rowY = left.y + 34;
        for (int i = 0; i < hudWidgets.size(); i++) {
            HudWidgetDraft widget = hudWidgets.get(i);
            Rect row = new Rect(left.x + 8, rowY, left.w - 16, 24);
            boolean selected = i == hudSelectedIndex;
            boolean hovered = row.contains(mouseX, mouseY);
            EclipseClientTheme.drawCard(context, row.x, row.y, row.w, row.h, selected);
            if (hovered && !selected) {
                context.fill(row.x + 1, row.y + 1, row.x + row.w - 1, row.y + row.h - 1, EclipseClientTheme.alpha(EclipseClientTheme.ACCENT, 0.07f));
            }
            if (widget.visible && alertRails) context.fill(row.x + row.w - 3, row.y + 2, row.x + row.w - 1, row.y + row.h - 2, EclipseClientTheme.alpha(EclipseClientTheme.ALERT, 0.42f));
            EclipseClientTheme.drawStatusDot(context, row.x + 8, row.y + 9, widget.visible);
            EclipseClientTheme.drawText(textRenderer, context, trim(textRenderer, widget.name, 17), row.x + 19, row.y + 7);
            EclipseClientTheme.drawFaintText(textRenderer, context, widget.visible ? "on" : "off", row.x + row.w - 25, row.y + 7);
            rowY += 28;
        }

        Rect innerCanvas = hudInnerCanvasRect();
        Rect placement = hudPlacementRect();
        EclipseClientTheme.drawInsetPanel(context, innerCanvas.x, innerCanvas.y, innerCanvas.w, innerCanvas.h);
        EclipseClientTheme.drawCanvasGrid(context, innerCanvas.x + 6, innerCanvas.y + 6, innerCanvas.w - 12, innerCanvas.h - 12, 16);
        EclipseClientTheme.drawGlassPanel(context, placement.x, placement.y, placement.w, placement.h, false);
        for (int i = 0; i < hudWidgets.size(); i++) {
            HudWidgetDraft widget = hudWidgets.get(i);
            if (!widget.visible) continue;
            Rect box = widgetCanvasRect(placement, widget);
            EclipseClientTheme.drawCard(context, box.x, box.y, box.w, box.h, i == hudSelectedIndex);
            EclipseClientTheme.drawText(textRenderer, context, widget.name, box.x + 6, box.y + 6);
        }

        HudWidgetDraft selected = selectedHudWidget();
        if (selected != null) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "Selected", inspector.x + 14, inspector.y + 30);
            EclipseClientTheme.drawText(textRenderer, context, selected.name, inspector.x + 14, inspector.y + 44);

            int miniW = (inspector.w - 34) / 2;
            renderMetricCard(context, inspector.x + 14, inspector.y + 66, miniW, "Position", selected.x + ", " + selected.y);
            renderMetricCard(context, inspector.x + 20 + miniW, inspector.y + 66, miniW, "Size", selected.w + " x " + selected.h);
            renderMetricCard(context, inspector.x + 14, inspector.y + 122, inspector.w - 28, "State", selected.visible ? "Visible" : "Hidden");

            Rect hide = new Rect(inspector.x + 14, inspector.y + 178, inspector.w - 28, 20);
            int moveW = (inspector.w - 46) / 4;
            Rect leftBtn = new Rect(inspector.x + 14, inspector.y + 204, moveW, 20);
            Rect rightBtn = new Rect(leftBtn.x + moveW + 6, inspector.y + 204, moveW, 20);
            Rect upBtn = new Rect(rightBtn.x + moveW + 6, inspector.y + 204, moveW, 20);
            Rect downBtn = new Rect(upBtn.x + moveW + 6, inspector.y + 204, moveW, 20);
            Rect saveBtn = new Rect(inspector.x + 14, inspector.y + 230, (inspector.w - 34) / 2, 20);
            Rect resetBtn = new Rect(saveBtn.x + saveBtn.w + 6, inspector.y + 230, (inspector.w - 34) / 2, 20);

            EclipseClientTheme.drawMiniChip(context, hide.x, hide.y, hide.w, hide.h, false);
            EclipseClientTheme.drawCentered(textRenderer, context, selected.visible ? "Hide widget" : "Show widget", hide.x, hide.y + 6, hide.w, EclipseClientTheme.text());
            drawMoveButton(context, leftBtn, "<");
            drawMoveButton(context, rightBtn, ">");
            drawMoveButton(context, upBtn, "^");
            drawMoveButton(context, downBtn, "v");
            EclipseClientTheme.drawDarkButton(context, saveBtn.x, saveBtn.y, saveBtn.w, saveBtn.h);
            EclipseClientTheme.drawCentered(textRenderer, context, "Save", saveBtn.x, saveBtn.y + 6, saveBtn.w, EclipseClientTheme.textInvert());
            EclipseClientTheme.drawMiniChip(context, resetBtn.x, resetBtn.y, resetBtn.w, resetBtn.h, false);
            EclipseClientTheme.drawCentered(textRenderer, context, "Reset", resetBtn.x, resetBtn.y + 6, resetBtn.w, EclipseClientTheme.text());
        }

        EclipseClientTheme.drawInsetPanel(context, clientEditor.x, clientEditor.y, clientEditor.w, clientEditor.h);
        if (scriptLabels) EclipseClientTheme.drawScriptText(textRenderer, context, "Client Editor", clientEditor.x + 10, clientEditor.y + 8, EclipseClientTheme.accent());
        else EclipseClientTheme.drawMutedText(textRenderer, context, "Client Editor", clientEditor.x + 10, clientEditor.y + 9);
        EclipseClientTheme.drawFaintText(textRenderer, context, "Dock / labels / client UI", clientEditor.x + 10, clientEditor.y + 20);

        int chipW = (clientEditor.w - 18) / 2;
        Rect dockStyle = new Rect(clientEditor.x + 10, clientEditor.y + 34, chipW, 20);
        Rect labels = new Rect(dockStyle.x + chipW + 8, clientEditor.y + 34, chipW, 20);
        Rect magnify = new Rect(clientEditor.x + 10, clientEditor.y + 58, chipW, 20);
        Rect rails = new Rect(magnify.x + chipW + 8, clientEditor.y + 58, chipW, 20);
        EclipseClientTheme.drawMiniChip(context, dockStyle.x, dockStyle.y, dockStyle.w, dockStyle.h, macDock);
        EclipseClientTheme.drawCentered(textRenderer, context, macDock ? "Mac Dock" : "Classic Dock", dockStyle.x, dockStyle.y + 6, dockStyle.w, macDock ? EclipseClientTheme.textInvert() : EclipseClientTheme.text());
        EclipseClientTheme.drawMiniChip(context, labels.x, labels.y, labels.w, labels.h, scriptLabels);
        EclipseClientTheme.drawCentered(textRenderer, context, scriptLabels ? "Script Labels" : "Plain Labels", labels.x, labels.y + 6, labels.w, scriptLabels ? EclipseClientTheme.textInvert() : EclipseClientTheme.text());
        EclipseClientTheme.drawMiniChip(context, magnify.x, magnify.y, magnify.w, magnify.h, dockMagnify);
        EclipseClientTheme.drawCentered(textRenderer, context, dockMagnify ? "Magnify On" : "Magnify Off", magnify.x, magnify.y + 6, magnify.w, dockMagnify ? EclipseClientTheme.textInvert() : EclipseClientTheme.text());
        EclipseClientTheme.drawMiniChip(context, rails.x, rails.y, rails.w, rails.h, alertRails);
        EclipseClientTheme.drawCentered(textRenderer, context, alertRails ? "Alert Style" : "Soft Style", rails.x, rails.y + 6, rails.w, alertRails ? EclipseClientTheme.textInvert() : EclipseClientTheme.text());
    }

    private void renderInspector(DrawContext context, int mouseX, int mouseY) {
        Rect left = inspectorLeftRect();
        Rect right = inspectorRightRect();
        EclipseClientTheme.drawRaisedPanel(context, left.x, left.y, left.w, left.h);
        EclipseClientTheme.drawRaisedPanel(context, right.x, right.y, right.w, right.h);

        EclipseClientTheme.drawText(textRenderer, context, "Runtime Inspector", left.x + 14, left.y + 14);
        EclipseClientTheme.drawMutedText(textRenderer, context, "Sampled metrics with low-overhead diagnostics", left.x + 14, left.y + 26);

        Rect compact = new Rect(left.x + left.w - 128, left.y + 12, 54, 18);
        Rect expanded = new Rect(left.x + left.w - 68, left.y + 12, 54, 18);
        EclipseClientTheme.drawMiniChip(context, compact.x, compact.y, compact.w, compact.h, inspectorMode == 0);
        EclipseClientTheme.drawMiniChip(context, expanded.x, expanded.y, expanded.w, expanded.h, inspectorMode == 1);
        if (inspectorMode == 0) EclipseClientTheme.drawInvertText(textRenderer, context, "Compact", compact.x + 7, compact.y + 5);
        else EclipseClientTheme.drawMutedText(textRenderer, context, "Compact", compact.x + 7, compact.y + 5);
        if (inspectorMode == 1) EclipseClientTheme.drawInvertText(textRenderer, context, "Expanded", expanded.x + 5, expanded.y + 5);
        else EclipseClientTheme.drawMutedText(textRenderer, context, "Expanded", expanded.x + 5, expanded.y + 5);

        PerfSnapshot snapshot = ClientRuntime.perf().snapshot();
        int topY = left.y + 44;
        int cardW = (left.w - 52) / 3;
        renderMetricCard(context, left.x + 14, topY, cardW, "FPS", String.valueOf(snapshot.estimatedFps()));
        renderMetricCard(context, left.x + 20 + cardW, topY, cardW, "Frame", String.format("%.2f ms", snapshot.frameMs()));
        renderMetricCard(context, left.x + 26 + cardW * 2, topY, cardW, "Tick", String.format("%.2f ms", snapshot.tickMs()));
        renderMetricCard(context, left.x + 14, topY + 68, cardW, "HUD", String.format("%.2f ms", snapshot.hudMs()));
        renderMetricCard(context, left.x + 20 + cardW, topY + 68, cardW, "UI", String.format("%.2f ms", snapshot.uiMs()));
        renderMetricCard(context, left.x + 26 + cardW * 2, topY + 68, cardW, "Mem", snapshot.memoryPercent() + "%");

        int graphY = topY + 146;
        int graphH = inspectorMode == 0 ? 82 : 118;
        drawGraphCard(context, left.x + 16, graphY, left.w - 32, graphH, "FPS Trend", fpsSamples, perfSamples, EclipseClientTheme.accent());
        if (inspectorMode == 1) {
            drawGraphCard(context, left.x + 16, graphY + graphH + 12, (left.w - 40) / 2, 96, "Modules", moduleSamples, perfSamples, EclipseClientTheme.good());
            drawGraphCard(context, left.x + 24 + (left.w - 40) / 2, graphY + graphH + 12, (left.w - 40) / 2, 96, "Memory", memorySamples, perfSamples, EclipseClientTheme.warn());
        }

        EclipseClientTheme.drawText(textRenderer, context, "Runtime Health", right.x + 14, right.y + 14);
        Rect warnPanel = new Rect(right.x + 14, right.y + 40, right.w - 28, 104);
        EclipseClientTheme.drawInsetPanel(context, warnPanel.x, warnPanel.y, warnPanel.w, warnPanel.h);
        renderWarnings(context, warnPanel);

        int statsY = warnPanel.y + warnPanel.h + 12;
        renderMetricCard(context, right.x + 14, statsY, right.w - 28, "Profile", ClientRuntime.theme().current().toString());
        renderMetricCard(context, right.x + 14, statsY + 70, right.w - 28, "Spatial", snapshot.waypointCount + " wp / " + snapshot.routeCount + " routes");
        renderMetricCard(context, right.x + 14, statsY + 140, right.w - 28, "HUD", snapshot.visibleHudWidgets + " visible widgets");

        Rect openHud = new Rect(right.x + 14, right.y + right.h - 42, right.w - 28, 22);
        EclipseClientTheme.drawDarkButton(context, openHud.x, openHud.y, openHud.w, openHud.h);
        EclipseClientTheme.drawCentered(textRenderer, context, "Open HUD Studio", openHud.x, openHud.y + 7, openHud.w, EclipseClientTheme.textInvert());
    }

    private void renderSpatialWorkspace(DrawContext context, int mouseX, int mouseY) {
        Rect waypoints = spatialWaypointsRect();
        Rect routes = spatialRoutesRect();
        Rect detail = spatialDetailRect();
        EclipseClientTheme.drawGlassPanel(context, waypoints.x, waypoints.y, waypoints.w, waypoints.h, false);
        EclipseClientTheme.drawGlassPanel(context, routes.x, routes.y, routes.w, routes.h, false);
        EclipseClientTheme.drawGlassPanel(context, detail.x, detail.y, detail.w, detail.h, false);

        EclipseClientTheme.drawText(textRenderer, context, "Waypoints", waypoints.x + 12, waypoints.y + 11);
        EclipseClientTheme.drawFaintText(textRenderer, context, ClientRuntime.spatial().waypoints().size() + " saved", waypoints.x + waypoints.w - 58, waypoints.y + 12);
        EclipseClientTheme.drawText(textRenderer, context, "Routes", routes.x + 12, routes.y + 11);
        EclipseClientTheme.drawFaintText(textRenderer, context, ClientRuntime.spatial().routes().size() + " paths", routes.x + routes.w - 52, routes.y + 12);
        EclipseClientTheme.drawText(textRenderer, context, "Details", detail.x + 14, detail.y + 11);

        int rowY = waypoints.y + 34;
        List<WaypointRecord> waypointList = ClientRuntime.spatial().waypoints();
        int maxWaypoints = Math.max(1, (waypoints.h - 78) / 28);
        for (int i = spatialWaypointScroll; i < waypointList.size() && i < spatialWaypointScroll + maxWaypoints; i++) {
            WaypointRecord waypoint = waypointList.get(i);
            Rect row = new Rect(waypoints.x + 8, rowY, waypoints.w - 16, 24);
            boolean selected = waypoint.id != null && waypoint.id.equals(selectedWaypointId);
            EclipseClientTheme.drawCard(context, row.x, row.y, row.w, row.h, selected);
            EclipseClientTheme.drawStatusDot(context, row.x + 8, row.y + 9, waypoint.visible);
            EclipseClientTheme.drawText(textRenderer, context, trim(textRenderer, waypoint.name, 18), row.x + 19, row.y + 5);
            EclipseClientTheme.drawFaintText(textRenderer, context, waypoint.x + " " + waypoint.y + " " + waypoint.z, row.x + 19, row.y + 15);
            rowY += 28;
        }
        Rect addCurrent = new Rect(waypoints.x + 8, waypoints.y + waypoints.h - 30, 72, 20);
        Rect removeWaypoint = new Rect(waypoints.x + 86, waypoints.y + waypoints.h - 30, 72, 20);
        EclipseClientTheme.drawDarkButton(context, addCurrent.x, addCurrent.y, addCurrent.w, addCurrent.h);
        EclipseClientTheme.drawCentered(textRenderer, context, "+ Add", addCurrent.x, addCurrent.y + 6, addCurrent.w, EclipseClientTheme.textInvert());
        EclipseClientTheme.drawMiniChip(context, removeWaypoint.x, removeWaypoint.y, removeWaypoint.w, removeWaypoint.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, "Remove", removeWaypoint.x, removeWaypoint.y + 6, removeWaypoint.w, EclipseClientTheme.text());

        int routeY = routes.y + 34;
        List<RouteRecord> routeList = ClientRuntime.spatial().routes();
        int maxRoutes = Math.max(1, (routes.h - 78) / 28);
        for (int i = spatialRouteScroll; i < routeList.size() && i < spatialRouteScroll + maxRoutes; i++) {
            RouteRecord route = routeList.get(i);
            Rect row = new Rect(routes.x + 8, routeY, routes.w - 16, 24);
            boolean selected = route.id != null && route.id.equals(selectedRouteId);
            boolean hovered = row.contains(mouseX, mouseY);
            EclipseClientTheme.drawCard(context, row.x, row.y, row.w, row.h, selected);
            if (hovered && !selected) context.fill(row.x + 1, row.y + 1, row.x + row.w - 1, row.y + row.h - 1, EclipseClientTheme.alpha(EclipseClientTheme.ACCENT, 0.07f));
            EclipseClientTheme.drawText(textRenderer, context, trim(textRenderer, route.name, 18), row.x + 10, row.y + 7);
            EclipseClientTheme.drawFaintText(textRenderer, context, route.waypointIds.size() + " pts", row.x + row.w - 42, row.y + 7);
            routeY += 28;
        }
        Rect addRoute = new Rect(routes.x + 8, routes.y + routes.h - 30, 58, 20);
        Rect append = new Rect(routes.x + 72, routes.y + routes.h - 30, 74, 20);
        Rect removeRoute = new Rect(routes.x + 152, routes.y + routes.h - 30, 70, 20);
        EclipseClientTheme.drawDarkButton(context, addRoute.x, addRoute.y, addRoute.w, addRoute.h);
        EclipseClientTheme.drawCentered(textRenderer, context, "New", addRoute.x, addRoute.y + 6, addRoute.w, EclipseClientTheme.textInvert());
        EclipseClientTheme.drawMiniChip(context, append.x, append.y, append.w, append.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, "Append", append.x, append.y + 6, append.w, EclipseClientTheme.text());
        EclipseClientTheme.drawMiniChip(context, removeRoute.x, removeRoute.y, removeRoute.w, removeRoute.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, "Remove", removeRoute.x, removeRoute.y + 6, removeRoute.w, EclipseClientTheme.text());

        WaypointRecord selectedWaypoint = selectedWaypoint();
        RouteRecord selectedRoute = selectedRoute();
        if (selectedRoute != null && inspectRouteDetails) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "Route", detail.x + 14, detail.y + 34);
            EclipseClientTheme.drawText(textRenderer, context, trim(textRenderer, selectedRoute.name, 26), detail.x + 14, detail.y + 48);
            renderMetricCard(context, detail.x + 14, detail.y + 72, detail.w - 28, "Visible", selectedRoute.visible ? "Yes" : "No");
            renderMetricCard(context, detail.x + 14, detail.y + 132, detail.w - 28, "Points", String.valueOf(selectedRoute.waypointIds.size()));
            renderMetricCard(context, detail.x + 14, detail.y + 192, detail.w - 28, "Nearest", nearestWaypointName(selectedRoute));
        } else if (selectedWaypoint != null) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "Waypoint", detail.x + 14, detail.y + 34);
            EclipseClientTheme.drawText(textRenderer, context, trim(textRenderer, selectedWaypoint.name, 26), detail.x + 14, detail.y + 48);
            renderMetricCard(context, detail.x + 14, detail.y + 72, detail.w - 28, "Dimension", trim(textRenderer, selectedWaypoint.dimension, 22));
            renderMetricCard(context, detail.x + 14, detail.y + 132, detail.w - 28, "Coords", selectedWaypoint.x + ", " + selectedWaypoint.y + ", " + selectedWaypoint.z);
            renderMetricCard(context, detail.x + 14, detail.y + 192, detail.w - 28, "State", selectedWaypoint.visible ? "Visible" : "Hidden");
        } else {
            renderMetricCard(context, detail.x + 14, detail.y + 42, detail.w - 28, "Waypoints", String.valueOf(ClientRuntime.spatial().waypoints().size()));
            renderMetricCard(context, detail.x + 14, detail.y + 102, detail.w - 28, "Routes", String.valueOf(ClientRuntime.spatial().routes().size()));
            renderMetricCard(context, detail.x + 14, detail.y + 162, detail.w - 28, "Visible", String.valueOf(ClientRuntime.spatial().visibleWaypointCount()));
        }

        Rect save = new Rect(detail.x + 14, detail.y + detail.h - 30, 82, 20);
        Rect openInspector = new Rect(detail.x + 102, detail.y + detail.h - 30, 98, 20);
        EclipseClientTheme.drawDarkButton(context, save.x, save.y, save.w, save.h);
        EclipseClientTheme.drawCentered(textRenderer, context, "Save", save.x, save.y + 6, save.w, EclipseClientTheme.textInvert());
        EclipseClientTheme.drawMiniChip(context, openInspector.x, openInspector.y, openInspector.w, openInspector.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, "Inspector", openInspector.x, openInspector.y + 6, openInspector.w, EclipseClientTheme.text());
    }

    private boolean clickSpatial(double mx, double my) {
        Rect waypoints = spatialWaypointsRect();
        int rowY = waypoints.y + 34;
        List<WaypointRecord> waypointList = ClientRuntime.spatial().waypoints();
        int maxWaypoints = Math.max(1, (waypoints.h - 78) / 28);
        for (int i = spatialWaypointScroll; i < waypointList.size() && i < spatialWaypointScroll + maxWaypoints; i++) {
            Rect row = new Rect(waypoints.x + 8, rowY, waypoints.w - 16, 24);
            if (row.contains(mx, my)) {
                selectedWaypointId = waypointList.get(i).id;
                inspectRouteDetails = false;
                return true;
            }
            rowY += 28;
        }
        Rect addCurrent = new Rect(waypoints.x + 8, waypoints.y + waypoints.h - 30, 72, 20);
        Rect removeWaypoint = new Rect(waypoints.x + 86, waypoints.y + waypoints.h - 30, 72, 20);
        if (addCurrent.contains(mx, my)) {
            WaypointRecord waypoint = ClientRuntime.spatial().addCurrentPlayerWaypoint(null);
            if (waypoint != null) { selectedWaypointId = waypoint.id; inspectRouteDetails = false; }
            return true;
        }
        if (removeWaypoint.contains(mx, my) && selectedWaypointId != null) {
            ClientRuntime.spatial().removeWaypoint(selectedWaypointId);
            selectedWaypointId = null;
            return true;
        }

        Rect routes = spatialRoutesRect();
        int routeY = routes.y + 34;
        List<RouteRecord> routeList = ClientRuntime.spatial().routes();
        int maxRoutes = Math.max(1, (routes.h - 78) / 28);
        for (int i = spatialRouteScroll; i < routeList.size() && i < spatialRouteScroll + maxRoutes; i++) {
            Rect row = new Rect(routes.x + 8, routeY, routes.w - 16, 24);
            if (row.contains(mx, my)) {
                selectedRouteId = routeList.get(i).id;
                inspectRouteDetails = true;
                return true;
            }
            routeY += 28;
        }
        Rect addRoute = new Rect(routes.x + 8, routes.y + routes.h - 30, 58, 20);
        Rect append = new Rect(routes.x + 72, routes.y + routes.h - 30, 74, 20);
        Rect removeRoute = new Rect(routes.x + 152, routes.y + routes.h - 30, 70, 20);
        if (addRoute.contains(mx, my)) {
            RouteRecord route = ClientRuntime.spatial().createRoute(null);
            selectedRouteId = route.id;
            inspectRouteDetails = true;
            return true;
        }
        if (append.contains(mx, my) && selectedRouteId != null && selectedWaypointId != null) {
            ClientRuntime.spatial().appendWaypointToRoute(selectedRouteId, selectedWaypointId);
            return true;
        }
        if (removeRoute.contains(mx, my) && selectedRouteId != null) {
            ClientRuntime.spatial().removeRoute(selectedRouteId);
            selectedRouteId = null;
            inspectRouteDetails = false;
            return true;
        }

        Rect detail = spatialDetailRect();
        Rect save = new Rect(detail.x + 14, detail.y + detail.h - 30, 82, 20);
        Rect openInspector = new Rect(detail.x + 102, detail.y + detail.h - 30, 98, 20);
        if (save.contains(mx, my)) {
            ClientRuntime.save();
            return true;
        }
        if (openInspector.contains(mx, my)) {
            viewMode = ViewMode.INSPECTOR;
            return true;
        }
        return false;
    }

    private void drawMoveButton(DrawContext context, Rect rect, String label) {
        EclipseClientTheme.drawMiniChip(context, rect.x, rect.y, rect.w, rect.h, false);
        EclipseClientTheme.drawCentered(textRenderer, context, label, rect.x, rect.y + 8, rect.w, EclipseClientTheme.text());
    }

    private void drawGraphCard(DrawContext context, int x, int y, int w, int h, String title, int[] samples, int count, int color) {
        EclipseClientTheme.drawInsetPanel(context, x, y, w, h);
        EclipseClientTheme.drawMutedText(textRenderer, context, title, x + 12, y + 10);
        Rect graph = new Rect(x + 12, y + 28, w - 24, h - 40);
        EclipseClientTheme.drawSoftPanel(context, graph.x, graph.y, graph.w, graph.h);
        EclipseClientTheme.drawGraph(context, samples, count, graph.x + 6, graph.y + 6, graph.w - 12, graph.h - 12, color);
    }

    private void renderWarnings(DrawContext context, Rect warnPanel) {
        PerfSnapshot snapshot = ClientRuntime.perf().snapshot();
        int fps = snapshot.estimatedFps();
        int mem = snapshot.memoryPercent();
        int modules = snapshot.enabledModules;
        int y = warnPanel.y + 12;
        if (fps < 60) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "- FPS dipped below 60. Consider Minimal HUD Mode.", warnPanel.x + 12, y);
            y += 18;
        }
        if (mem > 75) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "- Memory pressure is elevated. Reduce heavy visuals.", warnPanel.x + 12, y);
            y += 18;
        }
        if (modules > 12) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "- Many modules are active. Watch overlay and HUD cost.", warnPanel.x + 12, y);
            y += 18;
        }
        if (y == warnPanel.y + 12) {
            EclipseClientTheme.drawMutedText(textRenderer, context, "- No major issues detected. Runtime looks stable.", warnPanel.x + 12, y);
            y += 18;
        }
        EclipseClientTheme.drawFaintText(textRenderer, context, "Metrics are sampled and bounded to keep the inspector cheap.", warnPanel.x + 12, warnPanel.y + warnPanel.h - 18);
    }

    private boolean clickHud(double mx, double my) {
        Rect left = hudListRect();
        int rowY = left.y + 34;
        for (int i = 0; i < hudWidgets.size(); i++) {
            Rect row = new Rect(left.x + 8, rowY, left.w - 16, 24);
            if (row.contains(mx, my)) {
                hudSelectedIndex = i;
                return true;
            }
            rowY += 28;
        }

        Rect placement = hudPlacementRect();
        for (int i = hudWidgets.size() - 1; i >= 0; i--) {
            HudWidgetDraft widget = hudWidgets.get(i);
            if (!widget.visible) continue;
            Rect box = widgetCanvasRect(placement, widget);
            if (box.contains(mx, my)) {
                hudSelectedIndex = i;
                draggingHudIndex = i;
                dragOffsetX = (int) mx - box.x;
                dragOffsetY = (int) my - box.y;
                return true;
            }
        }

        Rect clientEditor = hudClientEditorRect();
        int chipW = (clientEditor.w - 18) / 2;
        Rect dockStyle = new Rect(clientEditor.x + 10, clientEditor.y + 34, chipW, 20);
        Rect labels = new Rect(dockStyle.x + chipW + 8, clientEditor.y + 34, chipW, 20);
        Rect magnify = new Rect(clientEditor.x + 10, clientEditor.y + 58, chipW, 20);
        Rect rails = new Rect(magnify.x + chipW + 8, clientEditor.y + 58, chipW, 20);
        if (dockStyle.contains(mx, my)) {
            macDock = !macDock;
            layoutDirty = true;
            return true;
        }
        if (labels.contains(mx, my)) {
            scriptLabels = !scriptLabels;
            return true;
        }
        if (magnify.contains(mx, my)) {
            dockMagnify = !dockMagnify;
            return true;
        }
        if (rails.contains(mx, my)) {
            alertRails = !alertRails;
            return true;
        }

        HudWidgetDraft selected = selectedHudWidget();
        if (selected == null) return false;
        Rect inspector = hudInspectorRect();
        Rect hide = new Rect(inspector.x + 14, inspector.y + 178, inspector.w - 28, 20);
        int moveW = (inspector.w - 46) / 4;
        Rect leftBtn = new Rect(inspector.x + 14, inspector.y + 204, moveW, 20);
        Rect rightBtn = new Rect(leftBtn.x + moveW + 6, inspector.y + 204, moveW, 20);
        Rect upBtn = new Rect(rightBtn.x + moveW + 6, inspector.y + 204, moveW, 20);
        Rect downBtn = new Rect(upBtn.x + moveW + 6, inspector.y + 204, moveW, 20);
        Rect saveBtn = new Rect(inspector.x + 14, inspector.y + 230, (inspector.w - 34) / 2, 20);
        Rect resetBtn = new Rect(saveBtn.x + saveBtn.w + 6, inspector.y + 230, (inspector.w - 34) / 2, 20);
        if (hide.contains(mx, my)) {
            selected.visible = !selected.visible;
            return true;
        }
        if (leftBtn.contains(mx, my)) {
            selected.x = Math.max(0, selected.x - hudSnap);
            return true;
        }
        if (rightBtn.contains(mx, my)) {
            selected.x = Math.min(Math.max(0, hudScreenWidth() - selected.w), selected.x + hudSnap);
            return true;
        }
        if (upBtn.contains(mx, my)) {
            selected.y = Math.max(0, selected.y - hudSnap);
            return true;
        }
        if (downBtn.contains(mx, my)) {
            selected.y = Math.min(Math.max(0, hudScreenHeight() - selected.h), selected.y + hudSnap);
            return true;
        }
        if (resetBtn.contains(mx, my)) {
            resetHudLayout();
            return true;
        }
        if (saveBtn.contains(mx, my)) {
            syncHudWidgetsToRuntime();
            ClientRuntime.save();
            return true;
        }
        return false;
    }

    private boolean clickInspector(double mx, double my) {
        Rect left = inspectorLeftRect();
        Rect compact = new Rect(left.x + left.w - 128, left.y + 12, 54, 18);
        Rect expanded = new Rect(left.x + left.w - 68, left.y + 12, 54, 18);
        if (compact.contains(mx, my)) {
            inspectorMode = 0;
            return true;
        }
        if (expanded.contains(mx, my)) {
            inspectorMode = 1;
            return true;
        }
        Rect right = inspectorRightRect();
        Rect openHud = new Rect(right.x + 14, right.y + right.h - 42, right.w - 28, 22);
        if (openHud.contains(mx, my)) {
            viewMode = ViewMode.HUD;
            return true;
        }
        return false;
    }

    private void resetHudLayout() {
        hudWidgets.clear();
        for (HudWidgetBinding binding : ClientRuntime.hud().widgets()) {
            if ("coordinates-hud".equals(binding.widgetId())) binding.setPosition(20, 14);
            if ("clock-hud".equals(binding.widgetId())) binding.setPosition(20, 36);
            if ("nearest-waypoint".equals(binding.widgetId())) binding.setPosition(20, 58);
            if ("active-modules".equals(binding.widgetId())) binding.setPosition(20, 82);
            if ("debug-overlay".equals(binding.widgetId())) binding.setPosition(20, 168);
            if ("coordinates-hud".equals(binding.widgetId()) || "clock-hud".equals(binding.widgetId()) || "nearest-waypoint".equals(binding.widgetId()) || "active-modules".equals(binding.widgetId())) binding.setVisible(true);
            if ("debug-overlay".equals(binding.widgetId())) binding.setVisible(false);
        }
        ensureHudWidgets();
        hudSelectedIndex = 0;
    }

    private HudWidgetDraft selectedHudWidget() {
        if (hudSelectedIndex < 0 || hudSelectedIndex >= hudWidgets.size()) return null;
        return hudWidgets.get(hudSelectedIndex);
    }

    private void renderOverview(DrawContext context, Rect panel) {
        EclipseClientTheme.drawText(textRenderer, context, "Eclipse Workspace", panel.x + 18, panel.y + 16);
        EclipseClientTheme.drawMutedText(textRenderer, context, "Professional shell with clearer hierarchy and tighter chrome.", panel.x + 18, panel.y + 30);

        int y = panel.y + 64;
        int cardW = (panel.w - 52) / 3;
        renderMetricCard(context, panel.x + 18, y, cardW, "Total modules", String.valueOf(ClientRuntime.modules().totalCount()));
        renderMetricCard(context, panel.x + 26 + cardW, y, cardW, "Active", String.valueOf(ClientRuntime.modules().activeCount()));
        renderMetricCard(context, panel.x + 34 + cardW * 2, y, cardW, "Waypoints", String.valueOf(ClientRuntime.spatial().waypoints().size()));

        int noteY = y + 80;
        EclipseClientTheme.drawInsetPanel(context, panel.x + 18, noteY, panel.w - 36, 108);
        EclipseClientTheme.drawText(textRenderer, context, "Interface direction", panel.x + 30, noteY + 12);
        EclipseClientTheme.drawMutedText(textRenderer, context, "- calmer surfaces and cleaner grouping", panel.x + 30, noteY + 30);
        EclipseClientTheme.drawMutedText(textRenderer, context, "- compact workspace, spatial, HUD, and diagnostics", panel.x + 30, noteY + 45);
        EclipseClientTheme.drawMutedText(textRenderer, context, "- less filler text, stronger product-style labels", panel.x + 30, noteY + 60);
        EclipseClientTheme.drawFaintText(textRenderer, context, "Built to read like one client instead of several tools stitched together.", panel.x + 30, noteY + 82);
    }

    private void renderMetricCard(DrawContext context, int x, int y, int w, String label, String value) {
        EclipseClientTheme.drawInsetPanel(context, x, y, w, 56);
        EclipseClientTheme.drawMutedText(textRenderer, context, label, x + 10, y + 9);
        EclipseClientTheme.drawText(textRenderer, context, value, x + 10, y + 25);
    }

    private void renderFooter(DrawContext context) {
        Rect root = root();
        int y = root.y + root.h - 28;
        EclipseClientTheme.drawInsetPanel(context, root.x + 16, y, root.w - 32, 18);
        String footer = switch (viewMode) {
            case WORKSPACE -> "Workspace  |  modules, categories, and tuned settings";
            case SPATIAL -> "Spatial  |  waypoints, routes, and saved path context";
            case HUD -> "HUD Studio  |  drag, snap, align, and save";
            case INSPECTOR -> "Inspector  |  low-overhead runtime diagnostics";
        };
        EclipseClientTheme.drawMutedText(textRenderer, context, footer, root.x + 24, y + 5);
    }

    private void renderSettingRow(DrawContext context, Rect panel, Setting<?> setting, int y) {
        int rowX = panel.x + 18;
        int rowW = panel.w - 36;
        EclipseClientTheme.drawGlassPanel(context, rowX, y, rowW, SETTING_ROW_H, false);
        int valueReserve = 142;
        EclipseClientTheme.drawText(textRenderer, context, trimToWidth(setting.title, Math.max(60, rowW - valueReserve)), rowX + 10, y + 7);

        if (setting instanceof BoolSetting boolSetting) {
            EclipseClientTheme.drawToggle(context, rowX + rowW - 40, y + 4, 26, 14, boolSetting.get());
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            Rect value = settingValueRect(rowX, y, rowW);
            EclipseClientTheme.drawInsetPanel(context, value.x, value.y, value.w, value.h);
            EclipseClientTheme.drawCentered(textRenderer, context, shortLabel(enumSetting.get().toString()), value.x, value.y + 4, value.w, EclipseClientTheme.text());
        } else if (setting instanceof IntSetting intSetting) {
            renderNumericEditor(context, rowX, y, rowW, String.valueOf(intSetting.get()));
        } else if (setting instanceof DoubleSetting doubleSetting) {
            renderNumericEditor(context, rowX, y, rowW, trimValue(String.format("%.2f", doubleSetting.get()), 6));
        } else {
            Rect value = new Rect(rowX + rowW - 124, y + 4, 114, 16);
            EclipseClientTheme.drawInsetPanel(context, value.x, value.y, value.w, value.h);
            EclipseClientTheme.drawCentered(textRenderer, context, shortLabel(setting.toString()), value.x, value.y + 4, value.w, EclipseClientTheme.textMuted());
        }
    }

    private void renderNumericEditor(DrawContext context, int rowX, int y, int rowW, String valueText) {
        Rect minus = new Rect(rowX + rowW - 98, y + 4, 20, 16);
        Rect value = new Rect(rowX + rowW - 76, y + 4, 50, 16);
        Rect plus = new Rect(rowX + rowW - 24, y + 4, 20, 16);
        EclipseClientTheme.drawInsetPanel(context, minus.x, minus.y, minus.w, minus.h);
        EclipseClientTheme.drawInsetPanel(context, value.x, value.y, value.w, value.h);
        EclipseClientTheme.drawInsetPanel(context, plus.x, plus.y, plus.w, plus.h);
        EclipseClientTheme.drawCentered(textRenderer, context, "-", minus.x, minus.y + 4, minus.w, EclipseClientTheme.text());
        EclipseClientTheme.drawCentered(textRenderer, context, valueText, value.x, value.y + 4, value.w, EclipseClientTheme.text());
        EclipseClientTheme.drawCentered(textRenderer, context, "+", plus.x, plus.y + 4, plus.w, EclipseClientTheme.text());
    }

    private boolean clickSetting(double mx, double my) {
        Rect panel = detailsPanelRect();
        int contentY = detailSettingsContentY();
        int contentBottom = panel.y + panel.h - FOOTER_H - 14;
        int skipped = settingsScroll;
        int y = contentY;

        for (int i = 0; i < cachedSettings.size(); i++) {
            SettingEntry entry = cachedSettings.get(i);
            if (skipped > 0) {
                skipped--;
                continue;
            }
            if (entry.groupName != null) y += 16;
            if (y + SETTING_ROW_H > contentBottom) return false;

            int rowX = panel.x + 18;
            int rowW = panel.w - 36;
            Setting<?> setting = entry.setting;
            if (setting instanceof BoolSetting boolSetting) {
                Rect toggle = new Rect(rowX + rowW - 40, y + 4, 26, 14);
                if (toggle.contains(mx, my)) {
                    boolSetting.set(!boolSetting.get());
                    settingsDirty = true;
                    return true;
                }
            } else if (setting instanceof EnumSetting<?> enumSetting) {
                Rect value = settingValueRect(rowX, y, rowW);
                if (value.contains(mx, my)) {
                    cycleEnum(enumSetting);
                    settingsDirty = true;
                    return true;
                }
            } else if (setting instanceof IntSetting intSetting) {
                Rect minus = new Rect(rowX + rowW - 98, y + 4, 20, 16);
                Rect plus = new Rect(rowX + rowW - 24, y + 4, 20, 16);
                if (minus.contains(mx, my)) {
                    intSetting.set(Math.max(intSetting.min, intSetting.get() - 1));
                    settingsDirty = true;
                    return true;
                }
                if (plus.contains(mx, my)) {
                    intSetting.set(Math.min(intSetting.max, intSetting.get() + 1));
                    settingsDirty = true;
                    return true;
                }
            } else if (setting instanceof DoubleSetting doubleSetting) {
                Rect minus = new Rect(rowX + rowW - 98, y + 4, 20, 16);
                Rect plus = new Rect(rowX + rowW - 24, y + 4, 20, 16);
                double step = Math.max(0.01, Math.pow(10, -Math.max(1, Math.min(2, doubleSetting.decimalPlaces))));
                if (minus.contains(mx, my)) {
                    doubleSetting.set(Math.max(doubleSetting.min, doubleSetting.get() - step));
                    settingsDirty = true;
                    return true;
                }
                if (plus.contains(mx, my)) {
                    doubleSetting.set(Math.min(doubleSetting.max, doubleSetting.get() + step));
                    settingsDirty = true;
                    return true;
                }
            }
            if (new Rect(rowX, y, rowW, SETTING_ROW_H).contains(mx, my)) return true;
            y += SETTING_ROW_H + 4;
        }
        return false;
    }

    private void cycleEnum(EnumSetting<?> enumSetting) {
        List<String> suggestions = enumSetting.getSuggestions();
        if (suggestions.isEmpty()) return;
        String current = enumSetting.get().toString();
        int index = suggestions.indexOf(current);
        int next = (index + 1 + suggestions.size()) % suggestions.size();
        enumSetting.parse(suggestions.get(next));
    }

    private int lastSample(int[] values) {
        return values[values.length - 1];
    }

    private Rect widgetCanvasRect(Rect placement, HudWidgetDraft widget) {
        double sx = placement.w / (double) Math.max(1, hudScreenWidth());
        double sy = placement.h / (double) Math.max(1, hudScreenHeight());
        int x = placement.x + (int) Math.round(widget.x * sx);
        int y = placement.y + (int) Math.round(widget.y * sy);
        int w = Math.max(8, (int) Math.round(widget.w * sx));
        int h = Math.max(8, (int) Math.round(widget.h * sy));
        return new Rect(x, y, w, h);
    }

    private Rect settingValueRect(int rowX, int y, int rowW) {
        return new Rect(rowX + rowW - 132, y + 4, 122, 16);
    }

    private int detailSettingsContentY() {
        return detailsPanelRect().y + 168;
    }

    private Rect moduleColumnToggleRect(Rect row) {
        return new Rect(row.x + 4, row.y + 5, 5, 5);
    }

    private String trimToWidth(String value, int maxWidth) {
        if (value == null) return "";
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        int end = value.length();
        while (end > 0 && textRenderer.getWidth(value.substring(0, end)) + ellipsisWidth > maxWidth) end--;
        return value.substring(0, Math.max(0, end)) + ellipsis;
    }

    private String bindLabel(Module module) {
        if (module == null || module.keybind == null) return "None";
        String value = module.keybind.toString();
        if (value == null || value.isBlank() || "None".equalsIgnoreCase(value)) return "None";
        return shortLabel(value);
    }

    private String trim(net.minecraft.client.font.TextRenderer renderer, String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private String trimValue(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, maxChars);
    }

    private String shortLabel(String value) {
        if (value == null) return "";
        return value.length() > 14 ? value.substring(0, 11) + "..." : value;
    }

    private String profileLabel() {
        return ClientRuntime.theme().current() == eclipse.client.theme.ClientThemeId.DARK_MONO ? "Mono Dark" : "Mono Light";
    }

    private String headerSubtitle() {
        return switch (viewMode) {
            case WORKSPACE -> section.subtitle();
            case SPATIAL -> "Persistent waypoint and route management with compact inspection.";
            case HUD -> "Safe-area aware HUD layout with snap-based positioning.";
            case INSPECTOR -> "Performance, memory, and HUD metrics in one sampled surface.";
        };
    }

    private String headerNote() {
        return switch (viewMode) {
            case HUD -> "Layout studio with bounded drag and snap";
            case SPATIAL -> "Waypoint anchors, route editing, and spatial detail";
            case INSPECTOR -> "Bounded diagnostics with compact runtime health";
            default -> "";
        };
    }

    private String sectionBadge(ClientSection clientSection) {
        return switch (clientSection) {
            case OVERVIEW -> "All";
            case VISUALS -> "Vis";
            case MOVEMENT -> "Mov";
            case COMBAT -> "Cbt";
            case NETWORK -> "Net";
            case UTILITY -> "Utl";
            case CHAT -> "Msg";
        };
    }

    private Rect root() {
        return new Rect(ROOT_MARGIN, ROOT_MARGIN, uiWidth() - ROOT_MARGIN * 2, uiHeight() - ROOT_MARGIN * 2);
    }

    private Rect sidebarRect() {
        Rect root = root();
        return new Rect(root.x + 14, root.y + 14, SIDEBAR_W, root.h - 28);
    }

    private Rect modulesPanelRect() {
        Rect root = root();
        int x = root.x + 14;
        int y = contentTop();
        int available = root.w - 28;
        int width = Math.min(342, Math.max(306, available / 2 - 18));
        return new Rect(x, y, width, root.y + root.h - y - 14);
    }

    private Rect detailsPanelRect() {
        Rect root = root();
        int w = Math.min(304, Math.max(270, root.w / 3));
        int x = root.x + root.w - w - 14;
        int y = contentTop();
        return new Rect(x, y, w, root.y + root.h - y - 14);
    }

    private Rect hudListRect() {
        Rect root = root();
        int x = root.x + 14;
        int y = contentTop();
        return new Rect(x, y, 196, root.y + root.h - y - 14);
    }

    private Rect hudCanvasRect() {
        Rect list = hudListRect();
        int x = list.x + list.w + 14;
        int y = list.y;
        int remaining = root().x + root().w - 14 - x;
        int inspectorW = Math.max(214, Math.min(250, remaining / 3));
        int canvasW = Math.max(320, remaining - inspectorW - 14);
        return new Rect(x, y, canvasW, list.h);
    }

    private Rect hudInspectorRect() {
        Rect canvas = hudCanvasRect();
        int x = canvas.x + canvas.w + 14;
        int y = canvas.y;
        int w = root().x + root().w - 14 - x;
        return new Rect(x, y, w, canvas.h);
    }

    private Rect hudClientEditorRect() {
        Rect inspector = hudInspectorRect();
        return new Rect(inspector.x + 14, inspector.y + inspector.h - 136, inspector.w - 28, 92);
    }

    private Rect inspectorLeftRect() {
        Rect root = root();
        int x = root.x + 14;
        int y = contentTop();
        int remaining = root.x + root.w - 14 - x;
        int rightW = Math.max(214, Math.min(252, remaining / 3));
        return new Rect(x, y, remaining - rightW - 14, root.y + root.h - y - 14);
    }

    private Rect inspectorRightRect() {
        Rect left = inspectorLeftRect();
        int x = left.x + left.w + 14;
        int y = left.y;
        int w = root().x + root().w - 14 - x;
        return new Rect(x, y, w, left.h);
    }

    private Rect rowToggleRect(Rect row) {
        return new Rect(row.x + row.w - 32, row.y + 9, 24, 14);
    }

    private Rect detailToggleRect() {
        Rect panel = detailsPanelRect();
        return new Rect(panel.x + panel.w - 52, panel.y + 43, 30, 14);
    }

    private Rect detailCloseRect() {
        Rect panel = detailsPanelRect();
        return new Rect(panel.x + panel.w - 28, panel.y + 12, 16, 18);
    }

    private Rect detailBindRect() {
        Rect panel = detailsPanelRect();
        return new Rect(panel.x + 18, panel.y + 62, Math.max(96, panel.w - 94), 20);
    }

    private Rect detailBindClearRect() {
        Rect panel = detailsPanelRect();
        return new Rect(panel.x + panel.w - 68, panel.y + 62, 50, 20);
    }

    private Rect themeChipRect() {
        Rect root = root();
        if (macDock) return new Rect(root.x + root.w - 52, root.y + 4, 20, 20);
        return new Rect(root.x + root.w - 76, root.y + 4, 20, 20);
    }

    private Rect closeChipRect() {
        Rect root = root();
        if (macDock) return new Rect(root.x + root.w - 26, root.y + 4, 20, 20);
        return new Rect(root.x + root.w - 50, root.y + 4, 20, 20);
    }

    private Rect clearSearchRect() {
        Rect panel = modulesPanelRect();
        return new Rect(panel.x + panel.w - 52, panel.y + 40, 36, 20);
    }

    private Rect hudInnerCanvasRect() {
        Rect canvas = hudCanvasRect();
        return new Rect(canvas.x + 14, canvas.y + 40, canvas.w - 28, canvas.h - 74);
    }

    private Rect hudPlacementRect() {
        Rect inner = hudInnerCanvasRect();
        int targetW = Math.max(1, hudScreenWidth());
        int targetH = Math.max(1, hudScreenHeight());
        double scale = Math.min(inner.w / (double) targetW, inner.h / (double) targetH);
        int previewW = Math.max(1, (int) Math.round(targetW * scale));
        int previewH = Math.max(1, (int) Math.round(targetH * scale));
        return new Rect(inner.x + (inner.w - previewW) / 2, inner.y + (inner.h - previewH) / 2, previewW, previewH);
    }

    private Rect workspaceAreaRect() {
        Rect root = root();
        int y = contentTop();
        int rightReserve = selectedModule == null ? 0 : detailsPanelRect().w + 18;
        return new Rect(root.x + 14, y, root.w - 28 - rightReserve, root.y + root.h - y - 14);
    }

    private Rect workspaceColumnRect(int index, int total) {
        Rect area = workspaceAreaRect();
        int gap = 8;
        int width = Math.max(78, (area.w - gap * Math.max(0, total - 1)) / total);
        int used = width * total + gap * Math.max(0, total - 1);
        int x = area.x + Math.max(0, (area.w - used) / 2) + index * (width + gap);
        int h = Math.min(area.h, 250);
        int y = area.y + Math.max(0, (area.h - h) / 2) - 6;
        return new Rect(x, y, width, h);
    }

    private Rect moduleArrowRect(Rect row) {
        return new Rect(row.x + row.w - 16, row.y, 16, row.h);
    }

    private int contentTop() {
        return root().y + TOP_BAR_H + 14;
    }

    private void syncHudWidgetsToRuntime() {
        for (HudWidgetDraft draft : hudWidgets) {
            HudWidgetBinding binding = ClientRuntime.hud().find(draft.id);
            if (binding != null) {
                binding.setPosition(draft.x, draft.y);
                binding.setVisible(draft.visible);
            }
        }
    }

    private String prettyName(String id) {
        return switch (id) {
            case "coordinates-hud" -> "Coordinates";
            case "clock-hud" -> "Clock";
            case "nearest-waypoint" -> "Nearest Waypoint";
            case "active-modules" -> "Active Modules";
            case "debug-overlay" -> "Performance";
            default -> id;
        };
    }

    private Rect spatialWaypointsRect() {
        Rect root = root();
        int x = root.x + 14;
        int y = contentTop();
        int available = root.w - 28;
        int detailW = Math.max(210, Math.min(270, available / 3));
        int listW = Math.max(190, (available - detailW - 24) / 2);
        return new Rect(x, y, listW, root.y + root.h - y - 14);
    }

    private Rect spatialRoutesRect() {
        Rect waypoints = spatialWaypointsRect();
        return new Rect(waypoints.x + waypoints.w + 12, waypoints.y, waypoints.w, waypoints.h);
    }

    private Rect spatialDetailRect() {
        Rect routes = spatialRoutesRect();
        int x = routes.x + routes.w + 14;
        return new Rect(x, routes.y, root().x + root().w - 14 - x, routes.h);
    }

    private WaypointRecord selectedWaypoint() {
        return ClientRuntime.spatial().findWaypoint(selectedWaypointId);
    }

    private RouteRecord selectedRoute() {
        return ClientRuntime.spatial().findRoute(selectedRouteId);
    }

    private String nearestWaypointName(RouteRecord route) {
        if (route == null || route.waypointIds.isEmpty()) return "None";
        WaypointRecord waypoint = ClientRuntime.spatial().findWaypoint(route.waypointIds.get(0));
        return waypoint == null ? "None" : waypoint.name;
    }

    private int snap(int value, int step) {
        if (step <= 1) return value;
        return Math.round(value / (float) step) * step;
    }

    private float animationProgress() {
        long now = System.currentTimeMillis();
        long started = closing ? closingStartedAtMs : openedAtMs;
        return MathHelper.clamp((now - started) / (float) GUI_ANIMATION_MS, 0.0f, 1.0f);
    }

    private float smooth(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private int uiWidth() {
        if (client == null) return width;
        return Math.max(640, (int) Math.round(client.getWindow().getWidth() / fixedUiScale()));
    }

    private int uiHeight() {
        if (client == null) return height;
        return Math.max(360, (int) Math.round(client.getWindow().getHeight() / fixedUiScale()));
    }

    private int hudScreenWidth() {
        return Math.max(1, width);
    }

    private int hudScreenHeight() {
        return Math.max(1, height);
    }

    private double fixedUiScale() {
        if (client == null) return 1.0;
        double windowWidth = client.getWindow().getWidth();
        double windowHeight = client.getWindow().getHeight();
        double adaptive = Math.min(windowWidth / 1280.0, windowHeight / 720.0) * BASE_UI_SCALE;
        return MathHelper.clamp(adaptive, 1.12, 2.05);
    }

    private double drawScale() {
        if (client == null) return 1.0;
        return fixedUiScale() / client.getWindow().getScaleFactor();
    }

    private double toUiX(double mouseX) {
        if (client == null) return mouseX;
        return mouseX * client.getWindow().getScaleFactor() / fixedUiScale();
    }

    private double toUiY(double mouseY) {
        if (client == null) return mouseY;
        return mouseY * client.getWindow().getScaleFactor() / fixedUiScale();
    }

    private String username() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSession() == null) return "Player";
        return mc.getSession().getUsername();
    }

    private String categoryLabel(Module module) {
        if (module == null || module.category == null) return "Unknown";
        return module.category.name;
    }

    private record SidebarModeEntry(ViewMode mode, Rect bounds) {}
    private record SidebarSectionEntry(ClientSection section, Rect bounds) {}
    private record HeaderChip(ClientSection section, Rect bounds) {}
    private record ViewChip(ViewMode mode, Rect bounds) {}
    private record ModuleColumnEntry(Module module, Rect bounds) {}
    private record SettingEntry(String groupName, Setting<?> setting) {}

    private static final class HudWidgetDraft {
        private final String id;
        private final String name;
        private int x;
        private int y;
        private final int w;
        private final int h;
        private boolean visible;

        private HudWidgetDraft(String id, String name, int x, int y, int w, int h, boolean visible) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.visible = visible;
        }
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}

