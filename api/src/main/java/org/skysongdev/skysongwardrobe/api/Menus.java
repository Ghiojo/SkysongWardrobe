package org.skysongdev.skysongwardrobe.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Opens wardrobe menus for players: their own, another player's, a Citizens NPC's or a
 * mannequin's. This is what a button in another plugin's menu calls:
 *
 * <pre>{@code
 * OpenResult result = SkysongWardrobeApi.get().menus().openNpcWardrobe(player, 12,
 *         OpenOptions.defaults().onClose(myMenu::open));
 * }</pre>
 *
 * <p>Each method returns at once with an {@link OpenResult}. Unless the options are
 * {@link OpenOptions#silent() silent}, the viewer is told why a wardrobe did not open, in
 * the words of SkysongWardrobe's messages.yml. Main thread only.
 */
@ApiStatus.NonExtendable
public interface Menus {

    /** Opens the wardrobe of the player {@code owner}, online or not, for {@code viewer}. */
    @NotNull OpenResult openWardrobe(@NotNull Player viewer, @NotNull UUID owner, @NotNull OpenOptions options);

    /** Opens the wardrobe of the Citizens NPC with this number (as in {@code /npc sel 12}) for {@code viewer}. */
    @NotNull OpenResult openNpcWardrobe(@NotNull Player viewer, int npcNumber, @NotNull OpenOptions options);

    /** Opens the wardrobe of the Citizens NPC with this UUID for {@code viewer}. */
    @NotNull OpenResult openNpcWardrobe(@NotNull Player viewer, @NotNull UUID npcId, @NotNull OpenOptions options);

    /**
     * Opens the wardrobe of the placed mannequin whose entity has this UUID, for {@code viewer}.
     * The mannequin must be in a loaded chunk.
     */
    @NotNull OpenResult openMannequinWardrobe(@NotNull Player viewer, @NotNull UUID mannequin,
                                              @NotNull OpenOptions options);
}
