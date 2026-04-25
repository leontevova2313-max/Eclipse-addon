package eclipse.modules.visuals;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class EntityTracers extends Module {
    private enum TargetPoint {
        Feet,
        Middle,
        Head
    }

    private final SettingGroup sgTargets = settings.getDefaultGroup();
    private final SettingGroup sgStyle = settings.createGroup("Style");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgHostiles = settings.createGroup("Hostiles");
    private final SettingGroup sgPassives = settings.createGroup("Passives");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");

    private final Setting<Boolean> players = sgTargets.add(new BoolSetting.Builder()
        .name("players")
        .description("Draw tracers to players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hostiles = sgTargets.add(new BoolSetting.Builder()
        .name("hostiles")
        .description("Draw tracers to hostile mobs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> passives = sgTargets.add(new BoolSetting.Builder()
        .name("passives")
        .description("Draw tracers to passive mobs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> items = sgTargets.add(new BoolSetting.Builder()
        .name("items")
        .description("Draw tracers to dropped items.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> crystals = sgTargets.add(new BoolSetting.Builder()
        .name("crystals")
        .description("Draw tracers to end crystals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreInvisible = sgTargets.add(new BoolSetting.Builder()
        .name("ignore-invisible")
        .description("Skips invisible entities.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxDistance = sgTargets.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum tracer range.")
        .defaultValue(96.0)
        .range(8.0, 256.0)
        .sliderRange(16.0, 160.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<TargetPoint> targetPoint = sgStyle.add(new EnumSetting.Builder<TargetPoint>()
        .name("target-point")
        .description("Where the tracer line ends.")
        .defaultValue(TargetPoint.Middle)
        .build()
    );

    private final Setting<SettingColor> playerColor = sgPlayers.add(new ColorSetting.Builder()
        .name("player-color")
        .description("Tracer color for players.")
        .defaultValue(new SettingColor(84, 184, 255, 225))
        .visible(players::get)
        .build()
    );

    private final Setting<SettingColor> hostileColor = sgHostiles.add(new ColorSetting.Builder()
        .name("hostile-color")
        .description("Tracer color for hostile mobs.")
        .defaultValue(new SettingColor(255, 92, 92, 225))
        .visible(hostiles::get)
        .build()
    );

    private final Setting<SettingColor> passiveColor = sgPassives.add(new ColorSetting.Builder()
        .name("passive-color")
        .description("Tracer color for passive mobs.")
        .defaultValue(new SettingColor(114, 255, 146, 220))
        .visible(passives::get)
        .build()
    );

    private final Setting<SettingColor> itemColor = sgItems.add(new ColorSetting.Builder()
        .name("item-color")
        .description("Tracer color for dropped items.")
        .defaultValue(new SettingColor(255, 214, 92, 210))
        .visible(items::get)
        .build()
    );

    private final Setting<SettingColor> crystalColor = sgCrystals.add(new ColorSetting.Builder()
        .name("crystal-color")
        .description("Tracer color for end crystals.")
        .defaultValue(new SettingColor(214, 108, 255, 225))
        .visible(crystals::get)
        .build()
    );

    public EntityTracers() {
        super(Eclipse.VISUALS, "tracers", "Draws tracer lines to players, mobs, items, and crystals.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        double maxDistanceSq = maxDistance.get() * maxDistance.get();
        Vec3d start = mc.player.getEyePos();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity, maxDistanceSq)) continue;

            Vec3d end = endPoint(entity);
            Color color = lineColor(entity);
            event.renderer.line(start.x, start.y, start.z, end.x, end.y, end.z, color);
        }
    }

    private boolean shouldRender(Entity entity, double maxDistanceSq) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (ignoreInvisible.get() && entity.isInvisible()) return false;
        if (mc.player.squaredDistanceTo(entity) > maxDistanceSq) return false;

        if (entity instanceof PlayerEntity) return players.get();
        if (entity instanceof EndCrystalEntity) return crystals.get();
        if (entity instanceof HostileEntity) return hostiles.get();
        if (entity instanceof PassiveEntity) return passives.get();
        if (entity instanceof ItemEntity) return items.get();

        return false;
    }

    private Vec3d endPoint(Entity entity) {
        return switch (targetPoint.get()) {
            case Feet -> new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            case Head -> new Vec3d(entity.getX(), entity.getY() + entity.getHeight(), entity.getZ());
            case Middle -> new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
        };
    }

    private Color lineColor(Entity entity) {
        SettingColor color = colorFor(entity);
        return new Color(color.r, color.g, color.b, color.a);
    }

    private SettingColor colorFor(Entity entity) {
        if (entity instanceof PlayerEntity) return playerColor.get();
        if (entity instanceof EndCrystalEntity) return crystalColor.get();
        if (entity instanceof HostileEntity) return hostileColor.get();
        if (entity instanceof PassiveEntity) return passiveColor.get();
        if (entity instanceof ItemEntity) return itemColor.get();
        return playerColor.get();
    }
}
