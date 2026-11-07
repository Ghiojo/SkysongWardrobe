package org.skysongdev.skysongwardrobe.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {
    private final List<String> logged = new ArrayList<>();
    private final Logger logger = Logger.getAnonymousLogger();

    MessagesTest() {
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text);
        return config;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void rendersMiniMessageWithThePrefixAndPlaceholders() throws InvalidConfigurationException {
        Messages messages = new Messages(yaml("""
                prefix: "<gold>[W]</gold>"
                saved: "<prefix> <green>Saved <id>."
                """), new YamlConfiguration(), logger);

        assertEquals("[W] Saved red_scarf.", plain(messages.get("saved", Map.of("id", "red_scarf"))));
    }

    @Test
    void placeholderValuesAreNotParsedAsMiniMessage() throws InvalidConfigurationException {
        Messages messages = new Messages(yaml("named: \"Hi <name>\""), new YamlConfiguration(), logger);

        assertEquals("Hi <red>Bob", plain(messages.get("named", Map.of("name", "<red>Bob"))));
    }

    @Test
    void missingKeyFallsBackToTheBundledCopy() throws InvalidConfigurationException {
        Messages messages = new Messages(new YamlConfiguration(), yaml("only-bundled: \"From the jar\""), logger);

        assertEquals("From the jar", plain(messages.get("only-bundled")));
        assertTrue(logged.isEmpty());
    }

    @Test
    void keyMissingEverywhereShowsTheKeyAndLogsIt() {
        Messages messages = new Messages(new YamlConfiguration(), new YamlConfiguration(), logger);

        assertEquals("not.there", plain(messages.get("not.there")));
        assertTrue(logged.contains("messages.yml has no text for not.there."));
    }
}
