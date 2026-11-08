package org.skysongdev.skysongwardrobe.npc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.commands.Permissions;
import org.skysongdev.skysongwardrobe.commands.SubCommand;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeKind;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeMenu;
import org.skysongdev.skysongwardrobe.wardrobe.WardrobeRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code /sw npc [number]}: opens a Citizens player NPC's wardrobe. Without a number it opens the
 * NPC the player selected, or else the one they are looking at, so owners who can't use
 * {@code /npc sel} only need to look at theirs. {@link NpcAccess} decides who may open which.
 */
public final class NpcCommand implements SubCommand {
    private static final String NUMBER = "number";

    private final NpcDirectory directory;
    private final NpcSkins skins;
    private final NpcAccess access;
    private final WardrobeRepository wardrobes;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final Server server;

    public NpcCommand(NpcDirectory directory, NpcSkins skins, NpcAccess access, WardrobeRepository wardrobes,
                      Supplier<ItemTagGateway> tags, PluginConfiguration configuration, Server server) {
        this.directory = directory;
        this.skins = skins;
        this.access = access;
        this.wardrobes = wardrobes;
        this.tags = tags;
        this.configuration = configuration;
        this.server = server;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("npc")
                .requires(source -> source.getSender() instanceof Player player && mayDressNpcs(player))
                .executes(context -> {
                    Player viewer = (Player) context.getSource().getSender();
                    return open(viewer, directory.selected(viewer).or(() -> directory.lookedAt(viewer)));
                })
                .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument(NUMBER, IntegerArgumentType.integer(0))
                        .executes(context -> open((Player) context.getSource().getSender(),
                                directory.byNumber(IntegerArgumentType.getInteger(context, NUMBER)))));
    }

    private boolean mayDressNpcs(Player player) {
        List<String> permissions = configuration.settings().npcPermissions();
        return Permissions.hasAny(player, permissions) || Permissions.hasAnyOther(player, permissions);
    }

    private int open(Player viewer, Optional<NpcInfo> target) {
        Optional<WardrobeLayout> layout = configuration.layout();
        Optional<String> refusal = target.isEmpty() ? Optional.of("npc.not-found")
                : !skins.isDressable(target.get().id()) ? Optional.of("npc.not-a-player")
                : !access.canOpen(viewer, target.get()) ? Optional.of("npc.no-access")
                : layout.isEmpty() ? Optional.of("wardrobe.closed")
                : inUse(target.get()) ? Optional.of("npc.in-use")
                : Optional.empty();
        if (refusal.isPresent()) {
            configuration.messages().send(viewer, refusal.get());
            return 0;
        }
        viewer.openInventory(new WardrobeMenu(WardrobeKind.NPC, wardrobes.load(target.get().id()), layout.get(),
                tags.get()).getInventory());
        return Command.SINGLE_SUCCESS;
    }

    private boolean inUse(NpcInfo npc) {
        return server.getOnlinePlayers().stream()
                .flatMap(online -> WardrobeMenu.of(online.getOpenInventory()).stream())
                .anyMatch(menu -> menu.shows(WardrobeKind.NPC, npc.id()));
    }
}
