package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.CitizensAPI;
import org.skysongdev.skysongwardrobe.npc.NpcDirectory;
import org.skysongdev.skysongwardrobe.npc.NpcSkins;

/**
 * The only way into the Citizens adapter. Nothing here loads until Citizens is enabled, so the
 * plugin runs without it.
 */
public final class CitizensHook {
    private CitizensHook() {
    }

    /** NPC skins on Citizens' main registry. */
    public static NpcSkins skins() {
        return new CitizensNpcSkins(CitizensAPI::getNPCRegistry);
    }

    /** NPC lookups on Citizens' main registry and default selection. */
    public static NpcDirectory directory() {
        return new CitizensNpcDirectory(CitizensAPI::getNPCRegistry, CitizensAPI::getDefaultNPCSelector);
    }
}
