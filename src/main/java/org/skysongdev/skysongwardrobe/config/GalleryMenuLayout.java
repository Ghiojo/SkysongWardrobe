package org.skysongdev.skysongwardrobe.config;

/**
 * The gallery menu as wardrobe.yml's {@code gallery:} section describes it.
 *
 * @param title    the menu title in MiniMessage; {@code <player>} is the gallery's owner
 * @param filler   the item on the bottom row where there is no arrow
 * @param previous the arrow to the previous page
 * @param next     the arrow to the next page
 */
public record GalleryMenuLayout(String title, MenuItem filler, MenuItem previous, MenuItem next) {
}
