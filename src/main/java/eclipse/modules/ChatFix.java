package eclipse.modules;

import eclipse.Eclipse;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFix extends Module {
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b((?:https?://)?(?:[a-z0-9-]+\\.)+[a-z]{2,}(?::\\d+)?(?:/[^\\s]*)?)");
    private static final Pattern CHAT_NAME_PATTERN = Pattern.compile("(?:(?<=<)|(?<=\\[))([A-Za-z0-9_]{3,16})(?=[>\\]])|^([A-Za-z0-9_]{3,16})(?=[:»])");

    private final SettingGroup sgPrefix = settings.createGroup("Prefix");
    private final SettingGroup sgLinks = settings.createGroup("Links");
    private final SettingGroup sgNames = settings.createGroup("Names");

    private final Setting<Boolean> prefixEnabled = sgPrefix.add(new BoolSetting.Builder()
        .name("prefix")
        .description("Adds Prefix Text before normal chat messages.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> prefix = sgPrefix.add(new StringSetting.Builder()
        .name("prefix")
        .description("Prefix added before normal chat messages.")
        .defaultValue("!\u0438\u0438")
        .placeholder("!\u0438\u0438")
        .build()
    );

    private final Setting<Boolean> ignoreCommands = sgPrefix.add(new BoolSetting.Builder()
        .name("ignore-commands")
        .description("Does not add the prefix to slash commands.")
        .defaultValue(true)
        .build()
    );

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

    private boolean sendingPrefixed;

    public ChatFix() {
        super(Eclipse.CATEGORY, "chat-fix", "Adds chat prefixes, clickable links, and quick private replies.");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (sendingPrefixed || mc.getNetworkHandler() == null) return;
        if (!(event.packet instanceof ChatMessageC2SPacket packet)) return;
        if (!prefixEnabled.get()) return;

        String message = packet.chatMessage();
        String configuredPrefix = prefix.get().trim();
        if (configuredPrefix.isEmpty()) return;
        if (message.isBlank()) return;
        if (ignoreCommands.get() && message.startsWith("/")) return;
        if (message.startsWith(configuredPrefix)) return;

        event.setCancelled(true);
        sendingPrefixed = true;
        mc.getNetworkHandler().sendChatMessage(configuredPrefix + " " + message);
        sendingPrefixed = false;
    }

    public static Text fixLinks(Text message, boolean signed) {
        ChatFix module = Modules.get().get(ChatFix.class);
        if (module == null || !module.isActive()) return message;
        if (!module.clickableLinks.get() && !module.clickNames.get()) return message;
        if (signed) return message;

        boolean[] changed = {false};
        MutableText fixed = copyAndDecorate(message, module, changed);

        return changed[0] ? fixed : message;
    }

    private static MutableText copyAndDecorate(Text text, ChatFix module, boolean[] changed) {
        MutableText copy = copyContent(text, module, changed);
        for (Text sibling : text.getSiblings()) {
            copy.append(copyAndDecorate(sibling, module, changed));
        }

        return copy;
    }

    private static MutableText copyContent(Text text, ChatFix module, boolean[] changed) {
        TextContent content = text.getContent();
        Style style = text.getStyle();

        if (content instanceof PlainTextContent.Literal literal) {
            MutableText output = Text.empty().setStyle(style);
            appendFixed(output, literal.string(), style, module, changed);
            return output;
        }

        return text.copyContentOnly().setStyle(style);
    }

    private static void appendFixed(MutableText output, String value, Style style, ChatFix module, boolean[] changed) {
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
            Style nameStyle = decorateNameStyle(style, name, module);
            output.append(Text.literal(name).setStyle(nameStyle));
            if (nameStyle != style) changed[0] = true;
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
                Style linkStyle = decorateLinkStyle(style, uri, underline);
                output.append(Text.literal(rawUrl).setStyle(linkStyle));
                if (!suffix.isEmpty()) output.append(Text.literal(suffix).setStyle(style));
                if (linkStyle != style) changed[0] = true;
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

    private static Style decorateNameStyle(Style style, String name, ChatFix module) {
        if (style.getClickEvent() != null) return style;

        String command = module.replyCommand.get().replace("{name}", name);
        Style updated = style
            .withClickEvent(new ClickEvent.SuggestCommand(command))
            .withColor(Formatting.LIGHT_PURPLE)
            .withBold(true);

        if (updated.getHoverEvent() == null) {
            updated = updated.withHoverEvent(new HoverEvent.ShowText(Text.literal("Reply to " + name)));
        }

        return updated;
    }

    private static Style decorateLinkStyle(Style style, URI uri, boolean underline) {
        if (style.getClickEvent() != null) return style;

        Style updated = style
            .withClickEvent(new ClickEvent.OpenUrl(uri))
            .withColor(Formatting.AQUA)
            .withUnderline(underline);

        if (updated.getHoverEvent() == null) {
            updated = updated.withHoverEvent(new HoverEvent.ShowText(Text.literal(uri.toString())));
        }

        return updated;
    }
}
