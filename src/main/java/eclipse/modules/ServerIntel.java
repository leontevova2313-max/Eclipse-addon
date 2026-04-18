package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class ServerIntel extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> newChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("new-chunks")
        .description("Reports first-time chunk data packets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> soundLocator = sgGeneral.add(new BoolSetting.Builder()
        .name("sound-locator")
        .description("Reports server sound packet positions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> coordLogger = sgGeneral.add(new BoolSetting.Builder()
        .name("coord-logger")
        .description("Logs coordinate-like chat messages and deaths.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> oreUpdates = sgGeneral.add(new BoolSetting.Builder()
        .name("ore-updates")
        .description("Reports ore blocks seen in block update packets.")
        .defaultValue(true)
        .build()
    );

    private final Set<Long> seenChunks = new HashSet<>();

    public ServerIntel() {
        super(Eclipse.CATEGORY, "eclipse-server-intel", "NewChunks, SoundLocator, CoordLogger, and ore update logging in one lightweight module.");
    }

    @Override
    public void onActivate() {
        seenChunks.clear();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (newChunks.get() && event.packet instanceof ChunkDataS2CPacket packet) {
            long key = (((long) packet.getChunkX()) << 32) ^ (packet.getChunkZ() & 0xffffffffL);
            if (seenChunks.add(key)) info("Chunk %d, %d loaded for the first time this session.", packet.getChunkX(), packet.getChunkZ());
        } else if (soundLocator.get() && event.packet instanceof PlaySoundS2CPacket packet) {
            info("Sound %s at %.1f %.1f %.1f", packet.getSound().getIdAsString(), packet.getX(), packet.getY(), packet.getZ());
        } else if (coordLogger.get() && event.packet instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString();
            if (looksUsefulChat(text)) info("Chat intel: %s", text);
        } else if (oreUpdates.get() && event.packet instanceof BlockUpdateS2CPacket packet) {
            reportOre(packet.getPos(), packet.getState().getBlock());
        } else if (oreUpdates.get() && event.packet instanceof ChunkDeltaUpdateS2CPacket packet) {
            packet.visitUpdates((pos, state) -> reportOre(pos, state.getBlock()));
        }
    }

    private boolean looksUsefulChat(String text) {
        String lower = text.toLowerCase();
        return lower.matches(".*-?\\d+[, ]+-?\\d+([, ]+-?\\d+)?.*")
            || lower.contains("died")
            || lower.contains("slain")
            || lower.contains("joined")
            || lower.contains("left");
    }

    private void reportOre(BlockPos pos, Block block) {
        if (!isOre(block)) return;
        info("Ore update: %s at %d %d %d", block.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
    }

    private boolean isOre(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
            || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
            || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
            || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
            || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
            || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
            || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
            || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
            || block == Blocks.ANCIENT_DEBRIS;
    }
}
