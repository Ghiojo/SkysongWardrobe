package org.skysongdev.skysongwardrobe.gallery;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** {@link NameLookup} that asks Mojang's session server. Any failure counts as "not found". */
public final class MojangNameLookup implements NameLookup {
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final int OK = 200;

    private final HttpClient client;
    private final Duration timeout;

    public MojangNameLookup(Duration timeout) {
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.timeout = timeout;
    }

    @Override
    public Optional<String> currentName(UUID player) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(PROFILE_URL + player.toString().replace("-", "")))
                .timeout(timeout)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == OK ? parseName(response.body()) : Optional.empty();
        } catch (IOException failed) {
            return Optional.empty();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** The {@code name} field of a session server profile, empty if the body has none. */
    static Optional<String> parseName(String body) {
        try {
            JsonElement profile = JsonParser.parseString(body);
            if (!profile.isJsonObject() || !profile.getAsJsonObject().has("name")) {
                return Optional.empty();
            }
            return Optional.of(profile.getAsJsonObject().get("name").getAsString()).filter(name -> !name.isBlank());
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException notAProfile) {
            return Optional.empty();
        }
    }
}
