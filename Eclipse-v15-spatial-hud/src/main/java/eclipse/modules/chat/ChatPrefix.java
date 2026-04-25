package eclipse.modules.chat;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

public class ChatPrefix extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> prefix = sgGeneral.add(new StringSetting.Builder()
        .name("prefix")
        .description("Text added before normal chat messages.")
        .defaultValue("!\u0438\u0438")
        .placeholder("!\u0438\u0438")
        .build()
    );

    private final Setting<Boolean> ignoreCommands = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-commands")
        .description("Skips slash commands.")
        .defaultValue(true)
        .build()
    );

    public ChatPrefix() {
        super(Eclipse.CHAT, "chat-prefix", "Adds a prefix to normal outgoing chat messages.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;
        String configuredPrefix = prefix.get().trim();
        if (configuredPrefix.isEmpty()) return;
        if (message.isBlank()) return;
        if (ignoreCommands.get() && message.startsWith("/")) return;
        if (message.startsWith(configuredPrefix)) return;

        event.message = configuredPrefix + " " + message;
    }
}
