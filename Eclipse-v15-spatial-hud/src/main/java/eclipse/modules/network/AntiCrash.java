package eclipse.modules.network;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

public class AntiCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> velocity = sgGeneral.add(new BoolSetting.Builder()
        .name("velocity")
        .description("Cancels impossible velocity packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Cancels suspicious explosion knockback packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxVelocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-velocity")
        .description("Maximum accepted velocity vector length.")
        .defaultValue(8.0)
        .range(1.0, 200.0)
        .sliderRange(1.0, 40.0)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> maxExplosionRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-explosion-radius")
        .description("Maximum accepted explosion radius.")
        .defaultValue(12.0)
        .range(1.0, 200.0)
        .sliderRange(1.0, 40.0)
        .decimalPlaces(1)
        .build()
    );

    public AntiCrash() {
        super(Eclipse.NETWORK, "eclipse-anti-crash", "Cancels a small set of suspicious packets that can destabilize the client.");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (velocity.get() && event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getVelocity().length() > maxVelocity.get()) {
                event.setCancelled(true);
                warning("Cancelled suspicious velocity packet.");
            }
        } else if (explosions.get() && event.packet instanceof ExplosionS2CPacket packet) {
            Vec3d knockback = packet.playerKnockback().orElse(Vec3d.ZERO);
            if (packet.radius() > maxExplosionRadius.get() || knockback.length() > maxVelocity.get()) {
                event.setCancelled(true);
                warning("Cancelled suspicious explosion packet.");
            }
        }
    }
}

