package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.skysongdev.skysongwardrobe.render.PlayerSkins;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitizensNpcSkinsTest {
    private static final UUID ID = UUID.randomUUID();
    private static final SignedTexture TRAIT = new SignedTexture("trait-value", "trait-sig");
    private static final SignedTexture PROFILE = new SignedTexture("profile-value", "profile-sig");

    private ServerMock server;
    private final NPCRegistry registry = mock(NPCRegistry.class);
    private final NPC npc = mock(NPC.class);
    private final MobType type = mock(MobType.class);
    private final SkinTrait skin = mock(SkinTrait.class);
    private final CitizensNpcSkins skins = new CitizensNpcSkins(() -> registry);

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        when(registry.getByUniqueId(ID)).thenReturn(npc);
        when(npc.getOrAddTrait(MobType.class)).thenReturn(type);
        when(npc.getOrAddTrait(SkinTrait.class)).thenReturn(skin);
        when(npc.getTraitNullable(SkinTrait.class)).thenReturn(skin);
        when(type.getType()).thenReturn(EntityType.PLAYER);
        when(skin.getTexture()).thenReturn(TRAIT.value());
        when(skin.getSignature()).thenReturn(TRAIT.signature());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onlyExistingPlayerNpcsCanBeDressed() {
        assertTrue(skins.isDressable(ID));
        assertFalse(skins.isDressable(UUID.randomUUID()));

        when(type.getType()).thenReturn(EntityType.ZOMBIE);

        assertFalse(skins.isDressable(ID));
    }

    @Test
    void aDespawnedNpcsSkinComesFromItsTrait() {
        assertEquals(Optional.of(TRAIT), skins.current(ID));
    }

    @Test
    void aSpawnedNpcsSkinComesFromItsProfile() {
        PlayerMock body = server.addPlayer();
        PlayerSkins.apply(body, PROFILE);
        when(npc.isSpawned()).thenReturn(true);
        when(npc.getEntity()).thenReturn(body);

        assertEquals(Optional.of(PROFILE), skins.current(ID));
    }

    @Test
    void anNpcWithoutATextureHasNoSkin() {
        when(skin.getTexture()).thenReturn(null);

        assertEquals(Optional.empty(), skins.current(ID));
        assertEquals(Optional.empty(), skins.current(UUID.randomUUID()));
    }

    @Test
    void applyingSetsTheSkinForGoodUnderItsOwnCacheName() {
        skins.apply(ID, PROFILE);

        verify(skin).setSkinPersistent(CitizensNpcSkins.skinName(PROFILE), PROFILE.signature(), PROFILE.value());
        assertTrue(CitizensNpcSkins.skinName(PROFILE).startsWith("skysongwardrobe-"));
    }
}
