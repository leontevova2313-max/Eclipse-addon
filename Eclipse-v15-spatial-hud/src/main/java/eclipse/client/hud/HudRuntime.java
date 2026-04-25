package eclipse.client.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class HudRuntime {
    private final List<HudWidgetBinding> widgets = new ArrayList<>();

    public void register(HudWidgetBinding binding) {
        widgets.add(binding);
    }

    public List<HudWidgetBinding> widgets() {
        return Collections.unmodifiableList(widgets);
    }

    public List<HudWidgetBinding> visibleWidgets(Predicate<String> moduleEnabled) {
        List<HudWidgetBinding> out = new ArrayList<>();
        for (HudWidgetBinding binding : widgets) {
            if (!binding.visible()) continue;
            if (binding.moduleId() != null && !moduleEnabled.test(binding.moduleId())) continue;
            out.add(binding);
        }
        return out;
    }

    public int visibleCount() {
        int count = 0;
        for (HudWidgetBinding binding : widgets) if (binding.visible()) count++;
        return count;
    }

    public HudWidgetBinding find(String id) {
        for (HudWidgetBinding binding : widgets) if (binding.widgetId().equals(id)) return binding;
        return null;
    }
}
