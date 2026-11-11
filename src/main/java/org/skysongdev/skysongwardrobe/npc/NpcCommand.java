package org.skysongdev.skysongwardrobe.npc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.api.event.OpenSource;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.commands.SubCommand;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.wardrobe.OpenOutcome;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code /sw npc [number]}: opens a Citizens player NPC's wardrobe. Without a number it opens the
 * NPC the player selected, or else the one they are looking at, so owners who can't use
 * {@code /npc sel} only need to look at theirs. {@link NpcOpener} checks who may open which.
 * {@code /sw npc removed} lists removed NPCs' archived wardrobes, for staff only.
 */
public final class NpcCommand implements SubCommand {
    private static final String NUMBER = "number";

    private final NpcDirectory directory;
    private final NpcOpener opener;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final NpcArchive archive;

    public NpcCommand(NpcDirectory directory, NpcOpener opener, Supplier<ItemTagGateway> tags,
                      PluginConfiguration configuration, NpcArchive archive) {
        this.directory = directory;
        this.opener = opener;
        this.tags = tags;
        this.configuration = configuration;
        this.archive = archive;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("npc")
                .requires(source -> source.getSender() instanceof Player player && mayDressNpcs(player))
                .executes(context -> {
                    Player viewer = (Player) context.getSource().getSender();
                    return open(viewer, directory.selected(viewer).or(() -> directory.lookedAt(viewer)));
                })
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("removed")
                        .requires(source -> Permissions.hasAnyOther(source.getSender(),
                                configuration.settings().npcPermissions()))
                        .executes(context -> openRemoved((Player) context.getSource().getSender())))
                .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument(NUMBER, IntegerArgumentType.integer(0))
                        .executes(context -> open((Player) context.getSource().getSender(),
                                directory.byNumber(IntegerArgumentType.getInteger(context, NUMBER)))));
    }

    private boolean mayDressNpcs(Player player) {
        List<String> permissions = configuration.settings().npcPermissions();
        return Permissions.hasAny(player, permissions) || Permissions.hasAnyOther(player, permissions);
    }

    private int open(Player viewer, Optional<NpcInfo> target) {
        OpenOutcome outcome = opener.open(viewer, target, true, OpenSource.COMMAND);
        opener.tell(viewer, outcome);
        return outcome == OpenOutcome.OPENED ? Command.SINGLE_SUCCESS : 0;
    }

    private int openRemoved(Player viewer) {
        Optional<WardrobeLayout> layout = configuration.layout();
        if (layout.isEmpty()) {
            configuration.messages().send(viewer, "wardrobe.closed");
            return 0;
        }
        RemovedNpcsMenu menu = new RemovedNpcsMenu(archive, configuration.galleryMenu(), layout.get(), tags.get(),
                configuration.messages());
        if (menu.isEmpty()) {
            configuration.messages().send(viewer, "npc.removed.none");
            return 0;
        }
        viewer.openInventory(menu.getInventory());
        return Command.SINGLE_SUCCESS;
    }
}
