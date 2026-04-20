package eclipse.modules.chat;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatLinks extends Module {
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b((?:https?://)?(?:[a-z0-9-]+\\.)+[a-z]{2,}(?::\\d+)?(?:/[^\\s]*)?)");
    private static final Pattern CHAT_NAME_PATTERN = Pattern.compile("(?:(?<=<)|(?<=\\[))([A-Za-z0-9_]{3,16})(?=[>\\]])|^([A-Za-z0-9_]{3,16})(?=[:В»])");

    private final SettingGroup sgLinks = settings.createGroup("Links");
    private final SettingGroup sgNames = settings.createGroup("Names");

    private final Setting<Boolean> clickableLinks = sgLinks.add(new BoolSetting.Builder()
        .name("clickable-links")
        .description("Makes plain links in chat clickable.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> underlineLinks = sgLinks.add(new BoolSetting.Builder()
        .name("underline-links")
        .description("Underlines detected links.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> clickNames = sgNames.add(new BoolSetting.Builder()
        .name("click-names")
        .description("Makes chat names clickable for quick private replies.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> replyCommand = sgNames.add(new StringSetting.Builder()
        .name("reply-command")
        .description("Command inserted when clicking a name. Use {name}.")
        .defaultValue("/msg {name} ")
        .placeholder("/msg {name} ")
        .build()
    );

    public ChatLinks() {
        super(Eclipse.CHAT, "chat-links", "Makes chat links and player names clickable.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onMessageReceive(ReceiveMessageEvent event) {
        Text fixed = fixLinks(event.getMessage());
        if (fixed != event.getMessage()) event.setMessage(fixed);
    }

    public static Text fixLinks(Text message) {
        ChatLinks module = Modules.get().get(ChatLinks.class);
        if (module == null || !module.isActive()) return message;
        if (!module.clickableLinks.get() && !module.clickNames.get()) return message;

        MutableText fixed = Text.empty().setStyle(message.getStyle());
        boolean[] changed = {false};

        message.visit((style, value) -> {
            appendFixed(fixed, value, style, module, changed);
            return Optional.empty();
        }, Style.EMPTY);

        return changed[0] ? fixed : message;
    }

    private static void appendFixed(MutableText output, String value, Style style, ChatLinks module, boolean[] changed) {
        if (!module.clickNames.get()) {
            appendLinkified(output, value, style, module.underlineLinks.get(), changed);
            return;
        }

        Matcher matcher = CHAT_NAME_PATTERN.matcher(value);
        int index = 0;

        while (matcher.find()) {
            if (matcher.start() > index) {
                appendLinkified(output, value.substring(index, matcher.start()), style, module.underlineLinks.get(), changed);
            }

            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String command = module.replyCommand.get().replace("{name}", name);
            Style nameStyle = style
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Reply to " + name)))
                .withColor(Formatting.LIGHT_PURPLE)
                .withBold(true);
            output.append(Text.literal(name).setStyle(nameStyle));
            changed[0] = true;
            index = matcher.end();
        }

        if (index < value.length()) {
            appendLinkified(output, value.substring(index), style, module.underlineLinks.get(), changed);
        }
    }

    private static void appendLinkified(MutableText output, String value, Style style, boolean underline, boolean[] changed) {
        Matcher matcher = URL_PATTERN.matcher(value);
        int index = 0;

        while (matcher.find()) {
            if (matcher.start() > index) {
                output.append(Text.literal(value.substring(index, matcher.start())).setStyle(style));
            }

            String rawUrl = trimTrailingPunctuation(matcher.group(1));
            String suffix = matcher.group(1).substring(rawUrl.length());
            URI uri = toUri(rawUrl);

            if (uri == null) {
                output.append(Text.literal(matcher.group(1)).setStyle(style));
            } else {
                Style linkStyle = style
                    .withClickEvent(new ClickEvent.OpenUrl(uri))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal(uri.toString())))
                    .withColor(Formatting.AQUA)
                    .withUnderline(underline);
                output.append(Text.literal(rawUrl).setStyle(linkStyle));
                if (!suffix.isEmpty()) output.append(Text.literal(suffix).setStyle(style));
                changed[0] = true;
            }

            index = matcher.end();
        }

        if (index < value.length()) {
            output.append(Text.literal(value.substring(index)).setStyle(style));
        }
    }

    private static String trimTrailingPunctuation(String value) {
        int end = value.length();
        while (end > 0) {
            char c = value.charAt(end - 1);
            if (c != '.' && c != ',' && c != ';' && c != ')' && c != ']') break;
            end--;
        }

        return value.substring(0, end);
    }

    private static URI toUri(String value) {
        String normalized = value.matches("(?i)^https?://.*") ? value : "https://" + value;
        try {
            return new URI(normalized);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}

