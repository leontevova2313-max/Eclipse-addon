package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DuplicateNameGuard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> warnOnce = sgGeneral.add(new BoolSetting.Builder()
        .name("warn-once")
        .description("Only reports duplicate module names once per session.")
        .defaultValue(true)
        .build()
    );

    private final Set<String> reported = new HashSet<>();
    private int ticks;

    public DuplicateNameGuard() {
        super(Eclipse.CATEGORY, "eclipse-name-guard", "Reports duplicate Meteor module names before they cause mixin-side crashes.");
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        ticks = 0;
        reported.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;
        if (ticks % 100 != 0) return;

        Map<String, Integer> counts = new HashMap<>();
        for (Module module : Modules.get().getAll()) {
            counts.merge(module.name, 1, Integer::sum);
        }

        counts.forEach((name, count) -> {
            if (count <= 1) return;
            if (warnOnce.get() && !reported.add(name)) return;
            warning("Duplicate module name detected: %s (%d modules). Rename one addon module.", name, count);
        });
    }
}
