package org.skysongdev.skysongwardrobe.gallery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinReferenceTest {
    private static final UUID OWNER = UUID.fromString("0f4e3a2c-5b6d-4e7f-8a9b-1c2d3e4f5a6b");

    @Test
    void storesAsUuidColonId() {
        SkinReference reference = new SkinReference(OWNER, new SkinId("cape"));

        assertEquals("0f4e3a2c-5b6d-4e7f-8a9b-1c2d3e4f5a6b:cape", reference.stored());
        assertEquals(Optional.of(reference), SkinReference.parse(reference.stored()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cape", "Steve:cape", "0f4e3a2c-5b6d-4e7f-8a9b-1c2d3e4f5a6b:", "0f4e3a2c-5b6d-4e7f-8a9b-1c2d3e4f5a6b:bad-id"})
    void refusesValuesThatAreNotAReference(String stored) {
        assertTrue(SkinReference.parse(stored).isEmpty());
    }

    @Test
    void refusesNull() {
        assertTrue(SkinReference.parse(null).isEmpty());
    }
}
