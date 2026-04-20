package eclipse.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

final class EclipseCombatSounds {
    private static final Identifier CRYSTAL_KILL = Identifier.of("eclipse", "crystal_kill");

    private EclipseCombatSounds() {
    }

    static void playCrystalKill() {
        play(CRYSTAL_KILL, 0.9F, 1.0F);
    }

    private static void play(Identifier id, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        client.world.playSound(client.player, client.player.getBlockPos(), SoundEvent.of(id), SoundCategory.PLAYERS, volume, pitch);
    }
}
