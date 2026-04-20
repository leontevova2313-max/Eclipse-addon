package eclipse.modules.chat.colorchat;

import eclipse.modules.chat.ColorChat;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ChatSendTransformer {
    private static final Pattern SECTION_FORMATTING = Pattern.compile("В§[0-9A-FK-ORa-fk-or]");
    private static final Pattern AMPERSAND_FORMATTING = Pattern.compile("&(?:#[0-9A-Fa-f]{6}|[0-9A-FK-ORa-fk-or])");
    private static final Pattern MINI_MESSAGE_TAG = Pattern.compile("<[^>]{1,64}>");
    private final ChatColorFormatter formatter = new ChatColorFormatter();

    public String transform(String message, ColorChat module) {
        if (message == null || message.isBlank()) return message;
        if (!module.formattingEnabled()) return module.stripFormattingBeforeSend() ? stripFormatting(message) : message;

        TargetSlice slice = targetSlice(message, module);
        if (slice == null || slice.text().isBlank()) return module.stripFormattingBeforeSend() ? stripFormatting(message) : message;

        String baseText = module.stripFormattingBeforeSend() ? stripFormatting(slice.text()) : slice.text();
        String replacement = switch (module.sendMode()) {
            case PREVIEW_ONLY -> baseText;
            case STRIP_TO_PLAIN -> stripFormatting(baseText);
            case SEND_RAW -> formatter.raw(baseText, module, module.compatibilityStrategy());
            case SERVER_COMPAT -> serverCompatible(baseText, module);
        };

        return slice.prefix() + replacement + slice.suffix();
    }

    public String stripFormatting(String input) {
        if (input == null || input.isEmpty()) return input;
        String stripped = SECTION_FORMATTING.matcher(input).replaceAll("");
        stripped = AMPERSAND_FORMATTING.matcher(stripped).replaceAll("");
        return MINI_MESSAGE_TAG.matcher(stripped).replaceAll("");
    }

    private String serverCompatible(String text, ColorChat module) {
        if (module.compatibilityStrategy() == CompatibilityStrategy.AUTO_PLAIN_FALLBACK) return stripFormatting(text);
        return formatter.raw(text, module, module.compatibilityStrategy());
    }

    private TargetSlice targetSlice(String message, ColorChat module) {
        String lower = message.toLowerCase(Locale.ROOT);
        for (String ignored : module.ignoredCommandPrefixes()) {
            if (!ignored.isBlank() && lower.startsWith(ignored.toLowerCase(Locale.ROOT))) return null;
        }

        if (!message.startsWith("/")) {
            return module.applyNormalChat() ? new TargetSlice("", message, "") : null;
        }

        PrivateCommand privateCommand = privateCommand(message);
        if (privateCommand != null) {
            return module.applyPrivateMessages() ? privateCommand.slice() : null;
        }

        if (module.ignoreCommands() && !module.applyCommandArgumentText()) return null;
        if (!module.applyCommandArgumentText()) return null;

        int split = message.indexOf(' ');
        if (split < 0 || split == message.length() - 1) return null;
        return new TargetSlice(message.substring(0, split + 1), message.substring(split + 1), "");
    }

    private PrivateCommand privateCommand(String message) {
        List<String> withTarget = List.of("/msg", "/tell", "/w", "/whisper", "/pm", "/message");
        String lower = message.toLowerCase(Locale.ROOT);
        for (String command : withTarget) {
            if (!lower.startsWith(command + " ")) continue;

            int first = message.indexOf(' ');
            int second = message.indexOf(' ', first + 1);
            if (second < 0 || second == message.length() - 1) return null;
            return new PrivateCommand(new TargetSlice(message.substring(0, second + 1), message.substring(second + 1), ""));
        }

        if (lower.startsWith("/r ")) {
            return message.length() > 3 ? new PrivateCommand(new TargetSlice(message.substring(0, 3), message.substring(3), "")) : null;
        }

        return null;
    }

    private record PrivateCommand(TargetSlice slice) {
    }

    private record TargetSlice(String prefix, String text, String suffix) {
    }
}

