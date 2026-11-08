package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.npc.NpcSkins;
import org.skysongdev.skysongwardrobe.render.PlayerSkins;
import org.skysongdev.skysongwardrobe.render.SignedTexture;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * NPC skins through Citizens' {@link SkinTrait}. A spawned NPC's skin is read off its player
 * profile, which also holds skins Citizens fetched by name; a despawned one's off the trait.
 */
final class CitizensNpcSkins implements NpcSkins {
    /** Citizens caches skins by this name, so each texture gets its own. */
    private static final String SKIN_NAME_PREFIX = "skysongwardrobe-";

    private final Supplier<NPCRegistry> registry;

    CitizensNpcSkins(Supplier<NPCRegistry> registry) {
        this.registry = registry;
    }

    @Override
    public boolean isDressable(UUID id) {
        return npc(id).map(npc -> npc.getOrAddTrait(MobType.class).getType() == EntityType.PLAYER).orElse(false);
    }

    @Override
    public Optional<SignedTexture> current(UUID id) {
        return npc(id).flatMap(npc -> npc.isSpawned() && npc.getEntity() instanceof Player body
                ? PlayerSkins.current(body).or(() -> fromTrait(npc))
                : fromTrait(npc));
    }

    @Override
    public void apply(UUID id, SignedTexture texture) {
        npc(id).ifPresent(npc -> npc.getOrAddTrait(SkinTrait.class)
                .setSkinPersistent(skinName(texture), texture.signature(), texture.value()));
    }

    static String skinName(SignedTexture texture) {
        return SKIN_NAME_PREFIX + Integer.toHexString(texture.value().hashCode());
    }

    private Optional<NPC> npc(UUID id) {
        return Optional.ofNullable(registry.get().getByUniqueId(id));
    }

    private static Optional<SignedTexture> fromTrait(NPC npc) {
        SkinTrait skin = npc.getTraitNullable(SkinTrait.class);
        if (skin == null || skin.getTexture() == null || skin.getSignature() == null) {
            return Optional.empty();
        }
        return Optional.of(new SignedTexture(skin.getTexture(), skin.getSignature()));
    }
}
