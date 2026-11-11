package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.npc.NpcAccess;
import org.skysongdev.skysongwardrobe.npc.NpcDirectory;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;
import org.skysongdev.skysongwardrobe.npc.NpcOpener;
import org.skysongdev.skysongwardrobe.npc.NpcSkins;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** NPC hooks over a fixed list of player NPCs, standing in for Citizens. */
final class NpcFixture {
    private NpcFixture() {
    }

    static NpcHooks hooks(WardrobeOpener opener, Supplier<List<String>> permissions, NpcInfo... npcs) {
        List<NpcInfo> all = new ArrayList<>(List.of(npcs));
        NpcDirectory directory = new NpcDirectory() {
            @Override
            public Optional<NpcInfo> byNumber(int number) {
                return all.stream().filter(npc -> npc.number() == number).findFirst();
            }

            @Override
            public Optional<NpcInfo> byId(UUID id) {
                return all.stream().filter(npc -> npc.id().equals(id)).findFirst();
            }

            @Override
            public Optional<NpcInfo> selected(CommandSender sender) {
                return Optional.empty();
            }

            @Override
            public Optional<NpcInfo> lookedAt(Player player) {
                return Optional.empty();
            }
        };
        NpcSkins skins = mock(NpcSkins.class);
        when(skins.isDressable(any())).thenReturn(true);
        NpcAccess access = NpcAccess.standard(permissions);
        return new NpcHooks(directory, new NpcOpener(skins, access, opener), access);
    }
}
