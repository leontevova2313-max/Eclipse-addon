package eclipse;

import eclipse.client.runtime.ClientRuntime;
import eclipse.client.profiles.ProfileStore;
import eclipse.modules.chat.ChatLinks;
import eclipse.modules.chat.ChatPrefix;
import eclipse.modules.combat.CrystalKiller;
import eclipse.modules.combat.Killer;
import eclipse.modules.combat.Velocity;
import eclipse.modules.movement.AutoFireworks;
import eclipse.modules.movement.ChorusClickTP;
import eclipse.modules.movement.Fly;
import eclipse.modules.movement.ExtraElytra;
import eclipse.modules.movement.PearlPhase;
import eclipse.modules.network.AntiCrash;
import eclipse.modules.network.CustomPackets;
import eclipse.modules.network.PingSpoof;
import eclipse.modules.network.ServerDiagnostics;
import eclipse.modules.utility.InventoryPresets;
import eclipse.modules.utility.LitematicaPrinter;
import eclipse.modules.utility.Profiles;
import eclipse.modules.utility.MiddleClickInfo;
import eclipse.modules.visuals.CameraTweaks;
import eclipse.modules.visuals.CrosshairInfo;
import eclipse.modules.visuals.EntityEsp;
import eclipse.modules.visuals.EntityTracers;
import eclipse.modules.visuals.EclipseCrosshair;
import eclipse.modules.visuals.EclipseVisuals;
import eclipse.modules.visuals.LightMeter;
import eclipse.modules.visuals.ScreenshotGrid;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        ClientRuntime.bootstrap();
        ProfileStore.bootstrapAutosafe();
        ClientRuntime.setModuleSupplier(Eclipse::allModules);

        Modules.get().add(new AntiCrash());
        Modules.get().add(new CrystalKiller());
        Modules.get().add(new Killer());
        Modules.get().add(new ChatLinks());
        Modules.get().add(new ChatPrefix());
        Modules.get().add(new CameraTweaks());
        Modules.get().add(new CrosshairInfo());
        Modules.get().add(new CustomPackets());
        Modules.get().add(new EntityEsp());
        Modules.get().add(new EntityTracers());
        Modules.get().add(new EclipseCrosshair());
        Modules.get().add(new EclipseVisuals());
        Modules.get().add(new AutoFireworks());
        Modules.get().add(new ChorusClickTP());
        Modules.get().add(new Fly());
        Modules.get().add(new ExtraElytra());
        Modules.get().add(new LightMeter());
        Modules.get().add(new InventoryPresets());
        Modules.get().add(new LitematicaPrinter());
        Modules.get().add(new MiddleClickInfo());
        Modules.get().add(new PearlPhase());
        Modules.get().add(new PingSpoof());
        Modules.get().add(new Profiles());
        Modules.get().add(new ScreenshotGrid());
        if (isDeveloperDiagnosticsEnabled()) {
            Modules.get().add(new ServerDiagnostics());
        }
        Modules.get().add(new Velocity());

        ClientRuntime.applyModuleStates(Eclipse::allModules);
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

    public static List<Module> allModules() {
        Modules registry = Modules.get();
        List<Module> modules = new ArrayList<>();
        modules.addAll(registry.getGroup(COMBAT));
        modules.addAll(registry.getGroup(MOVEMENT));
        modules.addAll(registry.getGroup(VISUALS));
        modules.addAll(registry.getGroup(CHAT));
        modules.addAll(registry.getGroup(UTILITY));
        modules.addAll(registry.getGroup(NETWORK));
        return modules;
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
