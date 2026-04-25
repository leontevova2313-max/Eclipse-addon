package eclipse.modules.network;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class CustomPackets extends Module {
    public enum PacketMode {
        MovementPulse,
        SlotPulse,
        SprintPulse,
        ElytraPulse
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<PacketMode> mode = sgGeneral.add(new EnumSetting.Builder<PacketMode>()
        .name("mode")
        .description("Packet pattern to send.")
        .defaultValue(PacketMode.SlotPulse)
        .build()
    );

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("interval")
        .description("Ticks between packet pulses.")
        .defaultValue(20)
        .range(1, 200)
        .sliderRange(1, 80)
        .build()
    );

    private final Setting<Integer> repeats = sgGeneral.add(new IntSetting.Builder()
        .name("repeats")
        .description("Packets sent per pulse.")
        .defaultValue(1)
        .range(1, 20)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Disables after one pulse.")
        .defaultValue(true)
        .build()
    );

    private int ticks;

    public CustomPackets() {
        super(Eclipse.NETWORK, "eclipse-custom-packets", "Sends controlled packet pulses for diagnostics and server behavior testing.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        ticks++;
        if (ticks % interval.get() != 0) return;

        for (int i = 0; i < repeats.get(); i++) sendPulse(i);
        if (autoDisable.get()) toggle();
    }

    private void sendPulse(int index) {
        switch (mode.get()) {
            case MovementPulse -> mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
            ));
            case SlotPulse -> mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
            case SprintPulse -> mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            case ElytraPulse -> {
                if (!mc.player.isOnGround() && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                }
            }
        }
    }
}

