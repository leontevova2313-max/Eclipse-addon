package eclipse;

import eclipse.modules.AntiCrash;
import eclipse.modules.ChatFix;
import eclipse.modules.EclipseVisuals;
import eclipse.modules.CameraTweaks;
import eclipse.modules.CustomPackets;
import eclipse.modules.DuplicateNameGuard;
import eclipse.modules.EclipseFlight;
import eclipse.modules.EclipseMove;
import eclipse.modules.EclipseNoSlow;
import eclipse.modules.ExternalCheatTrace;
import eclipse.modules.ExtraElytra;
import eclipse.modules.LitematicaPrinter;
import eclipse.modules.MiddleClickInfo;
import eclipse.modules.PearlPhase;
import eclipse.modules.PingSpoof;
import eclipse.modules.ServerAutoSetup;
import eclipse.modules.ServerDiagnostics;
import eclipse.modules.ServerIntel;
import eclipse.modules.Velocity;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class Eclipse extends MeteorAddon {
    public static final Category CATEGORY = new Category("Eclipse");

    @Override
    public void onInitialize() {
        Modules.get().add(new AntiCrash());
        Modules.get().add(new ChatFix());
        Modules.get().add(new CameraTweaks());
        Modules.get().add(new CustomPackets());
        Modules.get().add(new DuplicateNameGuard());
        Modules.get().add(new EclipseFlight());
        Modules.get().add(new EclipseMove());
        Modules.get().add(new EclipseNoSlow());
        Modules.get().add(new EclipseVisuals());
        Modules.get().add(new ExternalCheatTrace());
        Modules.get().add(new ExtraElytra());
        Modules.get().add(new LitematicaPrinter());
        Modules.get().add(new MiddleClickInfo());
        Modules.get().add(new PearlPhase());
        Modules.get().add(new PingSpoof());
        Modules.get().add(new ServerAutoSetup());
        Modules.get().add(new ServerDiagnostics());
        Modules.get().add(new ServerIntel());
        Modules.get().add(new Velocity());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "eclipse";
    }
}
