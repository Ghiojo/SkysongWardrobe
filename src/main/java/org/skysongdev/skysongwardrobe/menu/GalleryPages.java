package org.skysongdev.skysongwardrobe.menu;

import java.util.List;

/** Splits a gallery into pages of 45 entries, the five rows above the arrow row. */
public final class GalleryPages {
    public static final int PER_PAGE = 45;

    private GalleryPages() {
    }

    /** How many pages {@code size} entries need; an empty gallery still has one, empty page. */
    public static int count(int size) {
        return Math.max(1, (size + PER_PAGE - 1) / PER_PAGE);
    }

    /** The entries on page {@code page}, counted from 0. */
    public static <T> List<T> page(List<T> entries, int page) {
        int from = Math.min(entries.size(), page * PER_PAGE);
        return entries.subList(from, Math.min(entries.size(), from + PER_PAGE));
    }
}
