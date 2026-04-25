package eclipse.gui.client.bridge;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class EclipseClientBridge {
    public enum Section {
        OVERVIEW("Overview", null),
        VISUALS("Visuals", Eclipse.VISUALS),
        MOVEMENT("Movement", Eclipse.MOVEMENT),
        COMBAT("Combat", Eclipse.COMBAT),
        NETWORK("Network", Eclipse.NETWORK),
        UTILITY("Utility", Eclipse.UTILITY),
        CHAT("Chat", Eclipse.CHAT);

        public final String title;
        public final Category category;

        Section(String title, Category category) {
            this.title = title;
            this.category = category;
        }
    }

    private EclipseClientBridge() {}

    public static List<Module> modules(Section section) {
        Modules registry = Modules.get();
        List<Module> result = new ArrayList<>(48);
        if (registry == null) return result;

        if (section == Section.OVERVIEW) {
            append(result, registry.getGroup(Eclipse.VISUALS));
            append(result, registry.getGroup(Eclipse.MOVEMENT));
            append(result, registry.getGroup(Eclipse.COMBAT));
            append(result, registry.getGroup(Eclipse.NETWORK));
            append(result, registry.getGroup(Eclipse.UTILITY));
            append(result, registry.getGroup(Eclipse.CHAT));
        } else if (section.category != null) {
            append(result, registry.getGroup(section.category));
        }

        return result;
    }

    private static void append(List<Module> target, Collection<Module> source) {
        for (Module module : source) target.add(module);
    }

    public static int totalModuleCount() {
        Modules registry = Modules.get();
        if (registry == null) return 0;
        int count = 0;
        count += registry.getGroup(Eclipse.VISUALS).size();
        count += registry.getGroup(Eclipse.MOVEMENT).size();
        count += registry.getGroup(Eclipse.COMBAT).size();
        count += registry.getGroup(Eclipse.NETWORK).size();
        count += registry.getGroup(Eclipse.UTILITY).size();
        count += registry.getGroup(Eclipse.CHAT).size();
        return count;
    }

    public static int activeModuleCount() {
        Modules registry = Modules.get();
        if (registry == null) return 0;
        int count = 0;
        for (Module module : registry.getGroup(Eclipse.VISUALS)) if (module.isActive()) count++;
        for (Module module : registry.getGroup(Eclipse.MOVEMENT)) if (module.isActive()) count++;
        for (Module module : registry.getGroup(Eclipse.COMBAT)) if (module.isActive()) count++;
        for (Module module : registry.getGroup(Eclipse.NETWORK)) if (module.isActive()) count++;
        for (Module module : registry.getGroup(Eclipse.UTILITY)) if (module.isActive()) count++;
        for (Module module : registry.getGroup(Eclipse.CHAT)) if (module.isActive()) count++;
        return count;
    }

    public static String username() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSession() == null) return "Player";
        return client.getSession().getUsername();
    }

    public static String stateText(Module module) {
        return module.isActive() ? "Enabled" : "Disabled";
    }

    public static String categoryLabel(Module module) {
        if (module == null || module.category == null) return "Unknown";
        return module.category.name;
    }
}
