package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojangNameLookupTest {
    @Test
    void readsTheNameFromAProfile() {
        String body = """
                {"id": "0f4e3a2c5b6d4e7f8a9b1c2d3e4f5a6b", "name": "Alice", "properties": []}
                """;

        assertEquals(Optional.of("Alice"), MojangNameLookup.parseName(body));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not json", "[]", "{}", "{\"name\": \"\"}", "{\"name\": {}}"})
    void anythingElseIsNotFound(String body) {
        assertTrue(MojangNameLookup.parseName(body).isEmpty());
    }
}
