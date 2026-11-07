package org.skysongdev.skysongwardrobe.render;

import java.util.Locale;

/** The two player models. A player render always keeps the base skin's; a mannequin takes its spawner's. */
public enum ArmModel {
    /** Four-pixel arms, "Steve". */
    CLASSIC,
    /** Three-pixel arms, "Alex". */
    SLIM;

    /** The model a {@code mannequin_model} tag names: {@code slim} in any case is slim, anything else classic. */
    public static ArmModel fromTag(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).equals("slim") ? SLIM : CLASSIC;
    }
}
