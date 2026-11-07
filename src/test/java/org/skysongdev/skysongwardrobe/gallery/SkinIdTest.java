package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinIdTest {
    @Test
    void typedIdsAreLowercased() {
        assertEquals(Optional.of(new SkinId("red_cape_2")), SkinId.parse("Red_Cape_2"));
    }

    @Test
    void thirtyTwoCharactersIsTheLimit() {
        assertTrue(SkinId.parse("a".repeat(32)).isPresent());
        assertTrue(SkinId.parse("a".repeat(33)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "red-cape", "red cape", "red,cape", "../cape", "cape.png", "capé"})
    void refusesAnythingButLettersDigitsAndUnderscores(String typed) {
        assertTrue(SkinId.parse(typed).isEmpty());
    }

    @Test
    void constructorRefusesInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> new SkinId("Upper"));
        assertThrows(IllegalArgumentException.class, () -> new SkinId(null));
    }
}
