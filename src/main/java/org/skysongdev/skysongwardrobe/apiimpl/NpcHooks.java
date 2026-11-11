package org.skysongdev.skysongwardrobe.apiimpl;

import org.skysongdev.skysongwardrobe.npc.NpcAccess;
import org.skysongdev.skysongwardrobe.npc.NpcDirectory;
import org.skysongdev.skysongwardrobe.npc.NpcOpener;

/** What the API needs from the Citizens hook, which exists only when Citizens is enabled. */
public record NpcHooks(NpcDirectory directory, NpcOpener opener, NpcAccess access) {
}
