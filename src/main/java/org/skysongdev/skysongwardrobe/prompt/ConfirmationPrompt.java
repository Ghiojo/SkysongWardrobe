package org.skysongdev.skysongwardrobe.prompt;

import org.bukkit.entity.Player;

/** Asks a player to agree or decline. At most one of the two answers ever runs. */
public interface ConfirmationPrompt {
    /**
     * Shows the prompt. Closing it without answering runs neither callback.
     * Both callbacks run on the main thread.
     */
    void ask(Player player, PromptText text, Runnable onAgree, Runnable onDecline);
}
