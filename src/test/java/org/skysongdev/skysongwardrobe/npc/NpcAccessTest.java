package org.skysongdev.skysongwardrobe.npc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.SkysongWardrobe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcAccessTest {
    private static final List<String> PERMISSIONS = List.of("skysongwardrobe.npc", "skysongeconomy.shopkeeper");

    private ServerMock server;
    private SkysongWardrobe plugin;
    private PlayerMock owner;
    private PlayerMock stranger;
    private NpcInfo owned;
    private NpcInfo serverOwned;
    private final NpcAccess access = NpcAccess.standard(() -> PERMISSIONS);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SkysongWardrobe.class);
        owner = server.addPlayer();
        stranger = server.addPlayer();
        owned = new NpcInfo(UUID.randomUUID(), 1, "Baker", Optional.of(owner.getUniqueId()));
        serverOwned = new NpcInfo(UUID.randomUUID(), 2, "Guard", Optional.empty());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void anOwnerWithAnyNpcPermissionOpensTheirOwnNpc() {
        owner.addAttachment(plugin, "skysongeconomy.shopkeeper", true);

        assertTrue(access.canOpen(owner, owned));
        assertFalse(access.canOpen(owner, serverOwned));
    }

    @Test
    void anOwnerWithoutPermissionIsTurnedAway() {
        assertFalse(access.canOpen(owner, owned));
    }

    @Test
    void someoneElseWithThePermissionIsTurnedAway() {
        stranger.addAttachment(plugin, "skysongwardrobe.npc", true);

        assertFalse(access.canOpen(stranger, owned));
    }

    @Test
    void staffWithAnOtherPermissionOpenEveryNpc() {
        stranger.addAttachment(plugin, "skysongwardrobe.npc.other", true);

        assertTrue(access.canOpen(stranger, owned));
        assertTrue(access.canOpen(stranger, serverOwned));
    }

    @Test
    void aRegisteredRuleAddsAnotherWayIn() {
        access.register((viewer, npc) -> npc.name().equals("Guard") && viewer == stranger);

        assertTrue(access.canOpen(stranger, serverOwned));
        assertFalse(access.canOpen(stranger, owned));
    }

    @Test
    void noRulesLetNobodyIn() {
        stranger.setOp(true);

        assertFalse(new NpcAccess().canOpen(stranger, serverOwned));
    }
}
