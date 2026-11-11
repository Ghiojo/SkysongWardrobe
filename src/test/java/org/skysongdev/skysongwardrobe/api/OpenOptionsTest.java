package org.skysongdev.skysongwardrobe.api;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenOptionsTest {
    @Test
    void defaultsHonourAccessSpeakAndDoNothingOnClose() {
        OpenOptions defaults = OpenOptions.defaults();

        assertEquals(Access.HONOR, defaults.access());
        assertFalse(defaults.isSilent());
        assertTrue(defaults.closeAction().isEmpty());
    }

    @Test
    void eachChangeIsACopy() {
        Consumer<Player> reopen = player -> { };

        OpenOptions changed = OpenOptions.defaults().access(Access.BYPASS).silent().onClose(reopen);

        assertEquals(Access.BYPASS, changed.access());
        assertTrue(changed.isSilent());
        assertSame(reopen, changed.closeAction().orElseThrow());
        assertEquals(Access.HONOR, OpenOptions.defaults().access());
        assertFalse(OpenOptions.defaults().isSilent());
    }

    @Test
    void laterChangesKeepEarlierOnes() {
        Consumer<Player> reopen = player -> { };

        OpenOptions changed = OpenOptions.defaults().onClose(reopen).silent().access(Access.BYPASS);

        assertSame(reopen, changed.closeAction().orElseThrow());
        assertTrue(changed.isSilent());
    }

    @Test
    void nothingIsNull() {
        assertThrows(NullPointerException.class, () -> OpenOptions.defaults().access(null));
        assertThrows(NullPointerException.class, () -> OpenOptions.defaults().onClose(null));
    }

    @Test
    void everyRefusalHasAResultNamedAfterIt() {
        assertEquals(8, OpenResult.values().length);
        assertEquals(OpenResult.UNAVAILABLE, OpenResult.valueOf("UNAVAILABLE"));
    }
}
