package org.skysongdev.skysongwardrobe.commands;

import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionsTest {
    private final Permissible sender = mock(Permissible.class);
    private final List<String> list = List.of("skysongwardrobe.gallery", "skysongstats.vip");

    @Test
    void anyOneEntryIsEnough() {
        when(sender.hasPermission("skysongstats.vip")).thenReturn(true);

        assertTrue(Permissions.hasAny(sender, list));
        assertFalse(Permissions.hasAnyOther(sender, list));
    }

    @Test
    void otherIsTheBasePlusOther() {
        when(sender.hasPermission("skysongwardrobe.gallery.other")).thenReturn(true);

        assertEquals("skysongwardrobe.gallery.other", Permissions.other("skysongwardrobe.gallery"));
        assertTrue(Permissions.hasAnyOther(sender, list));
        assertFalse(Permissions.hasAny(sender, list));
    }

    @Test
    void emptyListGrantsNothing() {
        assertFalse(Permissions.hasAny(sender, List.of()));
    }
}
