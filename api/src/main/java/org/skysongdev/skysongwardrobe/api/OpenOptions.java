package org.skysongdev.skysongwardrobe.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * How to open a wardrobe. Immutable: each method returns a changed copy.
 *
 * <pre>{@code
 * OpenOptions options = OpenOptions.defaults()
 *         .access(Access.BYPASS)
 *         .silent()
 *         .onClose(player -> myMenu.open(player));
 * }</pre>
 */
public final class OpenOptions {
    private static final OpenOptions DEFAULTS = new OpenOptions(Access.HONOR, false, null);

    private final Access access;
    private final boolean silent;
    private final Consumer<Player> onClose;

    private OpenOptions(Access access, boolean silent, Consumer<Player> onClose) {
        this.access = access;
        this.silent = silent;
        this.onClose = onClose;
    }

    /** Honour the access rules, tell the viewer why a wardrobe did not open, and do nothing on close. */
    public static @NotNull OpenOptions defaults() {
        return DEFAULTS;
    }

    /** Whether the access rules apply; {@link Access#HONOR} by default. */
    public @NotNull OpenOptions access(@NotNull Access access) {
        return new OpenOptions(Objects.requireNonNull(access, "access"), silent, onClose);
    }

    /**
     * Don't send SkysongWardrobe's own message when the wardrobe does not open, so the caller
     * can explain the {@link OpenResult} its own way.
     */
    public @NotNull OpenOptions silent() {
        return new OpenOptions(access, true, onClose);
    }

    /**
     * Runs {@code action} with the viewer one tick after they close the wardrobe, so a plugin
     * can send them back to its own menu. It runs however the wardrobe was closed, including the
     * viewer logging out, so check {@link Player#isOnline()} before opening anything.
     */
    public @NotNull OpenOptions onClose(@NotNull Consumer<Player> action) {
        return new OpenOptions(access, silent, Objects.requireNonNull(action, "action"));
    }

    public @NotNull Access access() {
        return access;
    }

    public boolean isSilent() {
        return silent;
    }

    public @NotNull Optional<Consumer<Player>> closeAction() {
        return Optional.ofNullable(onClose);
    }
}
