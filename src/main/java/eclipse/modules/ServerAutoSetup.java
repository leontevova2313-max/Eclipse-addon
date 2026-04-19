package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerAutoSetup extends Module {
    private static final DateTimeFormatter PROFILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> saveProfileBackup = sgGeneral.add(new BoolSetting.Builder()
        .name("save-profile-backup")
        .description("Saves current Meteor module config into a profile before applying server settings.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enableSafeModules = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-safe-modules")
        .description("Enables stable movement, combat, and server utility modules after configuring them.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableRiskyMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-risky-movement")
        .description("Disables obvious setback-heavy movement modules such as speed, flight, long jump, and packet movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableRiskyCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-risky-combat")
        .description("Disables high-noise aura modules that are not part of the Karasique baseline.")
        .defaultValue(true)
        .build()
    );

    public ServerAutoSetup() {
        super(Eclipse.CATEGORY, "server-auto-setup", "Applies a Karasique Meteor module profile without touching render, themes, or binds.");
    }

    @Override
    public void onActivate() {
        int changed = 0;
        int enabled = 0;
        int disabled = 0;

        if (saveProfileBackup.get()) saveBackupProfile();

        changed += configureKarasiqueBaseline();

        if (disableRiskyMovement.get()) {
            disabled += disable("speed", "flight", "elytra-fly", "long-jump", "high-jump", "step", "spider", "jesus", "blink", "click-tp", "air-jump", "scaffold");
        }

        if (disableRiskyCombat.get()) {
            disabled += disable("crystal-aura", "bed-aura", "anchor-aura", "auto-crystal", "auto-city", "auto-trap", "auto-web", "surround", "self-trap", "burrow");
        }

        if (enableSafeModules.get()) {
            enabled += enable("sprint", "kill-aura", "auto-totem", "auto-weapon", "auto-tool", "inventory-tweaks", "anti-packet-kick");
            enabled += enable("chat-fix", "eclipse-velocity", "pearl-phase", "eclipse-elytra", "server-diagnostics");
        }

        info("Karasique setup applied: %d settings, %d enabled, %d disabled.", changed, enabled, disabled);
        disable();
    }

    private int configureKarasiqueBaseline() {
        int changed = 0;

        changed += set("sprint", "sprint-mode", "Strict");
        changed += set("sprint", "keep-sprint", "false");
        changed += set("sprint", "unsprint-on-hit", "true");

        changed += set("kill-aura", "attack-when-holding", "Weapons");
        changed += set("kill-aura", "rotate", "OnHit");
        changed += set("kill-aura", "auto-switch", "false");
        changed += set("kill-aura", "shield-mode", "None");
        changed += set("kill-aura", "only-on-click", "false");
        changed += set("kill-aura", "only-on-look", "false");
        changed += set("kill-aura", "pause-baritone", "true");
        changed += set("kill-aura", "priority", "ClosestAngle");
        changed += set("kill-aura", "max-targets", "1");
        changed += set("kill-aura", "range", "4.0");
        changed += set("kill-aura", "walls-range", "2.6");
        changed += set("kill-aura", "ignore-named", "false");
        changed += set("kill-aura", "ignore-passive", "true");
        changed += set("kill-aura", "ignore-tamed", "true");
        changed += set("kill-aura", "pause-on-lag", "true");
        changed += set("kill-aura", "pause-on-use", "true");
        changed += set("kill-aura", "TPS-sync", "true");
        changed += set("kill-aura", "custom-delay", "false");
        changed += set("kill-aura", "hit-delay", "11");
        changed += set("kill-aura", "switch-delay", "2");

        changed += set("auto-totem", "mode", "Strict");
        changed += set("auto-totem", "delay", "1");
        changed += set("auto-weapon", "only-on-click", "false");
        changed += set("auto-tool", "switch-back", "true");

        changed += set("velocity", "knockback", "true");
        changed += set("velocity", "knockback-horizontal", "0");
        changed += set("velocity", "knockback-vertical", "0");
        changed += set("velocity", "explosions", "true");
        changed += set("velocity", "explosions-horizontal", "0");
        changed += set("velocity", "explosions-vertical", "0");
        changed += set("velocity", "entity-push", "true");
        changed += set("velocity", "entity-push-amount", "0");
        changed += set("velocity", "blocks", "true");
        changed += set("velocity", "sinking", "false");
        changed += set("velocity", "fishing", "false");

        changed += set("eclipse-velocity", "mode", "Scale");
        changed += set("eclipse-velocity", "horizontal", "0");
        changed += set("eclipse-velocity", "vertical", "0");
        changed += set("eclipse-velocity", "only-player-velocity", "true");
        changed += set("eclipse-velocity", "explosions", "true");
        changed += set("eclipse-velocity", "explosion-horizontal", "0");
        changed += set("eclipse-velocity", "explosion-vertical", "0");
        changed += set("eclipse-velocity", "correction-pause", "0");
        changed += set("eclipse-velocity", "correction-policy", "Off");

        changed += set("pearl-phase", "throw-pitch", "80");
        changed += set("pearl-phase", "packet-pitch", "80");
        changed += set("pearl-phase", "yaw-offset", "0");
        changed += set("pearl-phase", "packets", "8");
        changed += set("pearl-phase", "horizontal", "0.055");
        changed += set("pearl-phase", "vertical", "-0.032");
        changed += set("pearl-phase", "full-packets", "true");
        changed += set("pearl-phase", "stop-velocity", "true");
        changed += set("pearl-phase", "cancel-velocity-packets", "true");
        changed += set("pearl-phase", "disable-after", "60");

        changed += set("eclipse-elytra", "mode", "Grim");
        changed += set("eclipse-elytra", "auto-start", "true");
        changed += set("eclipse-elytra", "auto-firework", "true");
        changed += set("eclipse-elytra", "firework-cooldown", "34");
        changed += set("eclipse-elytra", "control-response", "0.55");
        changed += set("eclipse-elytra", "rocket-slot", "9");
        changed += set("eclipse-elytra", "move-rockets-to-slot", "true");
        changed += set("eclipse-elytra", "rocket-swap-back", "true");
        changed += set("eclipse-elytra", "server-safe", "true");
        changed += set("eclipse-elytra", "correction-recovery", "RetryTakeoff");
        changed += set("eclipse-elytra", "correction-pause", "45");

        changed += set("server-diagnostics", "network-diagnostics", "true");
        changed += set("server-diagnostics", "movement-diagnostics", "true");
        changed += set("server-diagnostics", "combat-interaction-diagnostics", "true");
        changed += set("server-diagnostics", "module-context", "true");
        changed += set("server-diagnostics", "packet-mode", "ImportantOnly");
        changed += set("server-diagnostics", "history-limit", "300");
        changed += set("server-diagnostics", "throttle-ms", "750");
        changed += set("server-diagnostics", "movement-sample-interval", "5");
        changed += set("server-diagnostics", "export-on-close", "true");
        changed += set("server-diagnostics", "print-summary", "true");

        changed += set("litematica-printer", "blocks-per-tick", "1");
        changed += set("litematica-printer", "horizontal-range", "5");
        changed += set("litematica-printer", "vertical-range", "5");
        changed += set("litematica-printer", "scan-limit", "1600");
        changed += set("litematica-printer", "tick-delay", "2");
        changed += set("litematica-printer", "build-order", "StableSupport");
        changed += set("litematica-printer", "exact-state", "true");
        changed += set("litematica-printer", "important-state", "true");
        changed += set("litematica-printer", "replace-wrong-state", "true");
        changed += set("litematica-printer", "correction-pause", "60");
        changed += set("litematica-printer", "retry-delay", "12");
        changed += set("litematica-printer", "max-retries", "3");
        changed += set("litematica-printer", "skip-impossible-ticks", "100");
        changed += set("litematica-printer", "pause-when-missing-blocks", "true");
        changed += set("litematica-printer", "meteor-air-place", "true");
        changed += set("litematica-printer", "progress-scan-per-tick", "1200");

        return changed;
    }

    private void saveBackupProfile() {
        Profile profile = new Profile();
        profile.name.set("Eclipse backup " + LocalDateTime.now().format(PROFILE_TIME));
        profile.modules.set(true);
        profile.hud.set(false);
        profile.macros.set(false);
        profile.waypoints.set(false);
        Profiles.get().add(profile);
        info("Saved current Meteor modules as profile: %s", profile.name.get());
    }

    private int set(String moduleName, String settingName, String value) {
        Module module = Modules.get().get(moduleName);
        if (module == null) return 0;

        for (SettingGroup group : module.settings) {
            Setting<?> setting = group.get(settingName);
            if (setting != null && setting.parse(value)) return 1;
        }

        return 0;
    }

    private int enable(String... moduleNames) {
        int count = 0;
        for (String moduleName : moduleNames) {
            Module module = Modules.get().get(moduleName);
            if (module != null && !module.isActive()) {
                module.enable();
                count++;
            }
        }
        return count;
    }

    private int disable(String... moduleNames) {
        int count = 0;
        for (String moduleName : moduleNames) {
            Module module = Modules.get().get(moduleName);
            if (module != null && module.isActive()) {
                module.disable();
                count++;
            }
        }
        return count;
    }
}
