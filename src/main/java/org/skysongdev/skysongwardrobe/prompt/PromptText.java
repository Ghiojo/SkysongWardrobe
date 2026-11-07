package org.skysongdev.skysongwardrobe.prompt;

import net.kyori.adventure.text.Component;
import org.skysongdev.skysongwardrobe.config.Messages;

import java.util.Map;

/**
 * The four texts of a yes-or-no prompt, read from messages.yml under one key:
 * {@code <key>.title}, {@code <key>.body}, {@code <key>.agree} and {@code <key>.decline}.
 */
public record PromptText(Component title, Component body, Component agree, Component decline) {

    public static PromptText from(Messages messages, String key, Map<String, String> placeholders) {
        return new PromptText(
                messages.get(key + ".title", placeholders),
                messages.get(key + ".body", placeholders),
                messages.get(key + ".agree", placeholders),
                messages.get(key + ".decline", placeholders));
    }
}
