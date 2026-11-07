package org.skysongdev.skysongwardrobe.commands;

import org.bukkit.permissions.Permissible;

import java.util.List;

/**
 * Permission checks. Gallery and apply access are lists in config.yml where any one entry is
 * enough, so other plugins' permissions can grant them. Every permission is op-only by default,
 * since none is declared with another default.
 */
public final class Permissions {
    /** {@code /sw reload}. */
    public static final String ADMIN = "skysongwardrobe.admin";
    private static final String OTHER_SUFFIX = ".other";

    private Permissions() {
    }

    /** Whether the sender holds at least one of the permissions. */
    public static boolean hasAny(Permissible sender, List<String> permissions) {
        return permissions.stream().anyMatch(sender::hasPermission);
    }

    /** Whether the sender holds the {@code .other} variant of at least one of the permissions. */
    public static boolean hasAnyOther(Permissible sender, List<String> permissions) {
        return permissions.stream().map(Permissions::other).anyMatch(sender::hasPermission);
    }

    /** The permission to do the same to another player: the base plus {@code .other}. */
    public static String other(String permission) {
        return permission + OTHER_SUFFIX;
    }
}
