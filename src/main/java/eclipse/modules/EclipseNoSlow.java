package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;

public class EclipseNoSlow extends Module {
    public enum Mode {
        Vanilla,
        GrimSlot,
        GrimOffhand
    }

    private static EclipseNoSlow INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgUsing = settings.createGroup("Using Item");
    private final SettingGroup sgSneak = settings.createGroup("Sneak");
    private final SettingGroup sgOther = settings.createGroup("Other");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("NoSlow packet mode.")
        .defaultValue(Mode.GrimOffhand)
        .build()
    );

    private final Setting<Integer> pulseInterval = sgGeneral.add(new IntSetting.Builder()
        .name("pulse-interval")
        .description("Ticks between Grim packet pulses while using an item.")
        .defaultValue(1)
        .range(1, 20)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Double> usingForward = sgUsing.add(new DoubleSetting.Builder()
        .name("forward")
        .description("Forward multiplier while using an item.")
        .defaultValue(1.0)
        .range(0.2, 1.5)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> usingSideways = sgUsing.add(new DoubleSetting.Builder()
        .name("sideways")
        .description("Sideways multiplier while using an item.")
        .defaultValue(1.0)
        .range(0.2, 1.5)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> sneakForward = sgSneak.add(new DoubleSetting.Builder()
        .name("forward")
        .description("Forward multiplier while sneaking.")
        .defaultValue(1.0)
        .range(0.2, 1.5)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> sneakSideways = sgSneak.add(new DoubleSetting.Builder()
        .name("sideways")
        .description("Sideways multiplier while sneaking.")
        .defaultValue(1.0)
        .range(0.2, 1.5)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> otherForward = sgOther.add(new DoubleSetting.Builder()
        .name("forward")
        .description("Forward multiplier for other slow states.")
        .defaultValue(1.0)
        .range(0.2, 1.5)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Double> otherSideways = sgOther.add(new DoubleSetting.Builder()
        .name("sideways")
        .description("Sideways multiplier for other slow states.")
        .defaultValue(1.0)
        .range(0.2, 1.5)
        .sliderRange(0.2, 1.0)
        .decimalPlaces(2)
        .build()
    );

    private int ticks;

    public EclipseNoSlow() {
        super(Eclipse.CATEGORY, "eclipse-no-slow", "Grim-style NoSlow using movement multipliers and slot/offhand packet pulses.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        ticks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.getNetworkHandler() == null || !mc.player.isUsingItem()) return;

        ticks++;
        if (ticks % pulseInterval.get() != 0) return;

        if (mode.get() == Mode.GrimSlot) {
            pulseSlot();
        } else if (mode.get() == Mode.GrimOffhand) {
            if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, 0.0F, 0.0F));
            } else {
                pulseSlot();
            }
        }
    }

    private void pulseSlot() {
        int selected = mc.player.getInventory().getSelectedSlot();
        int spoofed = selected == 8 ? 7 : selected + 1;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(spoofed));
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selected));
    }

    public static Vec2f applyMultiplier(Vec2f movement) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (INSTANCE == null || !INSTANCE.isActive() || client.player == null) return movement;

        ClientPlayerEntity player = client.player;
        float forward;
        float sideways;

        if (player.isSneaking()) {
            forward = INSTANCE.sneakForward.get().floatValue();
            sideways = INSTANCE.sneakSideways.get().floatValue();
        } else if (player.isUsingItem()) {
            forward = INSTANCE.usingForward.get().floatValue();
            sideways = INSTANCE.usingSideways.get().floatValue();
        } else {
            forward = INSTANCE.otherForward.get().floatValue();
            sideways = INSTANCE.otherSideways.get().floatValue();
        }

        if (forward == 0.2F && sideways == 0.2F) return movement;
        return new Vec2f((movement.x / 0.2F) * forward, (movement.y / 0.2F) * sideways);
    }
}
