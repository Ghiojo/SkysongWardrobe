package org.skysongdev.skysongwardrobe.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The player-facing texts from messages.yml, in MiniMessage. A key the file lacks falls back
 * to the copy bundled in the jar, so an old messages.yml keeps working after an update.
 * Every text may use {@code <prefix>}, which is replaced by the {@code prefix} entry.
 * {@link #send} puts a text in chat or above the hotbar, as the {@code hotbar} list says.
 */
public final class Messages {
    private static final String PREFIX_KEY = "prefix";

    private final ConfigurationSection file;
    private final ConfigurationSection bundled;
    private final Logger logger;
    private final HotbarKeys hotbar;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public Messages(ConfigurationSection file, ConfigurationSection bundled, Logger logger) {
        this.file = file;
        this.bundled = bundled;
        this.logger = logger;
        this.hotbar = HotbarKeys.from(file, bundled);
    }

    /**
     * Sends the text at {@code key} to {@code audience}: above the hotbar when the {@code hotbar}
     * list covers the key and the audience is a player, otherwise in chat, since the console has
     * no hotbar.
     */
    public void send(Audience audience, String key, Map<String, String> placeholders) {
        Component text = get(key, placeholders);
        if (audience instanceof Player && hotbar.contains(key)) {
            audience.sendActionBar(text);
        } else {
            audience.sendMessage(text);
        }
    }

    /** {@link #send(Audience, String, Map)} with no placeholders. */
    public void send(Audience audience, String key) {
        send(audience, key, Map.of());
    }

    /** The text at {@code key} with its {@code <name>} placeholders filled in as plain, unstyled text. */
    public Component get(String key, Map<String, String> placeholders) {
        // The prefix is looked up only when a text uses it, so a file without one stays quiet.
        TagResolver.Builder resolvers = TagResolver.builder()
                .tag(PREFIX_KEY, (arguments, context) -> Tag.selfClosingInserting(miniMessage.deserialize(raw(PREFIX_KEY))));
        placeholders.forEach((name, value) -> resolvers.resolver(Placeholder.unparsed(name, value)));
        return miniMessage.deserialize(raw(key), resolvers.build());
    }

    /** The text at {@code key}, with no placeholders. */
    public Component get(String key) {
        return get(key, Map.of());
    }

    private String raw(String key) {
        String text = file.getString(key);
        if (text == null) {
            text = bundled.getString(key);
        }
        if (text == null) {
            logger.warning("messages.yml has no text for " + key + ".");
            return key;
        }
        return text;
    }
}
