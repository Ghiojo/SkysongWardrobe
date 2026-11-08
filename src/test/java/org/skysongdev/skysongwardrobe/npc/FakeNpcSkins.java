package org.skysongdev.skysongwardrobe.npc;

import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** NPC skins kept in maps, recording every apply. */
final class FakeNpcSkins implements NpcSkins {
    final Set<UUID> dressable = new HashSet<>();
    final Map<UUID, SignedTexture> skins = new HashMap<>();
    final List<UUID> applied = new ArrayList<>();

    @Override
    public boolean isDressable(UUID npc) {
        return dressable.contains(npc);
    }

    @Override
    public Optional<SignedTexture> current(UUID npc) {
        return Optional.ofNullable(skins.get(npc));
    }

    @Override
    public void apply(UUID npc, SignedTexture texture) {
        skins.put(npc, texture);
        applied.add(npc);
    }
}
