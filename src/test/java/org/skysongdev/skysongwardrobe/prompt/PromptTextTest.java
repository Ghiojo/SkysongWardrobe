package org.skysongdev.skysongwardrobe.prompt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.config.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTextTest {
    @Test
    void readsTheFourTextsWithPlaceholders() throws InvalidConfigurationException {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("""
                ask:
                  title: "Upload <id>?"
                  body: "From <link>"
                  agree: "Yes"
                  decline: "No"
                """);
        Messages messages = new Messages(file, new YamlConfiguration(), Logger.getAnonymousLogger());

        PromptText text = PromptText.from(messages, "ask", Map.of("id", "cape", "link", "https://x/<b>"));

        assertEquals("Upload cape?", plain(text.title()));
        assertEquals("From https://x/<b>", plain(text.body()));
        assertEquals("Yes", plain(text.agree()));
        assertEquals("No", plain(text.decline()));
    }

    @Test
    void bundledUploadPromptUsesEveryPlaceholder() throws IOException {
        YamlConfiguration bundled;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
            bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        Messages messages = new Messages(bundled, bundled, Logger.getAnonymousLogger());

        PromptText text = PromptText.from(messages, "upload.prompt",
                Map.of("id", "red_cape", "name", "Red Cape", "link", "https://example.com/cape.png"));

        String body = plain(text.body());
        assertTrue(body.contains("red_cape") && body.contains("Red Cape") && body.contains("https://example.com/cape.png"), body);
        assertEquals("Agree", plain(text.agree()));
        assertEquals("Decline", plain(text.decline()));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
