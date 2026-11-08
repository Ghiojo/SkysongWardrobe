package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.npc.NPCSelector;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitizensNpcDirectoryTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();

    private final NPCRegistry registry = mock(NPCRegistry.class);
    private final NPCSelector selector = mock(NPCSelector.class);
    private final NPC npc = mock(NPC.class);
    private final Owner owner = mock(Owner.class);
    private final Player player = mock(Player.class);
    private final CitizensNpcDirectory directory = new CitizensNpcDirectory(() -> registry, () -> selector);
    private final NpcInfo info = new NpcInfo(ID, 12, "Baker", Optional.of(OWNER));

    @BeforeEach
    void setUp() {
        when(npc.getUniqueId()).thenReturn(ID);
        when(npc.getId()).thenReturn(12);
        when(npc.getName()).thenReturn("Baker");
        when(npc.getTraitNullable(Owner.class)).thenReturn(owner);
        when(owner.getOwnerId()).thenReturn(OWNER);
    }

    @Test
    void findsAnNpcByItsNumber() {
        when(registry.getById(12)).thenReturn(npc);

        assertEquals(Optional.of(info), directory.byNumber(12));
        assertEquals(Optional.empty(), directory.byNumber(13));
    }

    @Test
    void findsTheSelectedNpc() {
        when(selector.getSelected(player)).thenReturn(npc);

        assertEquals(Optional.of(info), directory.selected(player));
    }

    @Test
    void findsTheNpcLookedAtWithinReach() {
        Entity body = mock(Entity.class);
        when(player.getTargetEntity(CitizensNpcDirectory.REACH)).thenReturn(body);
        when(registry.getNPC(body)).thenReturn(npc);

        assertEquals(Optional.of(info), directory.lookedAt(player));
    }

    @Test
    void lookingAtNothingFindsNothing() {
        assertEquals(Optional.empty(), directory.lookedAt(player));
    }

    @Test
    void aServerOwnedNpcHasNoOwner() {
        when(owner.getOwnerId()).thenReturn(null);
        when(registry.getById(12)).thenReturn(npc);

        assertEquals(Optional.empty(), directory.byNumber(12).orElseThrow().owner());
    }
}
