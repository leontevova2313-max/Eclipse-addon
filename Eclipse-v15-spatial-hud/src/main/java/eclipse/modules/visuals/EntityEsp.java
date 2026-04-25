package eclipse.modules.visuals;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
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
import net.minecraft.util.math.Box;

public class EntityEsp extends Module {
    private final SettingGroup sgTargets = settings.getDefaultGroup();
    private final SettingGroup sgStyle = settings.createGroup("Style");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgHostiles = settings.createGroup("Hostiles");
    private final SettingGroup sgPassives = settings.createGroup("Passives");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");

    private final Setting<Boolean> players = sgTargets.add(new BoolSetting.Builder()
        .name("players")
        .description("Highlights players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hostiles = sgTargets.add(new BoolSetting.Builder()
        .name("hostiles")
        .description("Highlights hostile mobs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> passives = sgTargets.add(new BoolSetting.Builder()
        .name("passives")
        .description("Highlights passive mobs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> items = sgTargets.add(new BoolSetting.Builder()
        .name("items")
        .description("Highlights dropped items.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> crystals = sgTargets.add(new BoolSetting.Builder()
        .name("crystals")
        .description("Highlights end crystals.")
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
        .description("Maximum highlight range.")
        .defaultValue(64.0)
        .range(8.0, 256.0)
        .sliderRange(16.0, 128.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgStyle.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How boxes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> playerSide = sgPlayers.add(new ColorSetting.Builder()
        .name("player-side-color")
        .description("Player fill color.")
        .defaultValue(new SettingColor(84, 184, 255, 30))
        .visible(() -> players.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> playerLine = sgPlayers.add(new ColorSetting.Builder()
        .name("player-line-color")
        .description("Player outline color.")
        .defaultValue(new SettingColor(84, 184, 255, 235))
        .visible(players::get)
        .build()
    );

    private final Setting<SettingColor> hostileSide = sgHostiles.add(new ColorSetting.Builder()
        .name("hostile-side-color")
        .description("Hostile fill color.")
        .defaultValue(new SettingColor(255, 92, 92, 26))
        .visible(() -> hostiles.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> hostileLine = sgHostiles.add(new ColorSetting.Builder()
        .name("hostile-line-color")
        .description("Hostile outline color.")
        .defaultValue(new SettingColor(255, 92, 92, 235))
        .visible(hostiles::get)
        .build()
    );

    private final Setting<SettingColor> passiveSide = sgPassives.add(new ColorSetting.Builder()
        .name("passive-side-color")
        .description("Passive fill color.")
        .defaultValue(new SettingColor(114, 255, 146, 24))
        .visible(() -> passives.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> passiveLine = sgPassives.add(new ColorSetting.Builder()
        .name("passive-line-color")
        .description("Passive outline color.")
        .defaultValue(new SettingColor(114, 255, 146, 225))
        .visible(passives::get)
        .build()
    );

    private final Setting<SettingColor> itemSide = sgItems.add(new ColorSetting.Builder()
        .name("item-side-color")
        .description("Item fill color.")
        .defaultValue(new SettingColor(255, 214, 92, 20))
        .visible(() -> items.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> itemLine = sgItems.add(new ColorSetting.Builder()
        .name("item-line-color")
        .description("Item outline color.")
        .defaultValue(new SettingColor(255, 214, 92, 220))
        .visible(items::get)
        .build()
    );

    private final Setting<SettingColor> crystalSide = sgCrystals.add(new ColorSetting.Builder()
        .name("crystal-side-color")
        .description("Crystal fill color.")
        .defaultValue(new SettingColor(214, 108, 255, 26))
        .visible(() -> crystals.get() && shapeMode.get() != ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> crystalLine = sgCrystals.add(new ColorSetting.Builder()
        .name("crystal-line-color")
        .description("Crystal outline color.")
        .defaultValue(new SettingColor(214, 108, 255, 235))
        .visible(crystals::get)
        .build()
    );

    public EntityEsp() {
        super(Eclipse.VISUALS, "entity-esp", "Highlights players, mobs, items, and crystals through walls.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        double maxDistanceSq = maxDistance.get() * maxDistance.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!shouldRender(entity, maxDistanceSq)) continue;

            Box box = entity.getBoundingBox();
            Color side = sideColor(entity);
            Color line = lineColor(entity);

            event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, side, line, shapeMode.get(), 0);
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

    private Color sideColor(Entity entity) {
        SettingColor color = colorFor(entity, true);
        return new Color(color.r, color.g, color.b, color.a);
    }

    private Color lineColor(Entity entity) {
        SettingColor color = colorFor(entity, false);
        return new Color(color.r, color.g, color.b, color.a);
    }

    private SettingColor colorFor(Entity entity, boolean side) {
        if (entity instanceof PlayerEntity) return side ? playerSide.get() : playerLine.get();
        if (entity instanceof EndCrystalEntity) return side ? crystalSide.get() : crystalLine.get();
        if (entity instanceof HostileEntity) return side ? hostileSide.get() : hostileLine.get();
        if (entity instanceof PassiveEntity) return side ? passiveSide.get() : passiveLine.get();
        if (entity instanceof ItemEntity) return side ? itemSide.get() : itemLine.get();
        return side ? playerSide.get() : playerLine.get();
    }
}
