package eclipse;

import eclipse.modules.chat.ChatLinks;
import eclipse.modules.chat.ChatPrefix;
import eclipse.modules.chat.ColorChat;
import eclipse.modules.combat.CrystalKiller;
import eclipse.modules.combat.Killer;
import eclipse.modules.combat.Velocity;
import eclipse.modules.movement.ExtraElytra;
import eclipse.modules.movement.PearlPhase;
import eclipse.modules.network.AntiCrash;
import eclipse.modules.network.CustomPackets;
import eclipse.modules.network.PingSpoof;
import eclipse.modules.network.ServerDiagnostics;
import eclipse.modules.utility.LitematicaPrinter;
import eclipse.modules.utility.MiddleClickInfo;
import eclipse.modules.utility.ServerAutoSetup;
import eclipse.modules.visuals.CameraTweaks;
import eclipse.modules.visuals.EclipseCrosshair;
import eclipse.modules.visuals.EclipseVisuals;
import eclipse.gui.theme.EclipseThemeBootstrap;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;

public class Eclipse extends MeteorAddon {
    public static final Category COMBAT = new Category("Eclipse Combat");
    public static final Category MOVEMENT = new Category("Eclipse Movement");
    public static final Category VISUALS = new Category("Eclipse Visuals");
    public static final Category CHAT = new Category("Eclipse Chat");
    public static final Category UTILITY = new Category("Eclipse Utility");
    public static final Category NETWORK = new Category("Eclipse Network");
    private static final String DIAGNOSTICS_MARKER = ".eclipse-dev-diagnostics";

    @Override
    public void onInitialize() {
        EclipseThemeBootstrap.ensureRegisteredAndSelected();

        Modules.get().add(new AntiCrash());
        Modules.get().add(new CrystalKiller());
        Modules.get().add(new Killer());
        Modules.get().add(new ChatLinks());
        Modules.get().add(new ChatPrefix());
        Modules.get().add(new ColorChat());
        Modules.get().add(new CameraTweaks());
        Modules.get().add(new CustomPackets());
        Modules.get().add(new EclipseCrosshair());
        Modules.get().add(new EclipseVisuals());
        Modules.get().add(new ExtraElytra());
        Modules.get().add(new LitematicaPrinter());
        Modules.get().add(new MiddleClickInfo());
        Modules.get().add(new PearlPhase());
        Modules.get().add(new PingSpoof());
        Modules.get().add(new ServerAutoSetup());
        if (isDeveloperDiagnosticsEnabled()) {
            Modules.get().add(new ServerDiagnostics());
        }
        Modules.get().add(new Velocity());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(COMBAT);
        Modules.registerCategory(MOVEMENT);
        Modules.registerCategory(VISUALS);
        Modules.registerCategory(CHAT);
        Modules.registerCategory(UTILITY);
        Modules.registerCategory(NETWORK);
    }

    @Override
    public String getPackage() {
        return "eclipse";
    }

    private boolean isDeveloperDiagnosticsEnabled() {
        return isLocalDiagnosticsMarkerPresent()
            || Boolean.getBoolean("eclipse.dev.diagnostics")
            || "true".equalsIgnoreCase(System.getenv("ECLIPSE_DEV_DIAGNOSTICS"));
    }

    private boolean isLocalDiagnosticsMarkerPresent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.runDirectory == null) return false;

        Path runDirectory = client.runDirectory.toPath().toAbsolutePath().normalize();
        if (Files.exists(runDirectory.resolve(DIAGNOSTICS_MARKER))) return true;

        Path instanceDirectory = runDirectory.getParent();
        return instanceDirectory != null && Files.exists(instanceDirectory.resolve(DIAGNOSTICS_MARKER));
    }

    public static boolean isEclipseCategory(Category category) {
        return category == COMBAT
            || category == MOVEMENT
            || category == VISUALS
            || category == CHAT
            || category == UTILITY
            || category == NETWORK;
    }
}
