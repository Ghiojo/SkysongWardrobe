package org.skysongdev.skysongwardrobe.items;

/** The two layers an item can carry, each in its own tag. */
public enum LayerKind {
    /** Drawn as uploaded. */
    REGULAR(WardrobeTags.LAYER),
    /** Multiplied by the item's dye colour before it is drawn. */
    DYABLE(WardrobeTags.DYABLE_LAYER);

    private final String tagId;

    LayerKind(String tagId) {
        this.tagId = tagId;
    }

    /** The SkysongItems tag that holds this layer. */
    public String tagId() {
        return tagId;
    }
}
