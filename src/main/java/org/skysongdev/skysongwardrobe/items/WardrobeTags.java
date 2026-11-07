package org.skysongdev.skysongwardrobe.items;

/** The SkysongItems tag ids SkysongWardrobe reads and writes. */
public final class WardrobeTags {
    /** STRING: the gallery entry drawn as-is, stored as {@code uuid:id}. */
    public static final String LAYER = "wardrobe_layer";
    /** STRING: the gallery entry tinted by {@link #DYE_COLOR}, stored as {@code uuid:id}. */
    public static final String DYABLE_LAYER = "wardrobe_dyable_layer";
    /** STRING: the item's kind, matched against a slot's {@code accepts} list in wardrobe.yml. */
    public static final String TYPE = "wardrobe_type";
    /** INT, built into SkysongItems: a packed RGB colour, the same value a CustomModelData tint uses. */
    public static final String DYE_COLOR = "dye_color";

    private WardrobeTags() {
    }
}
