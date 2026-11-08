package org.skysongdev.skysongwardrobe.citizens;

import net.citizensnpcs.api.CitizensAPI;
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
}
