package eclipse.client.runtime;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client-side catalog over the host module list. It hides the addon structure from the Eclipse
 * GUI and gives the client layer a stable, section-oriented view of modules.
 */
public final class ClientModuleCatalog {
    private final EnumMap<ClientSection, List<Module>> sectionCache = new EnumMap<>(ClientSection.class);
    private Supplier<List<Module>> source = List::of;
    private boolean dirty = true;

    public ClientModuleCatalog() {
        for (ClientSection section : ClientSection.values()) {
            sectionCache.put(section, new ArrayList<>());
        }
    }

    public void setSource(Supplier<List<Module>> source) {
        this.source = source == null ? List::of : source;
        this.dirty = true;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public List<Module> modules(ClientSection section) {
        refreshIfNeeded();
        return sectionCache.getOrDefault(section, List.of());
    }

    public List<Module> modules(ClientSection section, String query) {
        refreshIfNeeded();
        List<Module> sourceList = sectionCache.getOrDefault(section, List.of());
        if (query == null || query.isBlank()) return sourceList;

        String lowered = query.toLowerCase(Locale.ROOT);
        List<Module> result = new ArrayList<>(sourceList.size());
        for (Module module : sourceList) {
            String title = module.title == null ? "" : module.title.toLowerCase(Locale.ROOT);
            String description = module.description == null ? "" : module.description.toLowerCase(Locale.ROOT);
            if (title.contains(lowered) || description.contains(lowered)) result.add(module);
        }
        return result;
    }

    public int totalCount() {
        refreshIfNeeded();
        int count = 0;
        for (ClientSection section : ClientSection.values()) {
            if (section == ClientSection.OVERVIEW) continue;
            count += sectionCache.get(section).size();
        }
        return count;
    }

    public int activeCount() {
        refreshIfNeeded();
        int count = 0;
        for (ClientSection section : ClientSection.values()) {
            if (section == ClientSection.OVERVIEW) continue;
            for (Module module : sectionCache.get(section)) if (module.isActive()) count++;
        }
        return count;
    }

    public int activeCount(ClientSection section) {
        refreshIfNeeded();
        int count = 0;
        for (Module module : sectionCache.getOrDefault(section, List.of())) if (module.isActive()) count++;
        return count;
    }

    private void refreshIfNeeded() {
        if (!dirty) return;
        dirty = false;
        for (List<Module> modules : sectionCache.values()) modules.clear();

        List<Module> modules = source.get();
        if (modules == null || modules.isEmpty()) return;

        List<Module> overview = sectionCache.get(ClientSection.OVERVIEW);
        for (Module module : modules) {
            ClientSection section = map(module.category);
            if (section == null) continue;
            sectionCache.get(section).add(module);
            overview.add(module);
        }
    }

    private ClientSection map(Category category) {
        if (category == null) return null;
        if (category == Eclipse.VISUALS) return ClientSection.VISUALS;
        if (category == Eclipse.MOVEMENT) return ClientSection.MOVEMENT;
        if (category == Eclipse.COMBAT) return ClientSection.COMBAT;
        if (category == Eclipse.NETWORK) return ClientSection.NETWORK;
        if (category == Eclipse.UTILITY) return ClientSection.UTILITY;
        if (category == Eclipse.CHAT) return ClientSection.CHAT;
        return null;
    }
}
