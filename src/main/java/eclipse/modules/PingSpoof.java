package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay applied to selected latency response packets, in milliseconds.")
        .defaultValue(750)
        .range(50, 10000)
        .sliderRange(50, 3000)
        .build()
    );

    private final Setting<Boolean> keepAlive = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-alive")
        .description("Delays KeepAlive response packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pong = sgGeneral.add(new BoolSetting.Builder()
        .name("pong")
        .description("Delays CommonPong response packets.")
        .defaultValue(true)
        .build()
    );

    private final List<DelayedPacket> packets = new ArrayList<>();
    private boolean sendingDelayed;

    public PingSpoof() {
        super(Eclipse.CATEGORY, "ping-spoof", "Queues selected latency packets and sends them after a configurable delay.");
    }

    @Override
    public void onDeactivate() {
        flush();
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (sendingDelayed) return;

        if (shouldDelay(event.packet)) {
            packets.add(new DelayedPacket(event.packet, System.currentTimeMillis() + delay.get()));
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        Iterator<DelayedPacket> iterator = packets.iterator();

        while (iterator.hasNext()) {
            DelayedPacket delayed = iterator.next();
            if (delayed.sendAt > now) continue;

            sendDelayed(delayed.packet);
            iterator.remove();
        }
    }

    private boolean shouldDelay(Packet<?> packet) {
        return (keepAlive.get() && packet instanceof KeepAliveC2SPacket)
            || (pong.get() && packet instanceof CommonPongC2SPacket);
    }

    private void flush() {
        if (mc.getNetworkHandler() != null) {
            for (DelayedPacket delayed : packets) {
                sendDelayed(delayed.packet);
            }
        }

        packets.clear();
    }

    private void sendDelayed(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        sendingDelayed = true;
        mc.getNetworkHandler().getConnection().send(packet);
        sendingDelayed = false;
    }

    private record DelayedPacket(Packet<?> packet, long sendAt) {
    }
}
