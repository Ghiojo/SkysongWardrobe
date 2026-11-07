package org.skysongdev.skysongwardrobe.prompt;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@link ConfirmationPrompt} shown as a Paper dialog (a confirmation screen, Minecraft 1.21.6+).
 * Only glue to Paper's dialog API: the once-only logic is in {@link PromptAnswer}.
 */
public final class DialogConfirmationPrompt implements ConfirmationPrompt {
    private static final int BUTTON_WIDTH = 150;

    @Override
    public void ask(Player player, PromptText text, Runnable onAgree, Runnable onDecline) {
        PromptAnswer answer = new PromptAnswer(onAgree, onDecline);
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).build();
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(text.title())
                        .canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(text.body())))
                        .build())
                .type(DialogType.confirmation(
                        button(text.agree(), DialogAction.customClick((response, audience) -> answer.agree(), once)),
                        button(text.decline(), DialogAction.customClick((response, audience) -> answer.decline(), once)))));
        player.showDialog(dialog);
    }

    private static ActionButton button(Component label, DialogAction action) {
        return ActionButton.builder(label).width(BUTTON_WIDTH).action(action).build();
    }
}
