package org.skysongdev.skysongwardrobe.wardrobe;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.skysongdev.skysongwardrobe.config.PluginConfiguration;
import org.skysongdev.skysongwardrobe.config.WardrobeLayout;
import org.skysongdev.skysongwardrobe.items.ItemTagGateway;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Opens a wardrobe menu once the caller found the wearer and checked access: the last checks
 * every way in shares (the wardrobe isn't closed, and nobody else is dressing a wearer only one
 * may dress at a time), and the message for each refusal. Commands, the mannequin click and the
 * public API all open through here, so they all behave the same. Main thread only.
 */
public final class WardrobeOpener {
    private final WardrobeKinds kinds;
    private final Supplier<ItemTagGateway> tags;
    private final PluginConfiguration configuration;
    private final Server server;

    /** @param tags looked up when a menu opens, since SkysongItems may not be ready at startup */
    public WardrobeOpener(WardrobeKinds kinds, Supplier<ItemTagGateway> tags, PluginConfiguration configuration,
                          Server server) {
        this.kinds = kinds;
        this.tags = tags;
        this.configuration = configuration;
        this.server = server;
    }

    /**
     * Shows {@code viewer} the wardrobe of {@code wearer}, a wearer of the given kind.
     *
     * @throws IllegalStateException when nothing registered that kind of wardrobe
     */
    public OpenOutcome open(Player viewer, WardrobeKind kind, UUID wearer) {
        WardrobeKinds.Kind stored = kinds.get(kind)
                .orElseThrow(() -> new IllegalStateException("Nothing stores " + kind + " wardrobes."));
        Optional<WardrobeLayout> layout = configuration.layout();
        if (layout.isEmpty()) {
            return OpenOutcome.WARDROBE_CLOSED;
        }
        if (kind.exclusive() && inUse(kind, wearer)) {
            return OpenOutcome.IN_USE;
        }
        viewer.openInventory(new WardrobeMenu(kind, stored.wardrobes().load(wearer), layout.get(), tags.get())
                .getInventory());
        return OpenOutcome.OPENED;
    }

    /** Tells {@code viewer} why a wardrobe of the given kind did not open; nothing if it did. */
    public void tell(Player viewer, WardrobeKind kind, OpenOutcome outcome) {
        messageKey(kind, outcome).ifPresent(key -> configuration.messages().send(viewer, key));
    }

    /** The messages.yml key explaining the outcome, if it needs explaining. */
    static Optional<String> messageKey(WardrobeKind kind, OpenOutcome outcome) {
        String wearer = switch (kind) {
            case MANNEQUIN -> "mannequin";
            case NPC, REMOVED_NPC -> "npc";
            case PLAYER -> "wardrobe";
        };
        return switch (outcome) {
            case OPENED -> Optional.empty();
            case WARDROBE_CLOSED -> Optional.of("wardrobe.closed");
            case NOT_A_PLAYER_NPC -> Optional.of("npc.not-a-player");
            case NOT_FOUND -> Optional.of(wearer + ".not-found");
            case NO_ACCESS -> Optional.of(wearer + ".no-access");
            case IN_USE -> Optional.of(wearer + ".in-use");
        };
    }

    private boolean inUse(WardrobeKind kind, UUID wearer) {
        return server.getOnlinePlayers().stream()
                .flatMap(online -> WardrobeMenu.of(online.getOpenInventory()).stream())
                .anyMatch(menu -> menu.shows(kind, wearer));
    }
}
