package org.skysongdev.skysongwardrobe.apiimpl;

import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.api.Access;
import org.skysongdev.skysongwardrobe.api.Menus;
import org.skysongdev.skysongwardrobe.api.OpenOptions;
import org.skysongdev.skysongwardrobe.api.OpenResult;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.commands.WardrobeCommand;
import org.skysongdev.skysongwardrobe.mannequin.MannequinOpener;
import org.skysongdev.skysongwardrobe.npc.NpcInfo;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeOpener;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/** {@link Menus} on the same openers the commands and the mannequin click use. */
final class MenusImpl implements Menus {
    private final MainThread mainThread;
    private final WardrobeOpener opener;
    private final MannequinOpener mannequins;
    private final Supplier<Optional<NpcHooks>> npcs;
    private final Server server;

    MenusImpl(MainThread mainThread, WardrobeOpener opener, MannequinOpener mannequins,
              Supplier<Optional<NpcHooks>> npcs, Server server) {
        this.mainThread = mainThread;
        this.opener = opener;
        this.mannequins = mannequins;
        this.npcs = npcs;
        this.server = server;
    }

    @Override
    public OpenResult openWardrobe(Player viewer, UUID owner, OpenOptions options) {
        check(viewer, options);
        Objects.requireNonNull(owner, "owner");
        OpenOutcome outcome = options.access() == Access.HONOR && !mayOpenPlayer(viewer, owner)
                ? OpenOutcome.NO_ACCESS
                : opener.open(viewer, WardrobeKind.PLAYER, owner);
        return finish(viewer, WardrobeKind.PLAYER, outcome, options);
    }

    @Override
    public OpenResult openNpcWardrobe(Player viewer, int npcNumber, OpenOptions options) {
        return openNpc(viewer, options, hooks -> hooks.directory().byNumber(npcNumber));
    }

    @Override
    public OpenResult openNpcWardrobe(Player viewer, UUID npcId, OpenOptions options) {
        Objects.requireNonNull(npcId, "npcId");
        return openNpc(viewer, options, hooks -> hooks.directory().byId(npcId));
    }

    @Override
    public OpenResult openMannequinWardrobe(Player viewer, UUID mannequin, OpenOptions options) {
        check(viewer, options);
        Entity body = server.getEntity(Objects.requireNonNull(mannequin, "mannequin"));
        OpenOutcome outcome = body == null ? OpenOutcome.NOT_FOUND
                : mannequins.open(viewer, body, options.access() == Access.HONOR);
        return finish(viewer, WardrobeKind.MANNEQUIN, outcome, options);
    }

    private OpenResult openNpc(Player viewer, OpenOptions options, Function<NpcHooks, Optional<NpcInfo>> find) {
        check(viewer, options);
        Optional<NpcHooks> hooks = npcs.get();
        OpenOutcome outcome = hooks.isEmpty() ? OpenOutcome.UNAVAILABLE
                : hooks.get().opener().open(viewer, find.apply(hooks.get()), options.access() == Access.HONOR);
        return finish(viewer, WardrobeKind.NPC, outcome, options);
    }

    /** The /wardrobe rules: your own with the wardrobe permission, anyone's with its .other. */
    private static boolean mayOpenPlayer(Player viewer, UUID owner) {
        String permission = viewer.getUniqueId().equals(owner) ? WardrobeCommand.PERMISSION
                : Permissions.other(WardrobeCommand.PERMISSION);
        return viewer.hasPermission(permission);
    }

    private void check(Player viewer, OpenOptions options) {
        mainThread.check();
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(options, "options");
    }

    private OpenResult finish(Player viewer, WardrobeKind kind, OpenOutcome outcome, OpenOptions options) {
        if (outcome == OpenOutcome.OPENED) {
            options.closeAction().ifPresent(action ->
                    WardrobeMenu.of(viewer.getOpenInventory()).ifPresent(menu -> menu.onClose(action)));
        } else if (!options.isSilent()) {
            opener.tell(viewer, kind, outcome);
        }
        return OpenResult.valueOf(outcome.name());
    }
}
